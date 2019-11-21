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

package software.amazon.disco.application.example;

import org.apache.http.client.methods.HttpUriRequest;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.HttpServletNetworkRequestEvent;
import software.amazon.disco.agent.event.HttpServletNetworkResponseEvent;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.TransactionBeginEvent;
import software.amazon.disco.agent.event.TransactionEndEvent;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;

public class ExampleTest {
    /**
     * Just a small test to create a Thread, assert that some context survives the hand off into the child thread
     */
    @Test
    public void testAgentThreadInterception() throws Exception {
        TransactionContext.clear();
        TransactionContext.set("tx_value");
        AtomicBoolean correctValue = new AtomicBoolean();
        Thread t = new Thread(()->{
            correctValue.set(TransactionContext.get().equals("tx_value"));
        });
        t.start();
        t.join();
        Assert.assertTrue(correctValue.get());
    }

    /**
     * Test that our agent installed the Servlet support from the web package.
     * @throws Exception
     */
    @Test
    public void testServletInterception() throws Exception {
        MyEventListener l = new MyEventListener();
        EventBus.addListener(l);
        MyServlet servlet = new MyServlet();

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        servlet.service(req, resp);
        Assert.assertTrue(servlet.called);
        Assert.assertEquals(4, l.events.size());
        Assert.assertTrue(l.events.get(0) instanceof TransactionBeginEvent);
        Assert.assertTrue(l.events.get(1) instanceof HttpServletNetworkRequestEvent);
        Assert.assertTrue(l.events.get(2) instanceof HttpServletNetworkResponseEvent);
        Assert.assertTrue(l.events.get(3) instanceof TransactionEndEvent);
        EventBus.removeListener(l);
    }

    /**
     * Test that our agent installed the Apache Http Client support from the web package.
     * @throws IOException
     */
    @Test
    public void testHttpClientInterception() throws IOException {
        MyEventListener l = new MyEventListener();
        EventBus.addListener(l);

        MyHttpClient client = new MyHttpClient();
        HttpUriRequest request = mock(HttpUriRequest.class);
        client.execute(request);
        Assert.assertTrue(client.executeCallChainDepth > 0);
        Assert.assertEquals(2, l.events.size());
        Assert.assertTrue(l.events.get(0) instanceof ServiceDownstreamRequestEvent);
        Assert.assertTrue(l.events.get(1) instanceof ServiceDownstreamResponseEvent);
        EventBus.removeListener(l);
    }

    /**
     * Test that our agent installed the Apache Http Async Client support from the web package.
     * @throws IOException
     */
    @Test
    public void testHttpAsyncClientInterception() throws IOException {
        MyEventListener l = new MyEventListener();
        EventBus.addListener(l);

        MyHttpAsyncClient client = new MyHttpAsyncClient();
        HttpUriRequest request = mock(HttpUriRequest.class);
        client.execute(request, null);
        Assert.assertTrue(client.executeCallChainDepth > 0);
        Assert.assertEquals(2, l.events.size());
        Assert.assertTrue(l.events.get(0) instanceof ServiceDownstreamRequestEvent);
        Assert.assertTrue(l.events.get(1) instanceof ServiceDownstreamResponseEvent);
        EventBus.removeListener(l);
    }

    static class MyEventListener implements Listener {
        public List<Event> events = new LinkedList<>();

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            events.add(e);
        }
    }
}
