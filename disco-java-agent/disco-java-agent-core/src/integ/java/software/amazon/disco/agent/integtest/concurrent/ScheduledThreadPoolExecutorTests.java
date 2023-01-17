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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.disco.agent.integtest.concurrent.source.TestCallableFactory;
import software.amazon.disco.agent.integtest.concurrent.source.TestRunnableFactory;
import software.amazon.disco.agent.integtest.concurrent.source.TestableConcurrencyObject;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Specific tests for the additional public methods of the ScheduledThreadPoolExecutor
 */
@RunWith(Enclosed.class)
public class ScheduledThreadPoolExecutorTests {
    /**
     * Generate a list of annotated "testable concurrency objects" for parametrizing ScheduledThreadPoolExecutor tests.
     * @return A list of 2-element Object arrays, of the form [String, TestableConcurrencyObject].
     */
    private static List<Object[]> provideAllConcurrencyObjects() {
        BiFunction<String, List<Object[]>, Stream<Object[]>> prefixFirstElement =
                (prefix, pairs) -> pairs.stream().map(e -> new Object[] {prefix + e[0], e[1]});
        return Stream.concat(
                prefixFirstElement.apply("Runnable.", TestRunnableFactory.Data.provideAllRunnables()),
                prefixFirstElement.apply("Callable.", TestCallableFactory.Data.provideAllCallables())
        ).collect(Collectors.toList());
    }

    /**
     * Base class for sub-suites of this test suite.
     */
    private static class Base extends ExecutorServiceTests.Base {
        /**
         * Shorthand for casting this test suite's ExecutorService to a ScheduledThreadPoolExecutor.
         * @return A ScheduledThreadPoolExecutor for testing.
         */
        protected ScheduledThreadPoolExecutor getExecutor() {
            return (ScheduledThreadPoolExecutor) executorService;
        }

        /**
         * Schedule a "testable concurrency object" (wrapping a Callable or Runnable) to be executed with a given delay.
         * @param testableConcurrencyObject The object to schedule
         * @param delay Delay time unit count
         * @param unit Delay time unit
         * @return Future object
         */
        protected ScheduledFuture<?> schedule(TestableConcurrencyObject testableConcurrencyObject, long delay, TimeUnit unit) {
            if (testableConcurrencyObject instanceof TestRunnableFactory.TestableRunnable) {
                return getExecutor().schedule(((TestRunnableFactory.TestableRunnable) testableConcurrencyObject).getRunnable(), delay, unit);
            } else if (testableConcurrencyObject instanceof TestCallableFactory.NonThrowingTestableCallable) {
                return getExecutor().schedule(((TestCallableFactory.NonThrowingTestableCallable) testableConcurrencyObject).callable, delay, unit);
            } else {
                throw new IllegalArgumentException("Unexpected test object type: " + testableConcurrencyObject);
            }
        }
    }

    /**
     * Test scheduling Runnables and Callables and seeing them completed.
     */
    @RunWith(Parameterized.class)
    public static class ScheduleTests extends Base {

        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestableConcurrencyObject testableConcurrencyObject;

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> data() {
            return provideAllConcurrencyObjects();
        }

        @Test
        public void testSchedule() throws Exception {
            executorService = new ScheduledThreadPoolExecutor(1);
            testBeforeInvocation(testableConcurrencyObject);
            schedule(testableConcurrencyObject, 1, TimeUnit.MILLISECONDS);
            testAfterInvocation(testableConcurrencyObject, null, null);
        }
    }

    /**
     * Test scheduling and then cancelling Runnables and Callables before they're completed, with/without removing the
     * future objects from the ScheduledThreadPoolExecutor's internal queue.
     */
    @RunWith(Parameterized.class)
    public static class CancelTests extends Base {

        @Parameterized.Parameter(0)
        public String name;

        @Parameterized.Parameter(1)
        public TestableConcurrencyObject testableConcurrencyObject;

        @Parameterized.Parameter(2)
        public Boolean removeOnCancel;

        @Parameterized.Parameters(name = "{0}; removeOnCancel={2}")
        public static Collection<Object[]> data() {
            List<Object[]> allConcurrencyObjects = provideAllConcurrencyObjects();
            Function<Object, Stream<Object[]>> appendToEachPair =
                    (object) -> allConcurrencyObjects.stream().map(e -> new Object[] {e[0], e[1], object});
            return Stream.concat(appendToEachPair.apply(Boolean.FALSE), appendToEachPair.apply(Boolean.TRUE))
                    .collect(Collectors.toList());
        }

        @Test
        public void testCancel() {
            executorService = new ScheduledThreadPoolExecutor(1);
            getExecutor().setRemoveOnCancelPolicy(removeOnCancel);
            testBeforeInvocation(testableConcurrencyObject);

            // Schedule a task so that it will never expire while a test case runs
            ScheduledFuture<?> f = schedule(testableConcurrencyObject, 1, TimeUnit.DAYS);

            // Cancellation should be indicated as successful, future has been marked as cancelled, and task queue
            // should be of size zero if we've enabled remove-on-cancellation mode earlier
            assertTrue("Future cancelled successfully", f.cancel(true));
            assertTrue("Future marked as cancelled", f.isCancelled());
            if (removeOnCancel) {
                assertEquals("Task queue is empty", 0, getExecutor().getQueue().size());
            }

            // Callable shouldn't have been called, so "before invocation" conditions should still hold
            testBeforeInvocation(testableConcurrencyObject);
        }
    }
}
