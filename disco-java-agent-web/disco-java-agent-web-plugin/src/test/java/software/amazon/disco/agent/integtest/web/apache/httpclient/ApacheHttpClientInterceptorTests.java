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

import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicRequestLine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.event.DownstreamRequestHeaderRetrievable;
import software.amazon.disco.agent.event.DownstreamResponseHeaderRetrievable;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import software.amazon.disco.agent.integtest.web.apache.httpclient.source.FakeChainedExecuteCallHttpClientReturnResponse;
import software.amazon.disco.agent.integtest.web.apache.httpclient.source.FakeChainedExecuteCallHttpClientThrowException;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApacheHttpClientInterceptorTests {
    private TestListener testListener;

    private HttpUriRequest request;

    private static final String SOME_URI = "http://amazon.com/explore/something";
    private static final String METHOD = "GET";

    @Before
    public void before() throws URISyntaxException {
        TransactionContext.create();
        EventBus.addListener(testListener = new TestListener());
        request = new HttpGet(new URI(SOME_URI));
        request.addHeader("someheader", "somedata");
        request.addHeader("someheader2", "somedata2");
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
    public void testMinimalClientWithResponseHandler() throws Exception {
        ResponseHandler<Object> responseHandler = Mockito.mock(ResponseHandler.class);
        try (CloseableHttpClient httpClient = HttpClients.createMinimal()) {
            HttpGet request = new HttpGet("https://amazon.com");

            try {
                Mockito.when(responseHandler.handleResponse(Mockito.any())).thenReturn(new Object());
                httpClient.execute(request, responseHandler);
            } catch (IOException e) {
                //swallow
            } catch (Exception e) {
                Assert.fail();
            }
        }

        Mockito.verify(responseHandler).handleResponse(Mockito.any());
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
    public void testDefaultClientWithResponseHandler() throws Exception {
        ResponseHandler<Object> responseHandler = Mockito.mock(ResponseHandler.class);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://amazon.com");
            try {
                Mockito.when(responseHandler.handleResponse(Mockito.any())).thenReturn(new Object());
                httpClient.execute(request, responseHandler);
            } catch (IOException e) {
                //swallow
            } catch (Exception e) {
                Assert.fail();
            }
        }

        Mockito.verify(responseHandler).handleResponse(Mockito.any());
        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    public void testDefaultClientWithBasicHttpRequest() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            BasicHttpRequest request = new BasicHttpRequest(new BasicRequestLine("GET", "https://amazon.com", new ProtocolVersion("protocol", 1, 1)));
            try {
                httpClient.execute(new HttpHost("host", 80), request);
            } catch (IOException e) {
                //swallow
            }
        }

        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    public void testGetRequestLineNotCalledForGet() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://amazon.com") {
                @Override
                public RequestLine getRequestLine() {
                    //return the incorrect details to prove that this request line was not the source of the event data
                    return new BasicRequestLine("WRONG", "http://wrong.com", new ProtocolVersion("protocol", 1, 1));
                }
            };
            try {
                httpClient.execute(request);
            } catch (IOException e) {
                //swallow
            }
        }

        HttpServiceDownstreamRequestEvent event = (HttpServiceDownstreamRequestEvent)testListener.requestEvents.get(0);
        Assert.assertEquals("GET", event.getMethod());
        Assert.assertEquals("https://amazon.com", event.getUri());

    }

    @Test
    public void testExecuteInterceptionChained() throws Exception {
        // Set up victim http client
        FakeChainedExecuteCallHttpClientReturnResponse httpClient = new FakeChainedExecuteCallHttpClientReturnResponse();
        httpClient.fakeResponse.setEntity(new StringEntity("123456"));
        httpClient.fakeResponse.addHeader("someheader", "somedata");
        httpClient.fakeResponse.addHeader("someheader2", "somedata2");
        httpClient.execute(request);

        // Verify only one of interceptions does the interceptor business logic even if there is a method chaining,
        // as a result, only two service downstream events are published (request/response)
        assertEquals(1, testListener.requestEvents.size());

        // Verify the Request Event
        verifyServiceRequestEvent(testListener.requestEvents.get(0));
        verifyRequestHeaderRetrievable((DownstreamRequestHeaderRetrievable) testListener.requestEvents.get(0));
        assertEquals(1, testListener.responseEvents.size());

        // Verify the Response Event
        verifyServiceResponseEvent(testListener.responseEvents.get(0));
        verifyResponseHeaderRetrievable((DownstreamResponseHeaderRetrievable) testListener.responseEvents.get(0));

        assertNull(testListener.responseEvents.get(0).getResponse());
        assertNull(testListener.responseEvents.get(0).getThrown());
        assertEquals(200, testListener.responseEvents.get(0).getStatusCode());
        assertEquals(6, testListener.responseEvents.get(0).getContentLength());

        assertEquals(3, httpClient.executeCallChainDepth);
    }

    @Test(expected = IOException.class)
    public void testExecuteInterceptionException() throws Exception {
        // Set up victim http client
        FakeChainedExecuteCallHttpClientThrowException httpClient = new FakeChainedExecuteCallHttpClientThrowException();

        try {
            httpClient.execute(request);
        } finally {
            // Verify only one of interceptions does the interceptor business logic even if there is a method chaining,
            // as a result, only two service downstream events are published (request/response)
            assertEquals(1, testListener.requestEvents.size());

            // Verify the Request Event
            verifyServiceRequestEvent(testListener.requestEvents.get(0));
            verifyRequestHeaderRetrievable((DownstreamRequestHeaderRetrievable) testListener.requestEvents.get(0));
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
        List<HttpServiceDownstreamResponseEvent> responseEvents = new ArrayList<>();

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ServiceRequestEvent) {
                requestEvents.add((ServiceRequestEvent) e);
            } else if (e instanceof HttpServiceDownstreamResponseEvent) {
                responseEvents.add((HttpServiceDownstreamResponseEvent) e);
            } else {
                Assert.fail("Unexpected event");
            }
        }
    }

    private static void verifyServiceRequestEvent(final ServiceRequestEvent serviceDownstreamRequestEvent) {
        assertTrue(serviceDownstreamRequestEvent instanceof ServiceDownstreamRequestEvent);
        assertEquals(METHOD, serviceDownstreamRequestEvent.getOperation());
        assertEquals(SOME_URI, serviceDownstreamRequestEvent.getService());
        assertEquals("ApacheHttpClient", serviceDownstreamRequestEvent.getOrigin());
        assertNull(serviceDownstreamRequestEvent.getRequest());
    }

    private static void verifyServiceResponseEvent(final ServiceResponseEvent serviceDownstreamResponseEvent) {
        assertTrue(serviceDownstreamResponseEvent instanceof ServiceDownstreamResponseEvent);
        assertEquals(METHOD, serviceDownstreamResponseEvent.getOperation());
        assertEquals(SOME_URI, serviceDownstreamResponseEvent.getService());
        assertEquals("ApacheHttpClient", serviceDownstreamResponseEvent.getOrigin());
    }

    public static void verifyRequestHeaderRetrievable(final DownstreamRequestHeaderRetrievable retrievable) {
        assertEquals(retrievable.getFirstHeader("someheader"), "somedata");
        assertEquals(retrievable.getFirstHeader("someheader2"), "somedata2");
        assertNull(retrievable.getHeaders("nonexistentheader"));
    }

    public static void verifyResponseHeaderRetrievable(final DownstreamResponseHeaderRetrievable retrievable) {
        assertEquals(retrievable.getFirstHeader("someheader"), "somedata");
        assertEquals(retrievable.getFirstHeader("someheader2"), "somedata2");
        assertNull(retrievable.getHeaders("nonexistentheader"));
    }
}
