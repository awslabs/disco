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

package com.amazon.disco.agent.integtest.concurrent.source;

import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class TestForkJoinTaskFactory {
    static int numFactoryMethods;

    static {
        numFactoryMethods = 0;
        for (Method method: TestRunnableFactory.class.getDeclaredMethods()) {
            if (method.getReturnType().equals(TestRunnableFactory.TestableRunnable.class) && method.getName().startsWith("create")) {
                numFactoryMethods++;
            }
        }
        //we use the set of runnables twice for two types of adaptation - with and without result
        numFactoryMethods = numFactoryMethods * 2;

        for (Method method: TestCallableFactory.class.getDeclaredMethods()) {
            if (method.getReturnType().equals(TestCallableFactory.NonThrowingTestableCallable.class) && method.getName().startsWith("create")) {
                numFactoryMethods++;
            }
        }

        for (Method method: TestForkJoinTaskFactory.class.getDeclaredMethods()) {
            if (method.getReturnType().equals(NonThrowingTestableForkJoinTask.class) && method.getName().startsWith("create")) {
                numFactoryMethods++;
            }
        }

        Assert.assertEquals("Incorrect number of factory methods found in static initializer", Data.provideAllForkJoinTasks().size(), numFactoryMethods);
    }

    public static class NonThrowingTestableForkJoinTask extends TestableConcurrencyObjectImpl {
        public ForkJoinTask forkJoinTask;
        private TestableConcurrencyObject innerTestable;

        NonThrowingTestableForkJoinTask() {
            forkJoinTask = null;
            innerTestable = null;
        }

        NonThrowingTestableForkJoinTask(ForkJoinTask forkJoinTask, TestableConcurrencyObject innerTestable) {
            this.forkJoinTask = forkJoinTask;
            this.innerTestable = innerTestable;
        }

        NonThrowingTestableForkJoinTask(ForkJoinTask forkJoinTask) {
            this.forkJoinTask = forkJoinTask;
            this.innerTestable = null;
        }

        void setForkJoinTask(ForkJoinTask forkJoinTask) {
            this.forkJoinTask = forkJoinTask;
        }

        @Override
        public void reset() {
            super.reset();
            if (innerTestable != null) {
                innerTestable.reset();
            }
            if (forkJoinTask != null) {
                forkJoinTask.reinitialize();
            }
        }

        @Override
        public void perform() {
            try {
                if (innerTestable != null) {
                    innerTestable.perform();
                } else {
                    super.perform();
                }
            } catch (Throwable t) {
                thrown = t;
            }
        }

        @Override
        public void testBeforeInvocation() {
            if (innerTestable == null) {
                super.testBeforeInvocation();
            } else {
                innerTestable.testBeforeInvocation();
            }
        }

        @Override
        public void testAfterConcurrentInvocation() {
            if (innerTestable == null) {
                super.testAfterConcurrentInvocation();
            } else {
                innerTestable.testAfterConcurrentInvocation();
            }
        }

        @Override
        public void testAfterSingleThreadedInvocation() {
            if (innerTestable == null) {
                super.testAfterSingleThreadedInvocation();
            } else {
                innerTestable.testAfterSingleThreadedInvocation();
            }
        }
    }

    //TODO prepended with underscores until ready for use, and flesh out implementations in next CR
    public static NonThrowingTestableForkJoinTask _createConcreteRecursiveTask() {
        return null;
    }

    public static NonThrowingTestableForkJoinTask _createAnonymousRecursiveTask() {
        NonThrowingTestableForkJoinTask testableForkJoinTask = new NonThrowingTestableForkJoinTask();
        ForkJoinTask forkJoinTask = new RecursiveTask<String>() {
            @Override
            protected String compute() {
                testableForkJoinTask.perform();
                return "Result";
            }
        };
        testableForkJoinTask.setForkJoinTask(forkJoinTask);
        return testableForkJoinTask;
    }

    public static NonThrowingTestableForkJoinTask _createConcreteRecursiveAction() {
        return null;
    }

    public static NonThrowingTestableForkJoinTask _createConcreteCountedCompleter() {
        return null;
    }

    public static NonThrowingTestableForkJoinTask _createConcreteCustomForkJoinTaskSubclass() {
        return null;
    }

    //todo, various non-concrete forms such as anonymous and any inheritence patterns which make sense
    // - ForkJoinTask isn't an interface, and doesn't implement one, so no lambda cases here afaik

    public static class Data {
        public static Collection<Object[]> provideAllForkJoinTasks() {
            List<Object[]> runnableData = TestRunnableFactory.Data.provideAllRunnables();
            List<Object[]> runnableDataWithResult = TestRunnableFactory.Data.provideAllRunnables();
            List<Object[]> callableData = TestCallableFactory.Data.provideAllCallables();

            List<Object[]> result = new ArrayList<>(numFactoryMethods);
            int i = 0;

            int j = 0;
            for (Object[] tuple: runnableData) {
                TestRunnableFactory.TestableRunnable runnable = TestRunnableFactory.TestableRunnable.class.cast(tuple[1]);
                result.add(i++, new Object[] {"ForkJoinTaskAdaptedFromRunnable-"+runnableData.get(j++)[0], new NonThrowingTestableForkJoinTask(ForkJoinTask.adapt(runnable.getRunnable()), runnable)});
            }

            j = 0;
            for (Object[] tuple: runnableDataWithResult) {
                TestRunnableFactory.TestableRunnable runnable = TestRunnableFactory.TestableRunnable.class.cast(tuple[1]);
                result.add(i++, new Object[] {"ForkJoinTaskAdaptedFromRunnableWithResult-"+runnableData.get(j++)[0], new NonThrowingTestableForkJoinTask(ForkJoinTask.adapt(runnable.getRunnable()), runnable)});
            }

            j = 0;
            for (Object[] tuple: callableData) {
                TestCallableFactory.NonThrowingTestableCallable callable = TestCallableFactory.NonThrowingTestableCallable.class.cast(tuple[1]);
                result.add(i++, new Object[] {"ForkJoinTaskAdaptedFromCallable-"+runnableData.get(j++)[0], new NonThrowingTestableForkJoinTask(ForkJoinTask.adapt(callable.callable), callable)});
            }

            //result[i++] = new Object[] {"AnonymousRecursiveTask", createAnonymousRecursiveTask()};

            return result;
        }
    }
}
