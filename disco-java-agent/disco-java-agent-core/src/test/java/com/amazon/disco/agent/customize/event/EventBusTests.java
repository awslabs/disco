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
