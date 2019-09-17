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

public class DecoratedRunnableTests {
    private TestListener testListener = null;
    @Before
    public void before() {
        EventBus.addListener(testListener = new TestListener());
        TransactionContext.create();
    }

    @After
    public void after() {
        TransactionContext.clear();
        EventBus.removeListener(testListener);
    }

    @Test
    public void testDecoration() {
        Runnable r = Mockito.mock(Runnable.class);
        Runnable d = DecoratedRunnable.maybeCreate(r);
        Assert.assertNotEquals(r, d);
        Assert.assertTrue(d instanceof DecoratedRunnable);
        Assert.assertEquals(r, ((DecoratedRunnable)d).target);
    }

    @Test
    public void testDoubleDecoration() {
        Runnable r = Mockito.mock(Runnable.class);
        Runnable d = DecoratedRunnable.maybeCreate(r);
        Runnable dd = DecoratedRunnable.maybeCreate(d);
        Assert.assertEquals(d, dd);
    }

    @Test
    public void testNullDecoration() {
        Assert.assertNull(DecoratedRunnable.maybeCreate(null));
    }

    @Test
    public void testRun() throws Exception {
        Runnable r = ()->{};
        Runnable d = DecoratedRunnable.maybeCreate(r);
        Thread t = new Thread(d);
        t.start();
        t.join();
        Assert.assertNotNull(testListener.threadEnter);
        Assert.assertNotNull(testListener.threadExit);
    }

    @Test
    public void testRunWhenThrowsRTE() throws Exception {
        class UncaughtHolder {
            Throwable uncaught;
        }
        UncaughtHolder holder = new UncaughtHolder();
        Thread.UncaughtExceptionHandler h = (thread, exception)-> {holder.uncaught = exception;};

        Runnable r = ()->{throw new RuntimeException();};
        Runnable d = DecoratedRunnable.maybeCreate(r);
        Thread t = new Thread(d);
        t.setUncaughtExceptionHandler(h);
        t.start();
        t.join();
        Assert.assertNotNull(testListener.threadEnter);
        Assert.assertNotNull(testListener.threadExit);
        Assert.assertTrue(holder.uncaught instanceof RuntimeException);
    }

    @Test
    public void testRunWhenThrowsError() throws Exception {
        class UncaughtHolder {
            Throwable uncaught;
        }
        UncaughtHolder holder = new UncaughtHolder();
        Thread.UncaughtExceptionHandler h = (thread, exception)-> {holder.uncaught = exception;};

        Runnable r = ()->{throw new Error();};
        Runnable d = DecoratedRunnable.maybeCreate(r);
        Thread t = new Thread(d);
        t.setUncaughtExceptionHandler(h);
        t.start();
        t.join();
        Assert.assertNotNull(testListener.threadEnter);
        Assert.assertNotNull(testListener.threadExit);
        Assert.assertTrue(holder.uncaught instanceof Error);
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
