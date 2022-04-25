/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.agent.awsv1;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableResponse;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;

public class AWSClientInvokeInterceptorTests {
    private static final String SERVICE = "DynamoDBv2";

    private TestListener testListener;
    private DefaultRequest request;

    @Before
    public void before() {
        EventBus.removeAllListeners();
        EventBus.addListener(testListener = new TestListener());
        request = new DefaultRequest(
                new UpdateTableRequest("tableName", new ProvisionedThroughput(1L, 1L)), SERVICE);
    }

    @After
    public void after() {
        EventBus.removeAllListeners();
    }

    @Test
    public void testInstallation() {
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        AgentBuilder.Identified.Extendable extendable = Mockito.mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = Mockito.mock(AgentBuilder.Identified.Narrowable.class);
        AWSClientInvokeInterceptor interceptor = new AWSClientInvokeInterceptor();
        Mockito.when(agentBuilder.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        Mockito.when(narrowable.transform(any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        AgentBuilder result = interceptor.install(agentBuilder);
        Assert.assertSame(extendable, result);
    }

    @Test
    public void testMethodMatcherSucceeds() throws Exception {
        Assert.assertTrue(methodMatches("doInvoke", AmazonSQSClient.class));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailsOnMethod() throws Exception {
        methodMatches("notAMethod", AmazonDynamoDBClient.class);
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailsOnClass() throws Exception {
        Assert.assertFalse(methodMatches("doInvoke", String.class));
    }

    @Test
    public void testClassMatcherSucceeds() {
        Assert.assertTrue(classMatches(AmazonSNSClient.class));
    }

    @Test
    public void testClassMatcherFails() {
        Assert.assertFalse(classMatches(String.class));
    }

    @Test
    public void testClassMatcherFailsOnAbstractType() {
        Assert.assertFalse(classMatches(FakeAWSClass.class));
    }

    @Test
    public void testRequestEventCreation() {
        ServiceDownstreamRequestEvent event = AWSClientInvokeInterceptor.enter(request, null);

        Assert.assertEquals(AWSClientInvokeInterceptor.AWS_V1_ORIGIN, event.getOrigin());
        Assert.assertEquals(SERVICE, event.getService());
        Assert.assertEquals("UpdateTable", event.getOperation());
    }

    @Test
    public void testRequestEventPublish() {
        ServiceDownstreamRequestEvent event = AWSClientInvokeInterceptor.enter(request, null);

        Assert.assertNotNull(testListener.request);
        Assert.assertEquals(event, testListener.request);
        Assert.assertEquals(request, testListener.request.getRequest());
    }

    @Test
    public void testResponseEventPublish() {
        UpdateTableResponse response = UpdateTableResponse.builder().tableDescription((TableDescription) null).build();
        ServiceDownstreamRequestEvent requestEvent = AWSClientInvokeInterceptor.enter(request, null);

        AWSClientInvokeInterceptor.exit(requestEvent, response, null);
        ServiceDownstreamResponseEvent responseEvent = testListener.response;

        Assert.assertNotNull(responseEvent);
        Assert.assertEquals(AWSClientInvokeInterceptor.AWS_V1_ORIGIN, responseEvent.getOrigin());
        Assert.assertEquals(SERVICE, responseEvent.getService());
        Assert.assertEquals("UpdateTable", responseEvent.getOperation());
        Assert.assertEquals(response, responseEvent.getResponse());
        Assert.assertNull(responseEvent.getThrown());
    }

    @Test
    public void testResponseEventWithThrowable() {
        Exception thrown = new RuntimeException();
        ServiceDownstreamRequestEvent requestEvent = AWSClientInvokeInterceptor.enter(request, null);

        AWSClientInvokeInterceptor.exit(requestEvent, null, thrown);

        Assert.assertNotNull(testListener.response);
        Assert.assertEquals(thrown, testListener.response.getThrown());
        Assert.assertNull(testListener.response.getResponse());
    }

    /**
     * Helper function to test the class matcher matching
     * @param clazz Class type we are validating
     * @return true if matches else false
     */
    private boolean classMatches(Class clazz) {
        AWSClientInvokeInterceptor interceptor = new AWSClientInvokeInterceptor(){};
        return interceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
    }

    /**
     * Helper function to test the method matcher against an input class
     * @param methodName name of method
     * @param paramType class we are verifying contains the method
     * @return true if matches, else false
     * @throws NoSuchMethodException
     */
    private boolean methodMatches(String methodName, Class paramType) throws NoSuchMethodException {
        Method method = null;
        for (Method m: paramType.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                Assert.assertNull(method);
                method = m;
            }
        }

        if (method == null) {
            throw new NoSuchMethodException();
        }

        AWSClientInvokeInterceptor interceptor = new AWSClientInvokeInterceptor();
        return interceptor.buildMethodMatcher()
                .matches(new MethodDescription.ForLoadedMethod(method));
    }

    private static class TestListener implements Listener {
        ServiceDownstreamRequestEvent request;
        ServiceDownstreamResponseEvent response;
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ServiceDownstreamRequestEvent) {
                request = (ServiceDownstreamRequestEvent)e;
            } else if (e instanceof ServiceDownstreamResponseEvent) {
                response = (ServiceDownstreamResponseEvent)e;
            } else  {
                Assert.fail("Unexpected event");
            }
        }
    }

    /**
     * Fake AWS class to test that only non-abstract classes are instrumented
     */
    public abstract class FakeAWSClass extends AmazonWebServiceClient {
        public FakeAWSClass(ClientConfiguration clientConfiguration) {
            super(clientConfiguration);
        }
    }
}
