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

package software.amazon.disco.agent.integtest.web.apache.httpclient;

import org.apache.http.RequestLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.mockito.ArgumentCaptor;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.integtest.web.apache.httpclient.source.FakeChainedExecuteCallHttpClientReturnResponse;
import software.amazon.disco.agent.integtest.web.apache.httpclient.source.FakeChainedExecuteCallHttpClientThrowException;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import org.apache.http.HttpRequest;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApacheHttpClientInterceptorTests {

    private static final String INTERCEPTOR_CLASS = "software.amazon.disco.agent.web.apache.httpclient.ApacheHttpClientInterceptor";
    private static final String PROPAGATE_IN_REQUEST_TAG = "PROPAGATE_IN_REQUEST";

    private TestListener testListener;

    // Set the header attributes
    private static final String HEADER_NAME_1 = "headerName1";
    private static final String HEADER_VALUE_1 = "headerValue1";
    private static final String HEADER_NAME_2 = "headerName2";
    private static final String HEADER_VALUE_2 = "headerValue2";

    private static final String SOME_URI = "http://amazon.com/explore/something";
    private static final String METHOD = "GET";

    @Before
    public void before() {
        TransactionContext.create();
        EventBus.addListener(testListener = new TestListener());
    }

    @After
    public void after() {
        TransactionContext.clear();
        EventBus.removeListener(testListener);
    }

    @Test
    public void testMinimalClient() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
            HttpGet request = new HttpGet("https://amazon.com");
            try {
                httpClient.execute(request);
            } catch (IOException e) {
                //swallow
            }
        }

        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    public void testDefaultClient() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://amazon.com");
            try {
                httpClient.execute(request);
            } catch (IOException e) {
                //swallow
            }
        }

        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    public void testExecuteInterceptionChained() throws Exception {
        prepareToBePropagatedHeaders();

        HttpUriRequest request = mock(HttpUriRequest.class);
        setUpRequest(request);

        // Set up victim http client
        FakeChainedExecuteCallHttpClientReturnResponse httpClient = new FakeChainedExecuteCallHttpClientReturnResponse();

        httpClient.execute(request);

        verifyHeaderPropagationResults(request);

        // Verify only one of interceptions does the interceptor business logic even if there is a method chaining,
        // as a result, only two service downstream events are published (request/response)
        assertEquals(1, testListener.requestEvents.size());

        // Verify the Request Event
        verifyServiceRequestEvent(request, testListener.requestEvents.get(0));

        assertEquals(1, testListener.responseEvents.size());

        // Verify the Response Event
        verifyServiceResponseEvent(testListener.responseEvents.get(0));
        assertEquals(httpClient.fakeResponse, testListener.responseEvents.get(0).getResponse());
        assertNull(testListener.responseEvents.get(0).getThrown());

        assertEquals(3, httpClient.executeCallChainDepth);
    }

    @Test(expected = IOException.class)
    public void testExecuteInterceptionException() throws Exception {
        prepareToBePropagatedHeaders();

        HttpUriRequest request = mock(HttpUriRequest.class);
        setUpRequest(request);

        // Set up victim http client
        FakeChainedExecuteCallHttpClientThrowException httpClient = new FakeChainedExecuteCallHttpClientThrowException();

        try {
            httpClient.execute(request);
        } finally {
            verifyHeaderPropagationResults(request);

            // Verify only one of interceptions does the interceptor business logic even if there is a method chaining,
            // as a result, only two service downstream events are published (request/response)
            assertEquals(1, testListener.requestEvents.size());

            // Verify the Request Event
            verifyServiceRequestEvent(request, testListener.requestEvents.get(0));

            assertEquals(1, testListener.responseEvents.size());

            // Verify the Response Event
            verifyServiceResponseEvent(testListener.responseEvents.get(0));
            assertNull(testListener.responseEvents.get(0).getResponse());
            assertEquals(httpClient.fakeException, testListener.responseEvents.get(0).getThrown());

            assertEquals(3, httpClient.executeCallChainDepth);
        }
    }

    private static class TestListener implements Listener {
        List<ServiceRequestEvent> requestEvents = new ArrayList<>();
        List<ServiceResponseEvent> responseEvents = new ArrayList<>();

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ServiceRequestEvent) {
                requestEvents.add((ServiceRequestEvent) e);
            } else if (e instanceof ServiceResponseEvent) {
                responseEvents.add((ServiceResponseEvent) e);
            } else {
                Assert.fail("Unexpected event");
            }
        }
    }

    private static void prepareToBePropagatedHeaders() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // Add test metadata
        TransactionContext.putMetadata(HEADER_NAME_1, HEADER_VALUE_1);
        TransactionContext.setMetadataTag(HEADER_NAME_1, PROPAGATE_IN_REQUEST_TAG);
        TransactionContext.putMetadata(HEADER_NAME_2, HEADER_VALUE_2);
        TransactionContext.setMetadataTag(HEADER_NAME_2, PROPAGATE_IN_REQUEST_TAG);
    }

    private static void setUpRequest(final HttpRequest request) {
        RequestLine requestLine = mock(RequestLine.class);

        when(request.getRequestLine()).thenReturn(requestLine);
        when(requestLine.getUri()).thenReturn(SOME_URI);
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

    private static void verifyServiceRequestEvent(final HttpRequest request, final ServiceRequestEvent serviceDownstreamRequestEvent) {
        assertTrue(serviceDownstreamRequestEvent instanceof ServiceDownstreamRequestEvent);
        assertEquals(METHOD, serviceDownstreamRequestEvent.getOperation());
        assertEquals(SOME_URI, serviceDownstreamRequestEvent.getService());
        assertEquals("ApacheHttpClient", serviceDownstreamRequestEvent.getOrigin());
        assertEquals(request, serviceDownstreamRequestEvent.getRequest());
    }

    private static void verifyServiceResponseEvent(final ServiceResponseEvent serviceDownstreamResponseEvent) {
        assertTrue(serviceDownstreamResponseEvent instanceof ServiceDownstreamResponseEvent);
        assertEquals(METHOD, serviceDownstreamResponseEvent.getOperation());
        assertEquals(SOME_URI, serviceDownstreamResponseEvent.getService());
        assertEquals("ApacheHttpClient", serviceDownstreamResponseEvent.getOrigin());
    }
}
