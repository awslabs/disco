package com.amazon.disco.agent.event;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.SortedMap;

public class EventBusTests {
    private Listener listener;

    @Before
    public void before() {
        EventBus.removeAllListeners();
        listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPriority()).thenReturn(0);
    }

    @After
    public void after() {
        EventBus.removeAllListeners();
    }

    @Test
    public void testSortedMap() {
        Assert.assertTrue(SortedMap.class.isAssignableFrom(EventBus.listeners.getClass()));
    }

    @Test
    public void testAddListener() {
        EventBus.addListener(listener);
        Assert.assertEquals(listener, EventBus.listeners.get(0).iterator().next());
    }

    @Test
    public void testRemovePresentListener() {
        EventBus.addListener(listener);
        EventBus.removeListener(listener);
        Assert.assertTrue(EventBus.listeners.get(0).isEmpty());
    }

    @Test
    public void testRemoveAbsentListener() {
        Assert.assertEquals(null, EventBus.listeners.get(0));
        EventBus.removeListener(listener);
        Assert.assertEquals(null, EventBus.listeners.get(0));
    }

    @Test
    public void testRemoveAllListeners() {
        Listener listener2 = Mockito.mock(Listener.class);
        Mockito.when(listener2.getPriority()).thenReturn(0);
        EventBus.addListener(listener);
        EventBus.addListener(listener2);
        Assert.assertEquals(2, EventBus.listeners.get(0).size());
        EventBus.removeAllListeners();
        Assert.assertEquals(null, EventBus.listeners.get(0));
    }

    @Test
    public void testPublishEventToListener() {
        MyListener listener1 = new MyListener();
        EventBus.addListener(listener1);
        Event event = Mockito.mock(Event.class);
        EventBus.publish(event);
        Assert.assertEquals(event, listener1.received);
    }

    @Test
    public void testPublishEventToMultipleListeners() {
        MyListener listener1 = new MyListener();
        MyListener listener2 = new MyListener();
        EventBus.addListener(listener1);
        EventBus.addListener(listener2);
        Event event = Mockito.mock(Event.class);
        EventBus.publish(event);
        Assert.assertEquals(event, listener1.received);
        Assert.assertEquals(event, listener2.received);
    }

    @Test
    public void testPresentListenerIsPresent() {
        Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPriority()).thenReturn(0);
        EventBus.addListener(listener);
        Assert.assertTrue(EventBus.isListenerPresent(listener));
    }

    @Test
    public void testAbsentListenerIsAbsentWhenNoListenersOfItsPriority() {
        Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPriority()).thenReturn(0);
        Assert.assertFalse(EventBus.isListenerPresent(listener));
    }

    @Test
    public void testAbsentListenerIsAbsentWhenOtherListenersOfSamePriority() {
        Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPriority()).thenReturn(0);

        Listener otherListener = Mockito.mock(Listener.class);
        Mockito.when(otherListener.getPriority()).thenReturn(0);

        EventBus.addListener(otherListener);
        Assert.assertFalse(EventBus.isListenerPresent(listener));
    }

    class MyListener implements Listener {
        Event received;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event event) {
            received = event;
        }
    }
}
