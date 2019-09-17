package com.amazon.disco.agent.customize.event;

import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.Listener;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class EventBusTests {
    @Test
    public void testPublishWhenAlphaOneNotLoaded() {
        EventBus.publish(Mockito.mock(Event.class));
    }

    @Test
    public void testAddListenerWhenAlphaOneNotLoaded() {
        EventBus.addListener(Mockito.mock(Listener.class));
    }

    @Test
    public void testRemoveListenerWhenAlphaOneNotLoaded() {
        EventBus.removeListener(Mockito.mock(Listener.class));
    }

    @Test
    public void testRemoveAllListenersWhenAlphaOneNotLoaded() {
        EventBus.removeAllListeners();
    }

    @Test
    public void testIsListenerPresentWhenAlphaOneNotLoaded() {
        Listener listener = Mockito.mock(Listener.class);
        EventBus.addListener(listener);
        Assert.assertFalse(EventBus.isListenerPresent(listener));
    }
}
