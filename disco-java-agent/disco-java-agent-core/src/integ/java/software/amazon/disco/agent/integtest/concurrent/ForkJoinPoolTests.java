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

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.disco.agent.integtest.concurrent.source.ForceConcurrency;
import software.amazon.disco.agent.integtest.concurrent.source.ForkJoinTestBase;
import software.amazon.disco.agent.integtest.concurrent.source.TestCallableFactory;
import software.amazon.disco.agent.reflect.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(Enclosed.class)
public class ForkJoinPoolTests {
    /**
     * ForkJoinPool.commonPool has a built-in feature that allows the main thread to also participate in the "task stealing" exercise
     * upon invoking .join(), which prevented worker threads from being used to execute submitted tasks, effectively renders
     * Thread context propagation extremely difficult to test.
     */
    private static ForkJoinPool threadPool = new ForkJoinPool();
    
    @RunWith(Parameterized.class)
    public static class MultithreadedExecuteForkJoinTask extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolExecuteForkJoinTaskAndJoin() {
            testableForkJoinTask.testBeforeInvocation();

            threadPool.execute(testableForkJoinTask.forkJoinTask);
            testableForkJoinTask.forkJoinTask.join();
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedExecuteRunnable extends ForkJoinTestBase.RunnableBase {
        @After
        public void after(){
            // instantiate a new pool after each test case, e.g. Concrete, Anonymous, etc... since shutdown is invoked.
            threadPool = new ForkJoinPool();
        }

        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolExecuteRunnableAndAwait() throws Exception {
            testableRunnable.testBeforeInvocation();

            threadPool.execute(testableRunnable.getRunnable());

            //is shutdown the right thing to do - sounds kind of 'terminal'. What I mean is 'wait for all tasks'.
            threadPool.shutdown();
            threadPool.awaitTermination(1, TimeUnit.DAYS);
            testableRunnable.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedInvokeForkJoinTask extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();


        @Test
        public void testMultithreadedForkJoinPoolInvokeForkJoinTask() {
            testableForkJoinTask.testBeforeInvocation();

            threadPool.invoke(testableForkJoinTask.forkJoinTask);
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedInvokeAllCallableCollection extends ForkJoinTestBase.CallableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolInvokeAllCallableCollection() throws Exception {
            testableCallable.testBeforeInvocation();
            Callable<String> c1 = ()->"Result1";
            Callable<String> c3 = ()->"Result3";

            List<Future<String>> futures = threadPool.invokeAll(Arrays.asList(c1, testableCallable.callable, c3));
            testableCallable.testAfterConcurrentInvocation();

            Assert.assertEquals("Result1", futures.get(0).get());
            Assert.assertEquals(TestCallableFactory.NonThrowingTestableCallable.result, futures.get(1).get());
            Assert.assertEquals("Result3", futures.get(2).get());
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedSubmitCallable extends ForkJoinTestBase.CallableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolSubmitCallableAndJoin() {
            testableCallable.testBeforeInvocation();

            ForkJoinTask fjt = threadPool.submit(testableCallable.callable);
            fjt.join();
            testableCallable.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedSubmitForkJoinTask extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolSubmitForkJoinTaskAndJoin() {
            testableForkJoinTask.testBeforeInvocation();

            ForkJoinTask fjt = threadPool.submit(testableForkJoinTask.forkJoinTask);
            fjt.join();
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedSubmitRunnable extends ForkJoinTestBase.RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolSubmitRunnableAndJoin() throws Exception {
            testableRunnable.testBeforeInvocation();

            ForkJoinTask fjt = threadPool.submit(testableRunnable.getRunnable());
            fjt.join();
            testableRunnable.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedSubmitRunnableWithResult extends ForkJoinTestBase.RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolSubmitRunnableWithResultAndJoin() throws Exception {
            testableRunnable.testBeforeInvocation();

            ForkJoinTask<String> fjt = threadPool.submit(testableRunnable.getRunnable(), "Result");
            String result = fjt.join();
            testableRunnable.testAfterConcurrentInvocation();
            Assert.assertEquals("Result", result);
        }
    }

    public static class NullAdaptedRunnableTest {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        @Test
        public void testNullAdaptedRunnable() throws Exception {
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

            threadPool.submit(()->called.set(true)).get();
            Logger.installLoggerFactory(null);
            Assert.assertTrue(called.get());
            Assert.assertFalse(failed.get());
        }
    }
}
