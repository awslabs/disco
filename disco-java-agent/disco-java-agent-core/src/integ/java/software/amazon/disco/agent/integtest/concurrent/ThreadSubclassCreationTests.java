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

package software.amazon.disco.agent.integtest.concurrent;

import software.amazon.disco.agent.integtest.concurrent.source.TestRunnableFactory;
import software.amazon.disco.agent.integtest.concurrent.source.TestableConcurrencyObjectImpl;
import software.amazon.disco.agent.integtest.concurrent.source.ThreadSubclass;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.reflect.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test the propagation of TransactionContext when manually creating new subclasses of Threads
 */
public class ThreadSubclassCreationTests {
    @Before
    public void before() {
        TestableConcurrencyObjectImpl.before();
    }

    @After
    public void after() {
        TestableConcurrencyObjectImpl.after();
    }

    @Test()
    public void testSubclassThreadStart() throws Exception {
        ThreadSubclass t = new ThreadSubclass();
        TestRunnableFactory.TestableRunnable r = new TestRunnableFactory.NonThrowingTestableRunnable();
        t.testableRunnable = r;
        r.testBeforeInvocation();
        t.start();
        t.join();
        r.testAfterConcurrentInvocation();
    }

    @Test()
    public void testSubclassThreadStartWhenThrowsRTE() throws Exception {
        ThreadSubclass t = new ThreadSubclass();
        class UncaughtHolder {
            Throwable uncaught;
        }
        UncaughtHolder holder = new UncaughtHolder();
        Thread.UncaughtExceptionHandler h = (thread, exception)-> {holder.uncaught = exception;};
        t.setUncaughtExceptionHandler(h);
        TestRunnableFactory.TestableRunnable r = new TestRunnableFactory.ThrowingTestableRunnable(new RuntimeException());
        t.testableRunnable = r;
        r.testBeforeInvocation();
        t.start();
        t.join();
        r.testAfterConcurrentInvocation();
        Assert.assertTrue(holder.uncaught instanceof RuntimeException);
    }

    @Test()
    public void testSubclassThreadStartWhenThrowsError() throws Exception {
        ThreadSubclass t = new ThreadSubclass();
        class UncaughtHolder {
            Throwable uncaught;
        }
        UncaughtHolder holder = new UncaughtHolder();
        Thread.UncaughtExceptionHandler h = (thread, exception)-> {holder.uncaught = exception;};
        t.setUncaughtExceptionHandler(h);
        TestRunnableFactory.TestableRunnable r = new TestRunnableFactory.ThrowingTestableRunnable(new Error());
        t.testableRunnable = r;
        r.testBeforeInvocation();
        t.start();
        t.join();
        r.testAfterConcurrentInvocation();
        Assert.assertTrue(holder.uncaught instanceof Error);
    }

    @Test()
    public void testSubclassThreadRun() {
        AtomicBoolean failed = new AtomicBoolean(false);

        //ensure no logging during this operation.
        Logger.installLoggerFactory(name -> new software.amazon.disco.agent.logging.Logger() {
            @Override
            public void log(Level level, String message) {
                log(level, message, null);
            }

            @Override
            public void log(Level level, Throwable t) {
                log(level, null, t);
            }

            @Override
            public void log(Level level, String message, Throwable t) {
                if (level.ordinal() >= Level.INFO.ordinal()) {
                    failed.set(true);
                }
            }
        });

        ThreadSubclass t = new ThreadSubclass();
        TestRunnableFactory.TestableRunnable r = new TestRunnableFactory.NonThrowingTestableRunnable();
        t.testableRunnable = r;
        r.testBeforeInvocation();
        t.run();
        Logger.installLoggerFactory(null);
        r.testAfterSingleThreadedInvocation();
        Assert.assertFalse(failed.get());
    }
}
