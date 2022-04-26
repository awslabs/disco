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
package software.amazon.disco.agent.integtest.concurrent.regressiontests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;

import java.util.concurrent.*;

import static junit.framework.TestCase.assertEquals;

/**
 * This regression tests covers a previous issue where work submitted to an Executor is not decorated if this occurs inside
 * another runnable being executed on the same thread that it was submitted from.
 */
public class ReentrantTests {

    /**
     * Run in the current thread.
     */
    public static class DirectExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private final Executor directExecutor = new DirectExecutor();
    private final ExecutorService executorService = new ThreadPoolExecutor(1, 5, 1, TimeUnit.DAYS, new LinkedBlockingQueue<>(10));

    @Before
    public void before() {

        // Submit work to start the core threads (set to 1) in the ThreadPoolExecutor. This ensures that the actual test
        // is following the thread reuse case inside ThreadPoolExecutor
        executorService.submit(() -> {
        });

        TransactionContext.create();
        TransactionContext.putMetadata("foo", "bar");
    }

    @After
    public void after() {
        TransactionContext.destroy();
    }

    @Test
    public void test() {
        directExecutor.execute(() -> {
            try {
                executorService.submit(() -> {
                    assertEquals("bar", TransactionContext.getMetadata("foo"));
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}