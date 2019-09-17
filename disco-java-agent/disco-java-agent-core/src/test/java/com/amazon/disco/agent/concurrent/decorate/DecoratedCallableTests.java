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
import org.mockito.Mockito;

import java.util.concurrent.Callable;

public class DecoratedCallableTests {
    private TestListener testListener = null;
    @Before
    public void before() {
        TransactionContext.create();
        EventBus.addListener(testListener = new TestListener());
    }

    @After
    public void after() {
        EventBus.removeListener(testListener);
        TransactionContext.clear();
    }


    @Test
    public void testDecoration() {
        Callable c = Mockito.mock(Callable.class);
        Callable d = DecoratedCallable.maybeCreate(c);
        Assert.assertNotEquals(c, d);
        Assert.assertTrue(d instanceof DecoratedCallable);
        Assert.assertEquals(c, ((DecoratedCallable)d).target);
    }

    @Test
    public void testDoubleDecoration() {
        Callable c = Mockito.mock(Callable.class);
        Callable d = DecoratedCallable.maybeCreate(c);
        Callable dd = DecoratedCallable.maybeCreate(d);
        Assert.assertEquals(d, dd);
    }

    @Test
    public void testNullDecoration() {
        Assert.assertNull(DecoratedCallable.maybeCreate(null));
    }

    @Test
    public void testCall() throws Exception {
        Callable c = Mockito.mock(Callable.class);
        DecoratedCallable d = (DecoratedCallable)DecoratedCallable.maybeCreate(c);
        d.parentThreadId = -1L;
        d.call();
        Assert.assertNotNull(testListener.threadEnter);
        Assert.assertNotNull(testListener.threadExit);
    }

    @Test
    public void testCallWhenThrowsException() {
        Callable c = ()->{throw new RuntimeException();};
        DecoratedCallable d = (DecoratedCallable)DecoratedCallable.maybeCreate(c);
        d.parentThreadId = -1L;
        Throwable thrown = null;
        try {
            d.call();
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertTrue(thrown instanceof RuntimeException);
        Assert.assertNotNull(testListener.threadEnter);
        Assert.assertNotNull(testListener.threadExit);
    }

    @Test
    public void testCallWhenThrowsError() {
        Callable c = ()->{throw new Error();};
        DecoratedCallable d = (DecoratedCallable)DecoratedCallable.maybeCreate(c);
        d.parentThreadId = -1L;
        Throwable thrown = null;
        try {
            d.call();
        } catch (Throwable e) {
            thrown = e;
        }
        Assert.assertTrue(thrown instanceof Error);
        Assert.assertNotNull(testListener.threadEnter);
        Assert.assertNotNull(testListener.threadExit);
    }

    static class TestListener implements Listener {
        Event threadEnter = null;
        Event threadExit = null;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ThreadEnterEvent) {
                threadEnter = e;
            } else if (e instanceof ThreadExitEvent) {
                threadExit = e;
            }
        }
    }
}
