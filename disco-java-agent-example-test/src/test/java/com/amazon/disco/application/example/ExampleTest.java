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

package com.amazon.disco.application.example;

import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.HttpServletNetworkRequestEvent;
import com.amazon.disco.agent.event.HttpServletNetworkResponseEvent;
import com.amazon.disco.agent.event.Listener;
import com.amazon.disco.agent.reflect.concurrent.TransactionContext;
import com.amazon.disco.agent.reflect.event.EventBus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
        Assert.assertEquals(2, l.events.size());
        Assert.assertTrue(l.events.get(0) instanceof HttpServletNetworkRequestEvent);
        Assert.assertTrue(l.events.get(1) instanceof HttpServletNetworkResponseEvent);
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
