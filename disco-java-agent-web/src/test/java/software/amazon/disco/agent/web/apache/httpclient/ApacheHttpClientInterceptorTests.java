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

package software.amazon.disco.agent.web.apache.httpclient;

import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.disco.agent.web.apache.httpclient.source.MockEventBusListener;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApacheHttpClientInterceptorTests {

    private static final String URI = "http://amazon.com/explore/something";
    private static final String METHOD = "GET";

    // Set the whitelisted attributes
    private static final String HEADER_NAME_1 = "headerName1";
    private static final String HEADER_VALUE_1 = "headerValue1";
    private static final String HEADER_NAME_2 = "headerName2";
    private static final String HEADER_VALUE_2 = "headerValue2";

    private ApacheHttpClientInterceptor interceptor;
    private HttpResponse expectedResponse;
    private IOException expectedIOException;
    private MockEventBusListener mockEventBusListener;

    @Before
    public void before() {
        interceptor = new ApacheHttpClientInterceptor();
        expectedResponse = null;
        expectedIOException = null;
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
    public void testClassMatcherSucceededOnConcreteClass() {
        assertTrue(classMatches(SomeChainedExecuteMethodsHttpClient.class));
    }

    @Test
    public void testClassMatcherFailedOnInterface() {
        assertFalse(classMatches(HttpClient.class));
    }

    @Test
    public void testClassMatcherFailedOnUnrelatedClass() {
        assertFalse(classMatches(String.class));
    }

    /**
     * Matched count should be equivalent to the number of target `execute` method in {@link SomeChainedExecuteMethodsHttpClient}.
     */
    @Test
    public void testMethodMatcherSucceeded() throws Exception {
        assertEquals(11, methodMatchedCount("execute", SomeChainedExecuteMethodsHttpClient.class));
    }

    @Test
    public void testMethodMatcherFailedOnAbstractMethod() throws Exception {
        assertEquals(0, methodMatchedCount("execute", HttpClient.class));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailedOnNotExistingMethod() throws Exception {
        assertEquals(0, methodMatchedCount("doesntExistMethodName", SomeChainedExecuteMethodsHttpClient.class));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testMethodMatcherFailedOnWrongClass() throws Exception {
        assertEquals(0, methodMatchedCount("service", String.class));
    }

    /**
     * Intercepts chained methods starts here: {@link SomeChainedExecuteMethodsHttpClient#execute(HttpUriRequest)}
     * Ends here: {@link SomeChainedExecuteMethodsHttpClient#execute(HttpHost, HttpRequest, HttpContext)},
     * where returns {@link ApacheHttpClientInterceptorTests#expectedResponse}
     */
    @Test
    public void testInterceptorSucceededOnChainedMethods() throws Throwable {
        prepareToBePropagatedHeaders();

        HttpUriRequest request = mock(HttpUriRequest.class);
        setUpRequest(request);

        // Set up victim http client
        SomeChainedExecuteMethodsHttpClient someHttpClient = new SomeChainedExecuteMethodsHttpClient();
        expectedResponse = new BasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");

        ApacheHttpClientInterceptor.intercept(new Object[] {request}, "origin", () -> someHttpClient.execute(request));

        verifyHeaderPropagationResults(request);

        List<Event> events = mockEventBusListener.getReceivedEvents();
        // Verify only one of interceptions does the interceptor business logic even if there is a method chaining,
        // as a result, only two service downstream events are published (request/response)
        assertEquals(2, events.size());

        // Verify the Request Event
        verifyServiceRequestEvent(request, (ServiceDownstreamRequestEvent) events.get(0));

        // Verify the Response Event
        ServiceDownstreamResponseEvent serviceDownstreamResponseEvent = (ServiceDownstreamResponseEvent) events.get(1);
        verifyServiceResponseEvent(serviceDownstreamResponseEvent);
        assertEquals(expectedResponse, serviceDownstreamResponseEvent.getResponse());
        assertNull(serviceDownstreamResponseEvent.getThrown());

        assertEquals(3, someHttpClient.executeMethodChainingDepth);
    }

    /**
     * Intercepts {@link SomeChainedExecuteMethodsHttpClient#execute(SomeChainedExecuteMethodsHttpClient.WillThrowExceptionOnExecutionHttpRequest)},
     * where throws {@link ApacheHttpClientInterceptorTests#expectedIOException}
     */
    @Test(expected = IOException.class)
    public void testInterceptorSucceededAndReThrowOnException() throws Throwable {
        prepareToBePropagatedHeaders();

        SomeChainedExecuteMethodsHttpClient.WillThrowExceptionOnExecutionHttpRequest request = mock(SomeChainedExecuteMethodsHttpClient.WillThrowExceptionOnExecutionHttpRequest.class);
        setUpRequest(request);

        // Set up victim http client
        SomeChainedExecuteMethodsHttpClient someHttpClient = new SomeChainedExecuteMethodsHttpClient();
        expectedIOException = new IOException();

        try {
            ApacheHttpClientInterceptor.intercept(new Object[]{request}, "origin", () -> someHttpClient.execute(request));
        } finally {
            verifyHeaderPropagationResults(request);

            List<Event> events = mockEventBusListener.getReceivedEvents();
            // Verify only one of interceptions does the interceptor business logic even if there is a method chaining,
            // as a result, only two service downstream events are published (request/response)
            assertEquals(2, events.size());

            // Verify the Request Event
            verifyServiceRequestEvent(request, (ServiceDownstreamRequestEvent) events.get(0));

            // Verify the Response Event
            ServiceDownstreamResponseEvent serviceDownstreamResponseEvent = (ServiceDownstreamResponseEvent) events.get(1);
            verifyServiceResponseEvent(serviceDownstreamResponseEvent);
            assertNull(serviceDownstreamResponseEvent.getResponse());
            assertEquals(expectedIOException, serviceDownstreamResponseEvent.getThrown());

            assertEquals(1, someHttpClient.executeMethodChainingDepth);
        }
    }

    private static void prepareToBePropagatedHeaders() {
        // Set the whitelisted attributes
        ApacheHttpClientInterceptor.setWhiteListedAttributes(new HashSet<>(Arrays.asList(HEADER_NAME_1, HEADER_NAME_2)));

        // Add test metadata
        TransactionContext.putMetadata(HEADER_NAME_1, HEADER_VALUE_1);
        TransactionContext.setMetadataTag(HEADER_NAME_1, TransactionContext.PROPAGATE_IN_REQUEST_TAG);
        TransactionContext.putMetadata(HEADER_NAME_2, HEADER_VALUE_2);
        TransactionContext.setMetadataTag(HEADER_NAME_2, TransactionContext.PROPAGATE_IN_REQUEST_TAG);
    }

    private static void setUpRequest(final HttpRequest request) {
        RequestLine requestLine = mock(RequestLine.class);

        when(request.getRequestLine()).thenReturn(requestLine);
        when(requestLine.getUri()).thenReturn(URI);
        when(requestLine.getMethod()).thenReturn(METHOD);
    }

    private static void verifyHeaderPropagationResults(final HttpRequest request) {
        // Verify header propagation
        ArgumentCaptor<String> removeHeadersNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> addHeaderNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> addHeaderValueArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(request, times(2)).removeHeaders(removeHeadersNameArgumentCaptor.capture());
        verify(request, times(2)).addHeader(addHeaderNameArgumentCaptor.capture(), addHeaderValueArgumentCaptor.capture());
        assertEquals(HEADER_NAME_1, removeHeadersNameArgumentCaptor.getAllValues().get(0));
        assertEquals(HEADER_NAME_1, addHeaderNameArgumentCaptor.getAllValues().get(0));
        assertEquals(HEADER_VALUE_1, addHeaderValueArgumentCaptor.getAllValues().get(0));
        assertEquals(HEADER_NAME_2, removeHeadersNameArgumentCaptor.getAllValues().get(1));
        assertEquals(HEADER_NAME_2, addHeaderNameArgumentCaptor.getAllValues().get(1));
        assertEquals(HEADER_VALUE_2, addHeaderValueArgumentCaptor.getAllValues().get(1));
    }

    private static void verifyServiceRequestEvent(final HttpRequest request, final ServiceDownstreamRequestEvent serviceDownstreamRequestEvent) {
        assertEquals(METHOD, serviceDownstreamRequestEvent.getOperation());
        assertEquals(URI, serviceDownstreamRequestEvent.getService());
        assertEquals(ApacheHttpClientInterceptor.APACHE_HTTP_CLIENT_ORIGIN, serviceDownstreamRequestEvent.getOrigin());
        assertEquals(request, serviceDownstreamRequestEvent.getRequest());
    }

    private static void verifyServiceResponseEvent(final ServiceDownstreamResponseEvent serviceDownstreamResponseEvent) {
        assertEquals(METHOD, serviceDownstreamResponseEvent.getOperation());
        assertEquals(URI, serviceDownstreamResponseEvent.getService());
        assertEquals(ApacheHttpClientInterceptor.APACHE_HTTP_CLIENT_ORIGIN, serviceDownstreamResponseEvent.getOrigin());
    }

    /**
     * Helper method to test the class matcher matching
     *
     * @param clazz Class type we are validating
     * @return true if matches else false
     */
    private boolean classMatches(Class clazz) {
        return ApacheHttpClientInterceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
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
            if (ApacheHttpClientInterceptor.buildMethodMatcher().matches(forLoadedMethod)) {
                matchedCount++;
            }
        }
        return matchedCount;
    }

    public class SomeChainedExecuteMethodsHttpClient implements HttpClient {
        int executeMethodChainingDepth = 0;

        /**
         * Doesn't matter in this test suite.
         * Just a thing required as part of the HttpClient implementation
         */
        @Override
        public HttpParams getParams() {
            return null;
        }

        /**
         * Doesn't matter in this test suite.
         * Just a thing required as part of the HttpClient implementation
         */
        @Override
        public ClientConnectionManager getConnectionManager() {
            return null;
        }

        /**
         * Target method - chained.
         */
        @Override
        public HttpResponse execute(final HttpUriRequest request) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return this.execute(request, (HttpContext)null);
        }

        /**
         * Target method - chained.
         */
        @Override
        public HttpResponse execute(final HttpUriRequest request, final HttpContext context) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return this.execute(null, request, context);
        }

        /**
         * Target method - chained.
         */
        @Override
        public HttpResponse execute(final HttpHost target, final HttpRequest request) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return this.execute(target, request, (HttpContext)null);
        }

        /**
         * Target method - chained.
         * This method returns {@link ApacheHttpClientInterceptorTests#expectedResponse}
         */
        @Override
        public HttpResponse execute(final HttpHost target, final HttpRequest request, final HttpContext context) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return expectedResponse;
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return this.execute((HttpUriRequest)request, responseHandler, (HttpContext)null);
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return this.execute(null, request, responseHandler, context);
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return this.execute(target, request, responseHandler, (HttpContext)null);
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return responseHandler.handleResponse(this.execute(target, request, context));
        }

        /**
         * Non-target method - should not be intercepted.
         * This method has the right method name, but doesn't have any argument whose super type is HttpRequest
         */
        public HttpResponse execute(String msg) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return null;
        }

        /**
         * Target method - has an argument whose super type is HttpRequest.
         */
        public HttpResponse execute(SomeClassSuperTypeIsHttpRequest request, String msg) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return null;
        }

        /**
         * Target method - has an argument whose super type is HttpRequest, but different order.
         */
        public HttpResponse execute(String msg, SomeClassSuperTypeIsHttpRequest request) throws IOException, ClientProtocolException {
            executeMethodChainingDepth++;
            return null;
        }

        /**
         * Target method - has an argument whose super type is HttpRequest,
         * throwing {@link ApacheHttpClientInterceptorTests#expectedIOException}.
         */
        public HttpResponse execute(WillThrowExceptionOnExecutionHttpRequest request) throws IOException {
            executeMethodChainingDepth++;
            throw expectedIOException;
        }

        abstract class SomeClassSuperTypeIsHttpRequest implements HttpRequest { }
        abstract class WillThrowExceptionOnExecutionHttpRequest implements HttpRequest {}
    }
}