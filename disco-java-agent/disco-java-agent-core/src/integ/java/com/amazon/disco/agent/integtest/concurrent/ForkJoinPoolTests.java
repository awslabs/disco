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
import com.amazon.disco.agent.integtest.concurrent.source.ForkJoinTestBase;
import com.amazon.disco.agent.integtest.concurrent.source.TestCallableFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ForkJoinPoolTests {
    @RunWith(Parameterized.class)
    public static class MultithreadedExecuteForkJoinTask extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolExecuteForkJoinTaskAndJoin() {
            testableForkJoinTask.testBeforeInvocation();

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinPool.commonPool().execute(testableForkJoinTask.forkJoinTask);
            testableForkJoinTask.forkJoinTask.join();
            ForceConcurrency.after(ctx);
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }


    @RunWith(Parameterized.class)
    public static class MultithreadedExecuteRunnable extends ForkJoinTestBase.RunnableBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultithreadedForkJoinPoolExecuteRunnableAndAwait() throws Exception {
            testableRunnable.testBeforeInvocation();

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinPool.commonPool().execute(testableRunnable.getRunnable());
            ForceConcurrency.after(ctx);

            //is shutdown the right thing to do - sounds kind of 'terminal'. What I mean is 'wait for all tasks'.
            ForkJoinPool.commonPool().shutdown();
            ForkJoinPool.commonPool().awaitTermination(1, TimeUnit.DAYS);
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

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinPool.commonPool().invoke(testableForkJoinTask.forkJoinTask);
            ForceConcurrency.after(ctx);
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

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            List<Future<String>> futures = ForkJoinPool.commonPool().invokeAll(Arrays.asList(c1, testableCallable.callable, c3));
            ForceConcurrency.after(ctx);
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

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinTask fjt = ForkJoinPool.commonPool().submit(testableCallable.callable);
            fjt.join();
            ForceConcurrency.after(ctx);
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

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinTask fjt = ForkJoinPool.commonPool().submit(testableForkJoinTask.forkJoinTask);
            fjt.join();
            ForceConcurrency.after(ctx);
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

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinTask fjt = ForkJoinPool.commonPool().submit(testableRunnable.getRunnable());
            fjt.join();
            ForceConcurrency.after(ctx);
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

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinTask<String> fjt = ForkJoinPool.commonPool().submit(testableRunnable.getRunnable(), "Result");
            String result = fjt.join();
            ForceConcurrency.after(ctx);
            testableRunnable.testAfterConcurrentInvocation();
            Assert.assertEquals("Result", result);
        }
    }
}
