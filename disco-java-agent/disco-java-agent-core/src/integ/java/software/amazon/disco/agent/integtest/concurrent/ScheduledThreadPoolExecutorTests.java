/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.disco.agent.integtest.concurrent.source.TestCallableFactory;
import software.amazon.disco.agent.integtest.concurrent.source.TestRunnableFactory;

import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// Specific tests for the additional public methods of the ScheduledThreadPoolExecutor
public class ScheduledThreadPoolExecutorTests {
    @RunWith(Parameterized.class)
    public static class ScheduleRunnable extends ExecutorServiceTests.Base {

        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestRunnableFactory.TestableRunnable testableRunnable;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestRunnableFactory.Data.provideAllRunnables();}

        @Test
        public void testScheduleRunnable() throws Exception {
            executorService = new ScheduledThreadPoolExecutor(2);
            testBeforeInvocation(testableRunnable);
            ((ScheduledThreadPoolExecutor) executorService).schedule(testableRunnable.getRunnable(), 1, TimeUnit.MILLISECONDS);
            testAfterInvocation(testableRunnable, null, null);
        }
    }

    @RunWith(Parameterized.class)
    public static class ScheduleCallable extends ExecutorServiceTests.Base {

        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestCallableFactory.NonThrowingTestableCallable testableCallable;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestCallableFactory.Data.provideAllCallables();}

        @Test
        public void testScheduleCallable() throws Exception {
            executorService = new ScheduledThreadPoolExecutor(2);
            testBeforeInvocation(testableCallable);
            ((ScheduledThreadPoolExecutor) executorService).schedule(testableCallable.callable, 1, TimeUnit.MILLISECONDS);
            testAfterInvocation(testableCallable, null, null);
        }
    }
}
