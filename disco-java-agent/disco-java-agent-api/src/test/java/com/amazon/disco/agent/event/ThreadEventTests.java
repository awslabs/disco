package com.amazon.disco.agent.event;

import org.junit.Assert;
import org.junit.Test;

public class ThreadEventTests {
    @Test
    public void testThreadEnterEvent() {
        ThreadEvent event = new ThreadEnterEvent("Origin", 1L, 2L);
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(1L, event.getParentId());
        Assert.assertEquals(2L, event.getChildId());
        Assert.assertEquals(ThreadEvent.Operation.ENTERING, event.getOperation());
    }

    @Test
    public void testThreadExitEvent() {
        ThreadEvent event = new ThreadExitEvent("Origin", 1L, 2L);
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(1L, event.getParentId());
        Assert.assertEquals(2L, event.getChildId());
        Assert.assertEquals(ThreadEvent.Operation.EXITING, event.getOperation());
    }
}
