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
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;

import java.util.AbstractQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ThreadPoolExecutorTests {

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
    public void testThreadPoolExecutor() throws Exception {
        // This pool has only 1 thread and space for only 1 task on the internal queue
        // This provides reliable control over which task will get rejected, triggering the 'Caller Runs' handler
        ExecutorService e = new ThreadPoolExecutor(
                1, // 1 Thread, so we can ensure there is
                1,
                5,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy() // Root of the issue
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
            f.get(1, TimeUnit.DAYS);
        }
    }
}