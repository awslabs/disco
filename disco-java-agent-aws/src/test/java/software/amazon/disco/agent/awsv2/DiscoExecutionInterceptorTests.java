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

package software.amazon.disco.agent.awsv2;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.AbstractServiceEvent;
import software.amazon.disco.agent.event.AwsServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.AwsServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

public class DiscoExecutionInterceptorTests {
    private static final String SERVICE = "service";
    private static final String OPERATION = "operation";
    private static final String REGION = "us-west-2";
    private static final int STATUS_CODE = 200;
    private static final String REQUEST_ID = "12345";

    private DiscoExecutionInterceptor interceptor;
    private Map<String, List<String>> headers;
    private TestListener testListener;
    private InterceptorContext context;
    private ExecutionAttributes executionAttributes;

    @Mock
    private SdkRequest sdkRequestMock;

    @Mock
    private SdkHttpFullRequest sdkHttpRequestMock;

    @Mock
    private SdkResponse sdkResponseMock;

    @Mock
    private SdkHttpFullResponse sdkHttpResponseMock;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        TransactionContext.clear();
        EventBus.removeAllListeners();
        EventBus.addListener(testListener = new TestListener());
        interceptor = new DiscoExecutionInterceptor();
        headers = new HashMap<>();
        context = InterceptorContext.builder()
                .httpRequest(sdkHttpRequestMock)
                .httpResponse(sdkHttpResponseMock)
                .request(sdkRequestMock)
                .response(sdkResponseMock)
                .build();

        executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE);
        executionAttributes.putAttribute(SdkExecutionAttribute.OPERATION_NAME, OPERATION);
        executionAttributes.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.of(REGION));

        when(sdkHttpRequestMock.headers()).thenReturn(headers);
        when(sdkHttpResponseMock.headers()).thenReturn(headers);
        when(sdkHttpResponseMock.statusCode()).thenReturn(STATUS_CODE);
    }

    @Test
    public void testBeforeExecution() {
        interceptor.beforeExecution(context, executionAttributes);

        AwsServiceDownstreamRequestEvent event =
                (AwsServiceDownstreamRequestEvent) TransactionContext.getMetadata(DiscoExecutionInterceptor.TX_REQUEST_EVENT_KEY);

        verifyEvent(event);
        Assert.assertNotNull(event);
        Assert.assertEquals(REGION, event.getRegion());
    }

    @Test
    public void testModifyHttpRequest() {
        interceptor.beforeExecution(context, executionAttributes);  // to populate TX
        SdkHttpRequest sdkHttpRequest = interceptor.modifyHttpRequest(context, executionAttributes);

        Assert.assertEquals(sdkHttpRequestMock, sdkHttpRequest);
    }

    @Test
    public void testModifyHttpRequestPublishesRequestEvent() {
        interceptor.beforeExecution(context, executionAttributes);  // to populate TX
        interceptor.modifyHttpRequest(context, executionAttributes);

        AwsServiceDownstreamRequestEvent event = testListener.request;

        verifyEvent(event);
        Assert.assertEquals(REGION, event.getRegion());
    }

    @Test
    public void testBeforeTransmission() {
        interceptor.beforeTransmission(null, null);  // First invocation initializes value
        Assert.assertEquals(0, TransactionContext.getMetadata(DiscoExecutionInterceptor.TX_RETRY_COUNT_KEY));

        interceptor.beforeTransmission(null, null);  // Subsequent invokes increment it
        Assert.assertEquals(1, TransactionContext.getMetadata(DiscoExecutionInterceptor.TX_RETRY_COUNT_KEY));
    }

    @Test
    public void testAfterExecution() {
        // Set up TX & request event
        interceptor.beforeExecution(context, executionAttributes);
        interceptor.modifyHttpRequest(context, executionAttributes);

        // Then call method-under-test
        interceptor.afterExecution(context, executionAttributes);

        AwsServiceDownstreamResponseEvent event = testListener.response;
        verifyEvent(event);
        Assert.assertEquals(0, event.getRetryCount());
        Assert.assertEquals(STATUS_CODE, event.getStatusCode());
        Assert.assertEquals(sdkResponseMock, event.getResponse());
        Assert.assertEquals(testListener.request, event.getRequest());
        Assert.assertEquals(headers, event.getHeaderMap());
        Assert.assertNull(event.getThrown());
    }

    @Test
    public void testAfterExecutionWithMultipleRetries() {
        // Set up TX & request event
        interceptor.beforeExecution(context, executionAttributes);
        interceptor.modifyHttpRequest(context, executionAttributes);

        // "Mock" 2 retries
        interceptor.beforeTransmission(null, null);
        interceptor.beforeTransmission(null, null);
        interceptor.beforeTransmission(null, null);

        // Then call method-under-test
        interceptor.afterExecution(context, executionAttributes);

        verifyEvent(testListener.response);
        Assert.assertEquals(2, testListener.response.getRetryCount());
    }

    @Test
    public void testAfterExecutionRecordsRequestId() {
        List<String> requestIdList = new ArrayList<>();
        requestIdList.add(REQUEST_ID);
        headers.put("x-amzn-requestid", requestIdList);

        // Set up TX & request event
        interceptor.beforeExecution(context, executionAttributes);
        interceptor.modifyHttpRequest(context, executionAttributes);

        // Then call method-under-test
        interceptor.afterExecution(context, executionAttributes);

        verifyEvent(testListener.response);
        Assert.assertEquals(REQUEST_ID, testListener.response.getRequestId());
    }

    private void verifyEvent(AbstractServiceEvent event) {
        Assert.assertNotNull(event);
        Assert.assertEquals(DiscoExecutionInterceptor.AWS_SDK_V2_CLIENT_ORIGIN, event.getOrigin());
        Assert.assertEquals(SERVICE, event.getService());
        Assert.assertEquals(OPERATION, event.getOperation());
    }

    private static class TestListener implements Listener {
        AwsServiceDownstreamRequestEvent request;
        AwsServiceDownstreamResponseEvent response;
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ServiceDownstreamRequestEvent) {
                request = (AwsServiceDownstreamRequestEvent) e;
            } else if (e instanceof ServiceDownstreamResponseEvent) {
                response = (AwsServiceDownstreamResponseEvent) e;
            } else  {
                Assert.fail("Unexpected event");
            }
        }
    }
}
