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

package com.amazon.disco.agent.customize.event;

import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.Listener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class EventBusTests {
    @Before
    public void before() {
        com.amazon.disco.agent.event.EventBus.removeAllListeners();
    }

    @After
    public void after() {
        com.amazon.disco.agent.event.EventBus.removeAllListeners();
    }

    @Test
    public void testAddListenerAndPublishWhenAlphaOneLoaded() {
        MyListener listener = new MyListener();
        Event event = Mockito.mock(Event.class);
        EventBus.addListener(listener);
        EventBus.publish(event);
        Assert.assertEquals(event, listener.received);
    }

    @Test
    public void testRemoveListenerWhenAlphaOneLoaded() {
        MyListener listener = new MyListener();
        Event event = Mockito.mock(Event.class);
        EventBus.addListener(listener);
        EventBus.removeListener(listener);
        EventBus.publish(event);
        Assert.assertNull(listener.received);
    }

    @Test
    public void testRemoveAllListenersWhenAlphaOneLoaded() {
        MyListener listener = new MyListener();
        Event event = Mockito.mock(Event.class);
        EventBus.addListener(listener);
        EventBus.removeAllListeners();
        EventBus.publish(event);
        Assert.assertNull(listener.received);
    }

    @Test
    public void testisListenerPresentWhenAlphaOneLoaded() {
        MyListener listener = new MyListener();
        Assert.assertFalse(EventBus.isListenerPresent(listener));
        EventBus.addListener(listener);
        Assert.assertTrue(EventBus.isListenerPresent(listener));

    }

    class MyListener implements Listener {
        Event received;
        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            received = e;
        }
    }
}
