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

import software.amazon.disco.agent.integtest.concurrent.source.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Test that our Runnable/Callable strategy works for Java Executors, a common concurrency mechanism.
 */
public class ExecutorServiceTests {

    private static final List<ExecutorServiceFactory> executorServiceFactories = Arrays.asList(
        new FixedThreadPoolExecutorServiceFactory(),
        new ScheduledThreadPoolExecutorFactory(),
        new UserDecoratedExecutorFactory()
    );

    static abstract class Base {
        protected ExecutorService executorService;
        protected static final String result = "Result";

        @Before
        public void before() throws Exception {
            TestableConcurrencyObjectImpl.before();
            testMethodInterceptionCounter();
        }

        @After
        public void after() throws Exception {
            TestableConcurrencyObjectImpl.after();
            testMethodInterceptionCounter();
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

        @Parameterized.Parameter(2)
        public ExecutorServiceFactory executorServiceFactory;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {
            return executorServiceFactories.stream()
                .flatMap(e ->
                    TestRunnableFactory.Data.provideAllRunnables().stream()
                        .map(data -> {
                            String name = String.format("runnable:%s executor:%s", data[0], e.getClass().getSimpleName());
                            return new Object[]{name, data[1], e};
                        }))
                .collect(Collectors.toList());
        }
    }

    public static abstract class CallableBase extends Base {
        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestCallableFactory.NonThrowingTestableCallable testableCallable;

        @Parameterized.Parameter(2)
        public ExecutorServiceFactory executorServiceFactory;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {
            return executorServiceFactories.stream()
                .flatMap(e ->
                    TestCallableFactory.Data.provideAllCallables().stream()
                        .map(data -> {
                            String name = String.format("callable:%s executor:%s", data[0], e.getClass().getSimpleName());
                            return new Object[]{name, data[1], e};
                        }))
                .collect(Collectors.toList());
        }
    }

    @RunWith(Parameterized.class)
    public static class ExecuteRunnable extends RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testExecuteRunnable() throws Exception {
            executorService = executorServiceFactory.createExecutorService();
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
            executorService = executorServiceFactory.createExecutorService();
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
            executorService = executorServiceFactory.createExecutorService();
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
            executorService = executorServiceFactory.createExecutorService();
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
            executorService = executorServiceFactory.createExecutorService();
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
            executorService = executorServiceFactory.createExecutorService();
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
            executorService = executorServiceFactory.createExecutorService();
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
            executorService = executorServiceFactory.createExecutorService();
            testBeforeInvocation(testableCallable);
            List<Callable<String>> callables = new LinkedList<>();
            callables.add(testableCallable.callable);
            String result = executorService.invokeAny(callables, 1, TimeUnit.DAYS);
            testAfterInvocation(testableCallable, result, result);
        }
    }

    public static class SubmitRunnableWhenThrows extends Base {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testSubmitRunnableWhenThrows() throws Exception {
            ThrowingRunnable r = new ThrowingRunnable();
            r.testBeforeInvocation();
            executorService = Executors.newFixedThreadPool(2);

            Future<?> f = executorService.submit(r);
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

        static class ThrowingRunnable extends TestableConcurrencyObjectImpl.WhichThrows implements Runnable {
            @Override
            public void run() {
                perform();
            }
        }
    }

    public static class SubmitRunnableNestedWhenExecutorReusesThread extends RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testSubmitRunnableNestedWhenExecutorReusesThread() throws Exception {
            ExecutorService e = Executors.newFixedThreadPool(1);
            AbstractQueue<Future<?>> futures = new ConcurrentLinkedQueue<>();
            long[] threadIdStack = new long[3];
            threadIdStack[0] = Thread.currentThread().getId();
            TransactionContext.putMetadata("foo", "bar");
            futures.add(e.submit(() -> {
                TransactionContext.putMetadata("foo2", "bar2");
                threadIdStack[1] = Thread.currentThread().getId();
                futures.add(e.submit(() -> {
                    threadIdStack[2] = Thread.currentThread().getId();
                    Assert.assertEquals("bar", TransactionContext.getMetadata("foo"));
                    Assert.assertEquals("bar2", TransactionContext.getMetadata("foo2"));
                }));
            }));

            Future<?> f;
            while ((f = futures.poll()) != null) {
                f.get(1, TimeUnit.DAYS);
            }

            //need to retry test in the race condition case where a thread was not in fact reused. i.e. out of the 3 thread ids, 2 (or all 3) must be the same.
            if(!(threadIdStack[0]==threadIdStack[1] || threadIdStack[1]==threadIdStack[2] || threadIdStack[0]==threadIdStack[2])) {
                throw new ConcurrencyCanBeRetriedException();
            }
        }
    }

    @Test
    public void testSubmitRunnableToUserDecoratedExecutorFactoryWhenExecuteExitsNormally() throws Exception {
        testMethodInterceptionCounter();

        Runnable r = ()->{};
        ExecutorService executorService = new UserDecoratedExecutorFactory.UserDecoratedExecutor();

        Future<?> f = executorService.submit(r);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
        f.get();

        testMethodInterceptionCounter();
    }

    @Test
    public void testSubmitRunnableToUserDecoratedExecutorFactoryWhenExecuteThrows() throws Exception {
        testMethodInterceptionCounter();

        Runnable r = ()->{};
        ExecutorService executorService = new UserDecoratedExecutorFactory.UserDecoratedExecutor(new RuntimeException());

        try {
            Future<?> f = executorService.submit(r);
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.DAYS);
            f.get();
        } catch (RuntimeException e) {
            //do nothing
        }

        testMethodInterceptionCounter();
    }

    private static void testMethodInterceptionCounter() throws Exception {
        //invasive reflection not ideal here, but we need to check that the exit advice was called, ensuring that
        //reentrancy counter was left at zero
        Object interceptionCounter = Class.forName("software.amazon.disco.agent.concurrent.ExecutorInterceptor$ExecuteAdvice").getDeclaredField("interceptionCounter").get(null);
        Method hasIntercepted = Class.forName("software.amazon.disco.agent.interception.MethodInterceptionCounter").getDeclaredMethod("hasIntercepted");
        boolean result = (boolean)hasIntercepted.invoke(interceptionCounter);
        Assert.assertFalse(result);
    }
}
