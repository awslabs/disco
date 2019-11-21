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

package software.amazon.disco.agent.integtest.web.apache.httpasyncclient;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import software.amazon.disco.agent.event.ThreadEvent;
import software.amazon.disco.agent.integtest.web.apache.httpasyncclient.source.FakeAsyncClient;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ApacheHttpAsyncClientInterceptorTests {

    private TestListener testListener;

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
        CountDownLatch lock = new CountDownLatch(1);
        try (CloseableHttpAsyncClient client = HttpAsyncClients.createMinimal()) {
            client.start();
            HttpGet request = new HttpGet("https://amazon.com");
            Future<HttpResponse> future = client.execute(request, null);

            waitForResponseOrTimeoutWithFuture(lock, future);
        }

        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    public void testMinimalClientWithCallback() throws Exception {
        CountDownLatch lock = new CountDownLatch(1);
        try (CloseableHttpAsyncClient client = HttpAsyncClients.createMinimal()) {
            client.start();
            HttpGet request = new HttpGet("https://amazon.com");
            client.execute(request, getTestFutureCallback(lock));

            waitForResponseOrTimeoutWithExistingCallback(lock);
        }

        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    public void testDefaultClient() throws Exception {
        CountDownLatch lock = new CountDownLatch(1);
        try (CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {
            client.start();
            HttpGet request = new HttpGet("https://amazon.com");
            Future<HttpResponse> future = client.execute(request, null);

            waitForResponseOrTimeoutWithFuture(lock, future);
        }

        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    public void testDefaultClientWithCallback() throws Exception {
        CountDownLatch lock = new CountDownLatch(1);
        try (CloseableHttpAsyncClient client = HttpAsyncClients.createDefault()) {
            client.start();
            HttpGet request = new HttpGet("https://amazon.com");
            client.execute(request, getTestFutureCallback(lock));

            waitForResponseOrTimeoutWithExistingCallback(lock);
        }

        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
    }

    @Test
    @UseDataProvider("futureCallbacks")
    public void testInterceptorSucceededOnCompleted(FutureCallback<HttpResponse> futureCallback) {
        HttpUriRequest request = mockRequest();
        FakeAsyncClient client = new FakeAsyncClient(FakeAsyncClient.DesiredState.CALLBACK_COMPLETED);
        client.execute(request, futureCallback);

        assertTrue(client.executeCallChainDepth > 0);
        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
        assertTrue(testListener.requestEvents.get(0) instanceof ServiceDownstreamRequestEvent);
        // Verify the Request Event
        verifyServiceRequestEvent(testListener.requestEvents.get(0));

        assertTrue(testListener.responseEvents.get(0) instanceof HttpServiceDownstreamResponseEvent);
        // Verify the Response Event
        verifyServiceResponseEvent(testListener.responseEvents.get(0));
        assertNull(testListener.responseEvents.get(0).getResponse());
        assertNull(testListener.responseEvents.get(0).getThrown());
        assertEquals(FakeAsyncClient.fakeResponse.getStatusLine().getStatusCode(), testListener.responseEvents.get(0).getStatusCode());
    }

    @Test
    @UseDataProvider("futureCallbacks")
    public void testInterceptorSucceededOnFailed(FutureCallback<HttpResponse> futureCallback) {
        HttpUriRequest request = mockRequest();
        FakeAsyncClient client = new FakeAsyncClient(FakeAsyncClient.DesiredState.CALLBACK_FAILED);
        client.execute(request, futureCallback);

        assertTrue(client.executeCallChainDepth > 0);
        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
        assertTrue(testListener.requestEvents.get(0) instanceof ServiceDownstreamRequestEvent);
        // Verify the Request Event
        verifyServiceRequestEvent(testListener.requestEvents.get(0));

        assertTrue(testListener.responseEvents.get(0) instanceof HttpServiceDownstreamResponseEvent);
        // Verify the Response Event
        verifyServiceResponseEvent(testListener.responseEvents.get(0));
        assertNull(testListener.responseEvents.get(0).getResponse());
        assertEquals(FakeAsyncClient.fakeException, testListener.responseEvents.get(0).getThrown());
    }

    @Test
    @UseDataProvider("futureCallbacks")
    public void testInterceptorSucceededOnCancelled(FutureCallback<HttpResponse> futureCallback) {
        HttpUriRequest request = mockRequest();
        FakeAsyncClient client = new FakeAsyncClient(FakeAsyncClient.DesiredState.CALLBACK_CANCELLED);
        client.execute(request, futureCallback);

        assertTrue(client.executeCallChainDepth > 0);
        assertEquals(1, testListener.requestEvents.size());
        assertEquals(1, testListener.responseEvents.size());
        assertTrue(testListener.requestEvents.get(0) instanceof ServiceDownstreamRequestEvent);
        // Verify the Request Event
        verifyServiceRequestEvent(testListener.requestEvents.get(0));

        assertTrue(testListener.responseEvents.get(0) instanceof HttpServiceDownstreamResponseEvent);
        // Verify the Response Event
        verifyServiceResponseEvent(testListener.responseEvents.get(0));
        assertNull(testListener.responseEvents.get(0).getResponse());
        assertNull(testListener.responseEvents.get(0).getThrown());
    }

    @Test(expected = FakeAsyncClient.FakeRuntimeException.class)
    @UseDataProvider("futureCallbacks")
    public void testInterceptorSucceededOnThrowable(FutureCallback<HttpResponse> futureCallback) {
        HttpUriRequest request = mockRequest();
        FakeAsyncClient client = new FakeAsyncClient(FakeAsyncClient.DesiredState.THROWABLE_THROWN);

        try {
            client.execute(request, futureCallback);
        } finally {
            assertTrue(client.executeCallChainDepth > 0);
            assertEquals(1, testListener.requestEvents.size());
            assertEquals(1, testListener.responseEvents.size());
            assertTrue(testListener.requestEvents.get(0) instanceof ServiceDownstreamRequestEvent);
            // Verify the Request Event
            verifyServiceRequestEvent(testListener.requestEvents.get(0));

            assertTrue(testListener.responseEvents.get(0) instanceof HttpServiceDownstreamResponseEvent);
            // Verify the Response Event
            HttpServiceDownstreamResponseEvent serviceDownstreamResponseEvent = testListener.responseEvents.get(0);
            assertNull(serviceDownstreamResponseEvent.getOperation());
            assertNull(serviceDownstreamResponseEvent.getService());
            assertEquals("ApacheHttpAsyncClient", serviceDownstreamResponseEvent.getOrigin());
            assertNull(testListener.responseEvents.get(0).getResponse());
            assertEquals(FakeAsyncClient.fakeException, testListener.responseEvents.get(0).getThrown());
        }
    }

    @DataProvider
    public static Object[][] futureCallbacks() {
        return new Object[][] {
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
        };
    }

    private void waitForResponseOrTimeoutWithExistingCallback(CountDownLatch lock) throws InterruptedException {
        waitForResponseOrTimeoutWithFuture(lock, null);
    }
    /**
     * Leave @param future to be null if {@link CountDownLatch#countDown()} is set
     * e.g. set it inside our test {@link FutureCallback}
     * Otherwise, a {@link Runnable} task will cover it.
     */
    private void waitForResponseOrTimeoutWithFuture(CountDownLatch lock, Future<HttpResponse> future) throws InterruptedException {
        if (future != null) {
            Executors.newSingleThreadExecutor().submit(() -> {
                while (!future.isDone()) { }
                lock.countDown();
            });
        }
        lock.await(1, TimeUnit.MINUTES);
    }

    private FutureCallback<HttpResponse> getTestFutureCallback(CountDownLatch lock) {
        return new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse result) {
                lock.countDown();
            }

            @Override
            public void failed(final Exception ex) {
                lock.countDown();
            }

            @Override
            public void cancelled() {
                lock.countDown();
            }
        };
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
            } else if (e instanceof ThreadEvent) {
                // expected, ignore
            } else {
                Assert.fail("Unexpected event");
            }
        }
    }

    private static HttpUriRequest mockRequest() {
        HttpUriRequest request = mock(HttpUriRequest.class);
        RequestLine requestLine = mock(RequestLine.class);

        when(request.getRequestLine()).thenReturn(requestLine);
        when(requestLine.getUri()).thenReturn(SOME_URI);
        when(requestLine.getMethod()).thenReturn(METHOD);

        return request;
    }


    private static void verifyServiceRequestEvent(final ServiceRequestEvent serviceDownstreamRequestEvent) {
        assertTrue(serviceDownstreamRequestEvent instanceof ServiceDownstreamRequestEvent);
        assertEquals(METHOD, serviceDownstreamRequestEvent.getOperation());
        assertEquals(SOME_URI, serviceDownstreamRequestEvent.getService());
        assertEquals("ApacheHttpAsyncClient", serviceDownstreamRequestEvent.getOrigin());
        assertNull(serviceDownstreamRequestEvent.getRequest());
    }

    private static void verifyServiceResponseEvent(final ServiceResponseEvent serviceDownstreamResponseEvent) {
        assertTrue(serviceDownstreamResponseEvent instanceof ServiceDownstreamResponseEvent);
        assertEquals(METHOD, serviceDownstreamResponseEvent.getOperation());
        assertEquals(SOME_URI, serviceDownstreamResponseEvent.getService());
        assertEquals("ApacheHttpAsyncClient", serviceDownstreamResponseEvent.getOrigin());
    }
}
