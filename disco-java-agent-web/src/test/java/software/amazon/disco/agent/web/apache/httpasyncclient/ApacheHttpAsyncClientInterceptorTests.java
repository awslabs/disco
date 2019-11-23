/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.web.apache.httpasyncclient;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.web.apache.source.MockEventBusListener;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApacheHttpAsyncClientInterceptorTests {

    private static final String URI = "http://amazon.com/explore/something";
    private static final String METHOD = "GET";

    private ApacheHttpAsyncClientInterceptor interceptor;
    private MockEventBusListener mockEventBusListener;

    @Before
    public void before() {
        interceptor = new ApacheHttpAsyncClientInterceptor();
        mockEventBusListener = new MockEventBusListener();
        TransactionContext.create();
        EventBus.addListener(mockEventBusListener);
    }

    @After
    public void after() {
        TransactionContext.clear();
        EventBus.removeListener(mockEventBusListener);
    }

    @Test
    public void testInstallation() {
        AgentBuilder agentBuilder = mock(AgentBuilder.class);
        AgentBuilder.Identified.Extendable extendable = mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = mock(AgentBuilder.Identified.Narrowable.class);
        when(agentBuilder.type(any(ElementMatcher.class))).thenReturn(narrowable);
        when(narrowable.transform(any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        AgentBuilder result = interceptor.install(agentBuilder);
        assertSame(extendable, result);
    }

    @Test
    public void testClassMatcherSucceedsOnRealClient() {
        assertTrue(classMatches(HttpAsyncClients.createMinimal().getClass()));
        assertTrue(classMatches(HttpAsyncClients.createMinimal().getClass().getSuperclass()));
    }

    @Test
    public void testClassMatcherSucceededOnConcreteClass() {
        assertTrue(classMatches(SomeChainedExecuteMethodsHttpAsyncClient.class));
    }

    @Test
    public void testClassMatcherFailedOnInterface() {
        assertFalse(classMatches(HttpAsyncClient.class));
    }

    @Test
    public void testClassMatcherFailedOnUnrelatedClass() {
        assertFalse(classMatches(String.class));
    }

    @Test
    public void testMethodMatcherSucceedsOnRealClient() throws Exception {
        HttpAsyncClient client = HttpAsyncClients.createMinimal();
        assertEquals(1, methodMatchedCount("execute", client.getClass()));
    }

    @Test
    public void testMethodMatcherSucceeded() throws Exception {
        assertEquals(1, methodMatchedCount("execute", SomeChainedExecuteMethodsHttpAsyncClient.class));
    }

    @Test
    public void testMethodMatcherFailedOnAbstractMethod() throws Exception {
        assertEquals(0, methodMatchedCount("execute", HttpAsyncClient.class));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailedOnNotExistingMethod() throws Exception {
        assertEquals(0, methodMatchedCount("doesntExistMethodName", SomeChainedExecuteMethodsHttpAsyncClient.class));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailedOnWrongClass() throws Exception {
        assertEquals(0, methodMatchedCount("execute", String.class));
    }

    @Test
    public void testHeaderReplacement() throws Throwable {
        HttpGet get = new HttpGet(URI);
        get.addHeader("foo", "bar");
        get.addHeader("foo", "bar2");
        HttpAsyncRequestProducer requestProducer = HttpAsyncMethods.create(get);

        Object[] decoratedArgs = ApacheHttpAsyncClientInterceptor.RequestProducerAdvice.enter(requestProducer, mock(HttpContext.class), null);
        ((HttpAsyncRequestProducer) decoratedArgs[0]).generateRequest();

        List<Event> events = mockEventBusListener.getReceivedEvents();
        HttpServiceDownstreamRequestEvent event = (HttpServiceDownstreamRequestEvent)events.get(0);
        event.replaceHeader("foo", "bar3");

        assertEquals(1, get.getHeaders("foo").length);
        assertEquals("bar3", get.getFirstHeader("foo").getValue());
    }

    @Test
    public void testFeedingDecoratedRequestProducer() throws IOException, HttpException {
        HttpAsyncRequestProducer originalRequestProducer = HttpAsyncMethods.create(new HttpGet(URI));
        Object decoratedRequestProducer = Proxy.newProxyInstance(
                HttpAsyncRequestProducer.class.getClassLoader(),
                new Class[]{HttpAsyncRequestProducer.class},
                (proxy, method, args) -> method.invoke(originalRequestProducer, args));
        Object[] decoratedArgs = ApacheHttpAsyncClientInterceptor.RequestProducerAdvice.enter(decoratedRequestProducer, mock(HttpContext.class), null);

        // there is no way to directly compare two decorated/Proxy objects, so need to examine some side effects
        ((HttpAsyncRequestProducer) decoratedArgs[0]).generateRequest();
        assertEquals(0, mockEventBusListener.getReceivedEvents().size());
    }

    @Test
    public void testFeedingDecoratedFutureCallback() {
        HttpAsyncRequestProducer requestProducer = HttpAsyncMethods.create(new HttpGet(URI));
        FutureCallback<HttpResponse> originalFutureCallback = new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse result) {

            }

            @Override
            public void failed(final Exception ex) {

            }

            @Override
            public void cancelled() {

            }
        };

        Object decoratedFutureCallback = Proxy.newProxyInstance(
                FutureCallback.class.getClassLoader(),
                new Class[]{FutureCallback.class},
                (proxy, method, args) -> method.invoke(originalFutureCallback, args));
        Object[] decoratedArgs = ApacheHttpAsyncClientInterceptor.RequestProducerAdvice.enter(requestProducer, mock(HttpContext.class), decoratedFutureCallback);

        // there is no way to directly compare two decorated/Proxy objects, so need to examine some side effects
        ((FutureCallback) decoratedArgs[1]).cancelled();
        assertEquals(0, mockEventBusListener.getReceivedEvents().size());
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTests {
        @Parameterized.Parameter()
        public FutureCallback futureCallback;

        private ApacheHttpAsyncClientInterceptor interceptor;
        private MockEventBusListener mockEventBusListener;

        @Before
        public void before() {
            interceptor = new ApacheHttpAsyncClientInterceptor();
            mockEventBusListener = new MockEventBusListener();
            TransactionContext.create();
            EventBus.addListener(mockEventBusListener);
        }

        @After
        public void after() {
            TransactionContext.clear();
            EventBus.removeListener(mockEventBusListener);
        }

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { null },
                    { new FutureCallback<HttpResponse>() {

                        @Override
                        public void completed(final HttpResponse result) {

                        }

                        @Override
                        public void failed(final Exception ex) {

                        }

                        @Override
                        public void cancelled() {

                        }
                    } }
            });
        }

        @Test
        public void testInterceptorSucceededOnCompleted() throws Throwable {
            HttpResponse expectedResponse = new BasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");

            HttpClientContext context = HttpClientContext.create();
            FutureCallback decoratedFutureCallback = mockAndVerifyRequestSending(context, futureCallback);

            // Mock http context change after receiving the response
            context.setAttribute("http.response", expectedResponse);
            // Mock the step after receiving the response
            decoratedFutureCallback.completed(expectedResponse);

            List<Event> events = mockEventBusListener.getReceivedEvents();
            assertEquals(2, events.size());

            // Verify the Response Event
            HttpServiceDownstreamResponseEvent serviceDownstreamResponseEvent = (HttpServiceDownstreamResponseEvent) events.get(1);
            verifyServiceResponseEvent(serviceDownstreamResponseEvent);
            assertNull(serviceDownstreamResponseEvent.getResponse());
            assertNull(serviceDownstreamResponseEvent.getThrown());
            assertEquals(expectedResponse.getStatusLine().getStatusCode(), serviceDownstreamResponseEvent.getStatusCode());
        }

        @Test
        public void testInterceptorSucceededOnFailed() throws Throwable {
            IllegalStateException expectedException = new IllegalStateException();

            HttpClientContext context = HttpClientContext.create();
            FutureCallback decoratedFutureCallback = mockAndVerifyRequestSending(context, futureCallback);

            // Mock failed state
            decoratedFutureCallback.failed(expectedException);

            List<Event> events = mockEventBusListener.getReceivedEvents();
            assertEquals(2, events.size());

            // Verify the Response Event
            HttpServiceDownstreamResponseEvent serviceDownstreamResponseEvent = (HttpServiceDownstreamResponseEvent) events.get(1);
            verifyServiceResponseEvent(serviceDownstreamResponseEvent);
            assertNull(serviceDownstreamResponseEvent.getResponse());
            assertEquals(expectedException, serviceDownstreamResponseEvent.getThrown());
        }

        @Test
        public void testInterceptorSucceededOnCancelled() throws Throwable {
            HttpClientContext context = HttpClientContext.create();
            FutureCallback decoratedFutureCallback = mockAndVerifyRequestSending(context, futureCallback);

            // Mock cancelled state
            decoratedFutureCallback.cancelled();

            List<Event> events = mockEventBusListener.getReceivedEvents();
            assertEquals(2, events.size());

            // Verify the Response Event
            HttpServiceDownstreamResponseEvent serviceDownstreamResponseEvent = (HttpServiceDownstreamResponseEvent) events.get(1);
            verifyServiceResponseEvent(serviceDownstreamResponseEvent);
            assertNull(serviceDownstreamResponseEvent.getResponse());
            assertNull(serviceDownstreamResponseEvent.getThrown());
        }

        @Test
        public void testInterceptorSucceededOnThrowable() throws Throwable {
            Throwable expectedThrowable = new IllegalStateException();

            HttpClientContext context = HttpClientContext.create();
            mockAndVerifyRequestSending(context, futureCallback);

            // Mock throwable caught
            ApacheHttpAsyncClientInterceptor.RequestProducerAdvice.exit(expectedThrowable);

            List<Event> events = mockEventBusListener.getReceivedEvents();
            assertEquals(2, events.size());

            // Verify the Response Event
            HttpServiceDownstreamResponseEvent serviceDownstreamResponseEvent = (HttpServiceDownstreamResponseEvent) events.get(1);
            assertNull(serviceDownstreamResponseEvent.getOperation());
            assertNull(serviceDownstreamResponseEvent.getService());
            assertEquals(ApacheHttpAsyncClientInterceptor.APACHE_HTTP_ASYNC_CLIENT_ORIGIN, serviceDownstreamResponseEvent.getOrigin());
            assertNull(serviceDownstreamResponseEvent.getResponse());
            assertEquals(expectedThrowable, serviceDownstreamResponseEvent.getThrown());
        }

        private FutureCallback mockAndVerifyRequestSending(Object context, Object futureCallback) throws Throwable {
            HttpGet request = new HttpGet(URI);
            HttpAsyncRequestProducer requestProducer = HttpAsyncMethods.create(request);

            List<Event> events;
            Object[] decoratedArgs = ApacheHttpAsyncClientInterceptor.RequestProducerAdvice.enter(requestProducer, context, futureCallback);
            events = mockEventBusListener.getReceivedEvents();
            assertEquals(0, events.size());

            // Mock the prep step before sending request
            ((HttpAsyncRequestProducer) decoratedArgs[0]).generateRequest();

            events = mockEventBusListener.getReceivedEvents();
            assertEquals(1, events.size());

            // Verify the Request Event
            verifyServiceRequestEvent((HttpServiceDownstreamRequestEvent) events.get(0));

            return (FutureCallback) decoratedArgs[1];
        }
    }

    private static void verifyServiceRequestEvent(final HttpServiceDownstreamRequestEvent serviceDownstreamRequestEvent) {
        assertEquals(METHOD, serviceDownstreamRequestEvent.getMethod());
        assertEquals(URI, serviceDownstreamRequestEvent.getUri());
        assertEquals(ApacheHttpAsyncClientInterceptor.APACHE_HTTP_ASYNC_CLIENT_ORIGIN, serviceDownstreamRequestEvent.getOrigin());
        assertNull(serviceDownstreamRequestEvent.getRequest());
    }

    private static void verifyServiceResponseEvent(final ServiceDownstreamResponseEvent serviceDownstreamResponseEvent) {
        assertEquals(METHOD, serviceDownstreamResponseEvent.getOperation());
        assertEquals(URI, serviceDownstreamResponseEvent.getService());
        assertEquals(ApacheHttpAsyncClientInterceptor.APACHE_HTTP_ASYNC_CLIENT_ORIGIN, serviceDownstreamResponseEvent.getOrigin());
    }

    /**
     * Helper method to test the class matcher matching
     *
     * @param clazz Class type we are validating
     * @return true if matches else false
     */
    private boolean classMatches(Class clazz) {
        return ApacheHttpAsyncClientInterceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
    }

    /**
     * Helper method to test the method matcher against an input class
     *
     * @param methodName name of method
     * @param paramType class we are verifying contains the method
     * @return Matched methods count
     * @throws NoSuchMethodException
     */
    private int methodMatchedCount(String methodName, Class paramType) throws NoSuchMethodException {
        List<Method> methods = new ArrayList<>();
        for (Method m : paramType.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                methods.add(m);
            }
        }

        if (methods.size() == 0) throw new NoSuchMethodException();

        int matchedCount = 0;
        for (Method m : methods) {
            MethodDescription.ForLoadedMethod forLoadedMethod = new MethodDescription.ForLoadedMethod(m);
            if (ApacheHttpAsyncClientInterceptor.buildMethodMatcher(new TypeDescription.ForLoadedType(paramType)).matches(forLoadedMethod)) {
                matchedCount++;
            }
        }
        return matchedCount;
    }

    public class SomeChainedExecuteMethodsHttpAsyncClient implements HttpAsyncClient {
        public int executeCallChainDepth = 0;

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Future<T> execute(final HttpAsyncRequestProducer requestProducer, final HttpAsyncResponseConsumer<T> responseConsumer, final HttpContext context, final FutureCallback<T> callback) {
            executeCallChainDepth++;
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Future<T> execute(final HttpAsyncRequestProducer requestProducer, final HttpAsyncResponseConsumer<T> responseConsumer, final FutureCallback<T> callback) {
            executeCallChainDepth++;
            return execute(requestProducer, responseConsumer, HttpClientContext.create(), callback);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Future<HttpResponse> execute(final HttpHost target, final HttpRequest request, final HttpContext context, final FutureCallback<HttpResponse> callback) {
            executeCallChainDepth++;
            return execute(
                    HttpAsyncMethods.create(target, request),
                    HttpAsyncMethods.createConsumer(),
                    context, callback);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Future<HttpResponse> execute(final HttpHost target, final HttpRequest request, final FutureCallback<HttpResponse> callback) {
            executeCallChainDepth++;
            return execute(target, request, HttpClientContext.create(), callback);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Future<HttpResponse> execute(final HttpUriRequest request, final HttpContext context, final FutureCallback<HttpResponse> callback) {
            executeCallChainDepth++;
            return execute(new HttpHost("http://amazon.com"), request, context, callback);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Future<HttpResponse> execute(final HttpUriRequest request, final FutureCallback<HttpResponse> callback) {
            executeCallChainDepth++;
            return execute(request, HttpClientContext.create(), callback);
        }
    }

}