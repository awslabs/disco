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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import static com.amazon.disco.agent.integtest.concurrent.source.TestForkJoinTaskFactory.FORKJOINTASKS;

/**
 * A small class per 'test', so that TestNG can have a DataProvider, and a RetryAnalyzer attached, along with the
 * correct BeforeMethod and AfterMethod lifetimes.
 */
public class ForkJoinTaskTests {
    @RunWith(Parameterized.class)
    public static class SingleThreadedInvoke extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testSingleThreadedForkJoinTaskInvoke() {
            testableForkJoinTask.testBeforeInvocation();
            testableForkJoinTask.forkJoinTask.invoke();
            testableForkJoinTask.testAfterSingleThreadedInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedForkAndJoin extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultiThreadedForkJoinTaskForkAndJoin() {
            testableForkJoinTask.testBeforeInvocation();

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            testableForkJoinTask.forkJoinTask.fork();
            ForceConcurrency.after(ctx);

            testableForkJoinTask.forkJoinTask.join();
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedForkAndGet extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultiThreadedForkJoinTaskForkAndGet() throws Exception {
            testableForkJoinTask.testBeforeInvocation();

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            testableForkJoinTask.forkJoinTask.fork();
            ForceConcurrency.after(ctx);

            testableForkJoinTask.forkJoinTask.get();
            testableForkJoinTask.testAfterConcurrentInvocation();
        }

    }

    @RunWith(Parameterized.class)
    public static class MultithreadedForkAndGetWithTimeout extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultiThreadedForkJoinTaskForkAndGetWithTimeout() throws Exception {
            testableForkJoinTask.testBeforeInvocation();

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            testableForkJoinTask.forkJoinTask.fork();
            ForceConcurrency.after(ctx);

            testableForkJoinTask.forkJoinTask.get(1, TimeUnit.DAYS);
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedInvokeAllCollection extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultiThreadedForkJoinTaskInvokeAllCollection() {
            testableForkJoinTask.testBeforeInvocation();
            //create a chain of joins, to help ensure that the one of importance does not complete trivially
            ForkJoinTask fjt1 = ForkJoinTask.adapt(()->testableForkJoinTask.forkJoinTask.join());
            ForkJoinTask fjt3 = ForkJoinTask.adapt(()->fjt1.join());

            List<ForkJoinTask> list = Arrays.asList(fjt1, testableForkJoinTask.forkJoinTask, fjt3);

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinTask.invokeAll(list);
            ForceConcurrency.after(ctx);
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedInvokeAllTwoTask extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultiThreadedForkJoinTaskInvokeAllTwoTaskSpecialCase() {
            testableForkJoinTask.testBeforeInvocation();
            ForkJoinTask fjt1 = ForkJoinTask.adapt(()->testableForkJoinTask.forkJoinTask.join());

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinTask.invokeAll(fjt1, testableForkJoinTask.forkJoinTask);
            ForceConcurrency.after(ctx);
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }

    @RunWith(Parameterized.class)
    public static class MultithreadedInvokeAllVarargs extends ForkJoinTestBase.ForkJoinTaskBase {
        @Rule
        public ForceConcurrency.RetryRule retry = new ForceConcurrency.RetryRule();

        @Test
        public void testMultiThreadedForkJoinTaskInvokeAllVarargs() {
            testableForkJoinTask.testBeforeInvocation();
            //create a chain of joins, to help ensure that the one of importance does not complete trivially
            ForkJoinTask fjt1 = ForkJoinTask.adapt(()->testableForkJoinTask.forkJoinTask.join());
            ForkJoinTask fjt3 = ForkJoinTask.adapt(()->fjt1.join());

            ForceConcurrency.Context ctx = ForceConcurrency.before();
            ForkJoinTask.invokeAll(fjt1, testableForkJoinTask.forkJoinTask, fjt3);
            ForceConcurrency.after(ctx);
            testableForkJoinTask.testAfterConcurrentInvocation();
        }
    }
}
