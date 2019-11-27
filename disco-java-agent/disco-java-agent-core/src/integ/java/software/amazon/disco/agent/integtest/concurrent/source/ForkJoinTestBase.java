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

package software.amazon.disco.agent.integtest.concurrent.source;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;

import java.util.Collection;

public class ForkJoinTestBase {
    public static abstract class Base {
        @Before
        public void beforeBase() {
            TestableConcurrencyObjectImpl.before();
        }

        @After
        public void afterBase() {
            TestableConcurrencyObjectImpl.after();
        }
    }

    public static abstract class RunnableBase extends Base {
        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestRunnableFactory.Data.provideAllRunnables();}

        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestRunnableFactory.TestableRunnable testableRunnable;

        @Before
        public void beforeRunnableBase() {
            testableRunnable.reset();
        }
    }

    public static abstract class CallableBase extends Base {
        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestCallableFactory.Data.provideAllCallables();}

        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestCallableFactory.NonThrowingTestableCallable testableCallable;

        @Before
        public void beforeCallableBase() {
            testableCallable.reset();
        }
    }

    public static abstract class ForkJoinTaskBase extends Base {
        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestForkJoinTaskFactory.Data.provideAllForkJoinTasks();}

        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestForkJoinTaskFactory.NonThrowingTestableForkJoinTask testableForkJoinTask;

        @Before
        public void beforeForkJoinTaskBase() {
            testableForkJoinTask.reset();
        }
    }
}
