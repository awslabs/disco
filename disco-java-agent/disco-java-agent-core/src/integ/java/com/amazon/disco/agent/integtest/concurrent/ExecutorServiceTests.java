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

package com.amazon.disco.agent.integtest.concurrent;

import com.amazon.disco.agent.integtest.concurrent.source.ForceConcurrency;
import com.amazon.disco.agent.integtest.concurrent.source.TestableConcurrencyObject;
import com.amazon.disco.agent.integtest.concurrent.source.TestableConcurrencyObjectImpl;
import com.amazon.disco.agent.integtest.concurrent.source.TestCallableFactory;
import com.amazon.disco.agent.integtest.concurrent.source.TestRunnableFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Test that our Runnable/Callable strategy works for a Java Executor, a common concurrency mechanism.
 */
public class ExecutorServiceTests {
    static abstract class Base {
        protected ExecutorService executorService;
        protected static final String result = "Result";

        @Before
        public void before() {
            TestableConcurrencyObjectImpl.before();
            executorService = Executors.newFixedThreadPool(2);
        }

        @After
        public void after() {
            TestableConcurrencyObjectImpl.after();
        }

        protected void testBeforeInvocation(TestableConcurrencyObject testable) {
            testable.testBeforeInvocation();
        }

        protected void testAfterInvocation(TestableConcurrencyObject testable, Object returned, Object expected) throws Exception {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.DAYS);
            Assert.assertEquals(expected, returned);
            testable.testAfterConcurrentInvocation();
        }
    }

    public static abstract class RunnableBase extends Base {
        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestRunnableFactory.TestableRunnable testableRunnable;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestRunnableFactory.Data.provideAllRunnables();}
    }

    public static abstract class CallableBase extends Base {
        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestCallableFactory.NonThrowingTestableCallable testableCallable;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestCallableFactory.Data.provideAllCallables();}
    }

    @RunWith(Parameterized.class)
    public static class ExecuteRunnable extends RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testExecuteRunnable() throws Exception {
            testBeforeInvocation(testableRunnable);
            executorService.execute(testableRunnable.getRunnable());
            testAfterInvocation(testableRunnable, null, null);
        }
    }

    @RunWith(Parameterized.class)
    public static class SubmitRunnable extends RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testSubmitRunnable() throws Exception {
            testBeforeInvocation(testableRunnable);
            Future<?> future = executorService.submit(testableRunnable.getRunnable());
            testAfterInvocation(testableRunnable, future.get(), null);
        }
    }

    @RunWith(Parameterized.class)
    public static class SubmitRunnableWithResult extends RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testSubmitRunnableWithResult() throws Exception {
            testBeforeInvocation(testableRunnable);
            Future<String> future = executorService.submit(testableRunnable.getRunnable(), result);
            testAfterInvocation(testableRunnable, future.get(), result);
        }
    }

    @RunWith(Parameterized.class)
    public static class SubmitCallable extends CallableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testSubmitCallable() throws Exception {
            testBeforeInvocation(testableCallable);
            Future<String> future = executorService.submit(testableCallable.callable);
            testAfterInvocation(testableCallable, future.get(), result);
        }
    }

    @RunWith(Parameterized.class)
    public static class InvokeAllCallable extends CallableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testInvokeAllCallable() throws Exception {
            testBeforeInvocation(testableCallable);
            List<Callable<String>> callables = new LinkedList<>();
            callables.add(testableCallable.callable);
            List<Future<String>> futures = executorService.invokeAll(callables);
            testAfterInvocation(testableCallable, futures.get(0).get(), result);
        }
    }

    @RunWith(Parameterized.class)
    public static class InvokeAllCallableWithTimeout extends CallableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testInvokeAllCallableWithTimeout() throws Exception {
            testBeforeInvocation(testableCallable);
            List<Callable<String>> callables = new LinkedList<>();
            callables.add(testableCallable.callable);
            List<Future<String>> futures = executorService.invokeAll(callables, 1, TimeUnit.DAYS);
            testAfterInvocation(testableCallable, futures.get(0).get(), result);
        }
    }

    @RunWith(Parameterized.class)
    public static class InvokeAnyCallable extends CallableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testInvokeAnyCallable() throws Exception {
            testBeforeInvocation(testableCallable);
            List<Callable<String>> callables = new LinkedList<>();
            callables.add(testableCallable.callable);
            String result = executorService.invokeAny(callables);
            testAfterInvocation(testableCallable, result, result);
        }
    }

    @RunWith(Parameterized.class)
    public static class InvokeAnyCallableWithTimeout extends CallableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testInvokeAnyCallableWithTimeout() throws Exception {
            testBeforeInvocation(testableCallable);
            List<Callable<String>> callables = new LinkedList<>();
            callables.add(testableCallable.callable);
            String result = executorService.invokeAny(callables, 1, TimeUnit.DAYS);
            testAfterInvocation(testableCallable, result, result);
        }
    }

    class SubmitRunnableWhenThrows extends Base {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testSubmitRunnableWhenThrows() throws Exception {
            ThrowingRunnable r = new ThrowingRunnable();
            r.testBeforeInvocation();

            Future f = executorService.submit(r);
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.DAYS);
            Throwable thrown = null;
            try {
                f.get();
            } catch (ExecutionException e) {
                thrown = e.getCause();
            }
            Assert.assertNotNull(thrown);
            Assert.assertTrue(thrown instanceof TestableConcurrencyObjectImpl.WhichThrows.TestableConcurrencyObjectException);
            r.testAfterConcurrentInvocation();
        }

        class ThrowingRunnable extends TestableConcurrencyObjectImpl.WhichThrows implements Runnable {
            @Override
            public void run() {
                perform();
            }
        }
    }
}
