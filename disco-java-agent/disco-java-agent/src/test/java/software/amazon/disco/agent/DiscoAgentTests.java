/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;
import software.amazon.disco.agent.reflect.metrics.DiscoAgentMetrics;

import java.util.ArrayList;
import java.util.List;

public class DiscoAgentTests {
    @Test
    public void testAgentDeduplication() throws Exception {
        TransactionContext.create();
        TestListener listener = new TestListener();
        EventBus.addListener(listener);

        //first test that the agent is present and deduped when supplied twice on the command line
        //Core has a logger allowing us to spy and verify that
        Assert.assertEquals(1, TestLoggerFactory.INSTANCE.messages.size());

        // Force a pair of Core events to occur, and establish that Listener was called only once since no double-instrumentation can have occurred
        causeThreadEvent();
        Assert.assertEquals(2, listener.events.size());
        Assert.assertTrue(listener.events.get(0) instanceof ThreadEnterEvent);
        Assert.assertTrue(listener.events.get(1) instanceof ThreadExitEvent);

        //now inject the agent again to show that deduping works for both command line supplied use cases, and injected use cases
        Injector.loadAgent(System.getProperty("discoAgentPath"), null);

        //this third attempt at loading the agent will produce a single extra line of log again
        TestLoggerFactory.INSTANCE.reset();
        Assert.assertEquals(1, TestLoggerFactory.INSTANCE.messages.size());

        //final check, produce another single pair of events
        listener.events.clear();
        causeThreadEvent();
        Assert.assertEquals(2, listener.events.size());
        Assert.assertTrue(listener.events.get(0) instanceof ThreadEnterEvent);
        Assert.assertTrue(listener.events.get(1) instanceof ThreadExitEvent);
        TransactionContext.destroy();
    }

    @Test
    public void testAgentStartTimeSet() {
        Assert.assertTrue(DiscoAgentMetrics.getAgentUptime() > 0L);
    }

    static class TestListener implements Listener {
        List<Event> events = new ArrayList<>();
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            events.add(e);
        }
    }

    private static void causeThreadEvent() throws Exception {
        Thread t = new Thread(()->{});
        t.start();
        t.join();
    }
}
