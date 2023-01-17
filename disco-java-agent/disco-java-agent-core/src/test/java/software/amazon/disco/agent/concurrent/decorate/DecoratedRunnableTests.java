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

package software.amazon.disco.agent.concurrent.decorate;

import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEnterEvent;
import software.amazon.disco.agent.event.ThreadExitEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.function.BiFunction;

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
        Assert.assertEquals(r, ((DecoratedRunnable) d).target);
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
    public void testDecorationAndRemovesTX() {
        TransactionContext.putMetadata("key", "value");
        Runnable r = Mockito.mock(Runnable.class);

        DecoratedRunnable decorated = DecoratedRunnable.maybeCreate(r, true);
        decorated.after();

        Assert.assertNull(TransactionContext.getMetadata("key"));
    }

    @Test
    public void testDecorationAndNotRemovesTX() {
        TransactionContext.putMetadata("key", "value");
        Runnable r = Mockito.mock(Runnable.class);

        DecoratedRunnable decorated = DecoratedRunnable.maybeCreate(r, false);
        decorated.after();

        Assert.assertEquals("value", TransactionContext.getMetadata("key"));
    }

    @Test
    public void testDecorationFunctionAndRemovesTX() {
        BiFunction<Runnable, Boolean, Runnable> function = new DecoratedRunnable.RunnableDecorateFunction();
        TransactionContext.putMetadata("key", "value");
        Runnable r = Mockito.mock(Runnable.class);

        Runnable decorated = function.apply(r, true);

        Assert.assertTrue(decorated instanceof DecoratedRunnable);
        ((DecoratedRunnable) decorated).after();

        Assert.assertNull(TransactionContext.getMetadata("key"));
    }

    @Test
    public void testDecorationFunctionAndNotRemovesTX() {
        BiFunction<Runnable, Boolean, Runnable> function = new DecoratedRunnable.RunnableDecorateFunction();
        TransactionContext.putMetadata("key", "value");
        Runnable r = Mockito.mock(Runnable.class);

        Runnable decorated = function.apply(r, false);

        Assert.assertTrue(decorated instanceof DecoratedRunnable);
        ((DecoratedRunnable) decorated).after();

        Assert.assertEquals("value", TransactionContext.getMetadata("key"));
    }

    @Test
    public void testRun() throws Exception {
        Runnable r = () -> {
        };
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
        Thread.UncaughtExceptionHandler h = (thread, exception) -> {
            holder.uncaught = exception;
        };

        Runnable r = () -> {
            throw new RuntimeException();
        };
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
        Thread.UncaughtExceptionHandler h = (thread, exception) -> {
            holder.uncaught = exception;
        };

        Runnable r = () -> {
            throw new Error();
        };
        Runnable d = DecoratedRunnable.maybeCreate(r);
        Thread t = new Thread(d);
        t.setUncaughtExceptionHandler(h);
        t.start();
        t.join();
        Assert.assertNotNull(testListener.threadEnter);
        Assert.assertNotNull(testListener.threadExit);
        Assert.assertTrue(holder.uncaught instanceof Error);
    }

    @Test
    public void testEquals() throws Exception {
        Runnable r = () -> {
            throw new Error();
        };
        Runnable q = () -> {};
        DecoratedRunnable dr1 = DecoratedRunnable.maybeCreate(r);
        DecoratedRunnable dr2 = DecoratedRunnable.maybeCreate(r);
        Assert.assertTrue("DecoratedRunnable instances pointing to same target are equal",
                dr1.equals(dr2));

        DecoratedRunnable dq = DecoratedRunnable.maybeCreate(q);
        Assert.assertFalse("DecoratedRunnable instances pointing to different targets are not equal",
                dq.equals(dr1));
        Assert.assertFalse("DecoratedRunnable instance is never equal to null",
                dq.equals(null));
        Assert.assertFalse("DecoratedRunnable instance is not equal to its target",
                dq.equals(q));
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
