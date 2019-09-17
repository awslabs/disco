package com.amazon.disco.agent.concurrent.decorate;

import com.amazon.disco.agent.concurrent.TransactionContext;
import com.amazon.disco.agent.event.Event;
import com.amazon.disco.agent.event.EventBus;
import com.amazon.disco.agent.event.Listener;
import com.amazon.disco.agent.event.ThreadEnterEvent;
import com.amazon.disco.agent.event.ThreadExitEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DecoratedTests {
    MyListener listener;

    @Before
    public void before() {
        EventBus.addListener(listener = new MyListener());
        TransactionContext.create();
    }

    @After
    public void after() {
        TransactionContext.clear();
        EventBus.removeListener(listener);
    }

    @Test
    public void testBeforeSameThread() {
        Decorated d = new MyDecorated();
        d.before();
        Assert.assertNull(listener.enter);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testAfterSameThread() {
        Decorated d = new MyDecorated();
        d.after();
        Assert.assertNull(listener.enter);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testBeforeDifferentThread() {
        Decorated d = new MyDecorated();
        d.parentThreadId = -1L;
        d.before();
        Assert.assertTrue(listener.enter instanceof ThreadEnterEvent);
        Assert.assertNull(listener.exit);
    }

    @Test
    public void testAfterDifferentThread() {
        Decorated d = new MyDecorated();
        d.parentThreadId = -1L;
        d.after();
        Assert.assertNull(listener.enter);
        Assert.assertTrue(listener.exit instanceof ThreadExitEvent);
    }

    static class MyDecorated extends Decorated {

    }

    static class MyListener implements Listener {
        Event enter = null;
        Event exit = null;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ThreadEnterEvent) {
                enter = e;
            } else if (e instanceof ThreadExitEvent) {
                exit = e;
            }
        }
    }
}
