/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;

import java.util.AbstractQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ThreadPoolExecutorTests {
    private static final long TERMINATION_TIMEOUT = 1L;
    private static final TimeUnit TERMINATION_TIMEOUT_UNIT = TimeUnit.HOURS;
    private static final long KEEP_ALIVE = 5L;
    private static final TimeUnit KEEP_ALIVE_UNIT = TimeUnit.SECONDS;

    private static class CustomRunnable implements Runnable {
        @Override
        public void run() {
        }
    }

    @Before
    public void before() {
        TransactionContext.create();
        TransactionContext.putMetadata("foo", "bar");
    }

    @After
    public void after() {
        TransactionContext.destroy();
    }

    @Test
    public void testContextStaysAfterRejectedTaskRuns() throws Exception {
        // Having only one worker thread provides control over which task will get rejected
        ExecutorService e = new ThreadPoolExecutor(
            1, // One worker thread, to ensure at most one task runs at a time
            1,
            KEEP_ALIVE, KEEP_ALIVE_UNIT,
            new LinkedBlockingQueue<>(1),
            new ThreadPoolExecutor.CallerRunsPolicy() // Run rejected tasks in the thread that submitted it
        );
        AbstractQueue<Future> futures = new ConcurrentLinkedQueue<>();

        // This 'parent' task immediately runs on the single thread in the pool (call it thread 1)
        futures.add(e.submit(() -> {

            // This 'child' task gets added to the queue; there are no available workers so it does not start running yet
            futures.add(e.submit(() -> {
                // Do nothing
            }));

            // This 'child' task gets rejected, because there is no space in the queue and no available workers
            // The CallerRuns policy triggers, causing this child task to run on the current thread (thread 1).
            // If the TX were cleared at this point, the TX would then no longer be available for the rest of the
            // parent task.
            futures.add(e.submit(() -> {
                // Do nothing
            }));

            assertEquals("bar", TransactionContext.getMetadata("foo"));
        }));

        Future f;
        while ((f = futures.poll()) != null) {
            f.get(TERMINATION_TIMEOUT, TERMINATION_TIMEOUT_UNIT);
        }
    }

    @Ignore("The remove method of a Disco-enabled ThreadPoolExecutor fails to remove tasks")
    @Test
    public void testRemovalOfTaskBeforeItRuns() throws Exception {
        ThreadPoolExecutor e = new ThreadPoolExecutor(
            1, // One worker thread, to ensure at most one task runs at a time
            1,
            KEEP_ALIVE, KEEP_ALIVE_UNIT,
            new LinkedBlockingQueue<>(1)
        );

        // We'll use this variable to prove beyond doubt that the removed task didn't run
        AtomicBoolean trigger = new AtomicBoolean(false);
        // We'll use this latch to keep a task in mid-execution, thus holding the single worker thread occupied
        CountDownLatch occupyWorkerLatch = new CountDownLatch(1);
        Runnable triggerTask = new Runnable() {
            @Override
            public void run() {
                trigger.set(true);
            }
        };
        Runnable occupyWorkerTask = createOccupyWorkerTask(occupyWorkerLatch);

        // Occupy the worker thread so that we can remove triggerTask before it runs
        e.execute(occupyWorkerTask);
        // triggerTask won't run yet because the single worker thread is occupied
        e.execute(triggerTask);
        // Remove triggerTask before it can run; remove() should return true to indicate success
        assertEquals(true, e.remove(triggerTask));
        // Unblock the active task to allow it to complete
        occupyWorkerLatch.countDown();
        // Make sure any incomplete pending tasks get a chance to run (there shouldn't be any)
        e.shutdown();
        e.awaitTermination(TERMINATION_TIMEOUT, TERMINATION_TIMEOUT_UNIT);
        // Double-check that triggerTask didn't run
        assertEquals(false, trigger.get());
    }

    @Test
    public void testShutdownNowReturnsUndecoratedRunnable() throws Exception{
        ThreadPoolExecutor e = new ThreadPoolExecutor(
                1, // One worker thread, to ensure at most one task runs at a time
                1,
                KEEP_ALIVE, KEEP_ALIVE_UNIT,
                new LinkedBlockingQueue<>(2)
        );

        // We'll use this latch to keep a task in mid-execution, thus newly added task will be awaiting execution
        CountDownLatch occupyWorkerLatch = new CountDownLatch(1);
        Runnable occupyWorkerTask = createOccupyWorkerTask(occupyWorkerLatch);
        CustomRunnable unstartedTask1 = new CustomRunnable();
        CustomRunnable unstartedTask2 = new CustomRunnable();

        e.execute(occupyWorkerTask);
        // unstarted tasks won't run because the single worker thread is occupied
        e.execute(unstartedTask1);
        e.execute(unstartedTask2);
        // List of tasks returned by shutdownNow method should be unwrapped CustomRunnable
        e.shutdownNow().forEach(r -> Assert.assertTrue(r instanceof CustomRunnable));
        // shutdownNow will cancel occupyWorkerTask, still unblock it to be safer
        occupyWorkerLatch.countDown();
        e.awaitTermination(TERMINATION_TIMEOUT, TERMINATION_TIMEOUT_UNIT);
    }

    /**
     * Helper method to create occupy worker task.
     *
     * @param occupyWorkerLatch CountDownLatch will be used to keep a task in mid-execution.
     */
    private static Runnable createOccupyWorkerTask(CountDownLatch occupyWorkerLatch) {
        Runnable occupyWorkerTask = () -> {
            try {
                occupyWorkerLatch.await();
            }
            catch (InterruptedException exception) {
            }
        };
        return occupyWorkerTask;
    }
}