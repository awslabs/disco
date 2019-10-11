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

import com.amazon.disco.agent.integtest.concurrent.source.TestableConcurrencyObjectImpl;
import com.amazon.disco.agent.integtest.concurrent.source.TestRunnableFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

/**
 * Test the propagation of TransactionContext when manually creating new Thread objects
 */
public class ThreadCreationTests {

    @RunWith(Parameterized.class)
    public static class RunnableThreadStartTest {
        private TestRunnableFactory.TestableRunnable testableRunnable;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestRunnableFactory.Data.provideAllRunnables();}
        public RunnableThreadStartTest(String name, TestRunnableFactory.TestableRunnable testableRunnable) {
            this.testableRunnable = testableRunnable;
        }

        @Before
        public void before() {
            TestableConcurrencyObjectImpl.before();
        }

        @After
        public void after() {
            TestableConcurrencyObjectImpl.after();
        }

        @Test
        public void testRunnableThreadStart() throws Exception {
            start(testableRunnable);
        }
    }

    @RunWith(Parameterized.class)
    public static class RunnableThreadRunTest {
        private TestRunnableFactory.TestableRunnable testableRunnable;

        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {return TestRunnableFactory.Data.provideAllRunnables();}
        public RunnableThreadRunTest(String name, TestRunnableFactory.TestableRunnable testableRunnable) {
            this.testableRunnable = testableRunnable;
        }

        @Before
        public void before() {
            TestableConcurrencyObjectImpl.before();
        }

        @After
        public void after() {
            TestableConcurrencyObjectImpl.after();
        }

        @Test
        public void testRunnableThreadStart() throws Exception {
            run(testableRunnable);
        }
    }

    private static void run(TestRunnableFactory.TestableRunnable runnable) {
        runnable.testBeforeInvocation();
        Thread thread = new Thread(runnable.getRunnable());
        thread.run();
        runnable.testAfterSingleThreadedInvocation();
    }

    private static void start(TestRunnableFactory.TestableRunnable runnable) throws Exception {
        runnable.testBeforeInvocation();
        Thread thread = new Thread(runnable.getRunnable());
        thread.start();
        thread.join();
        runnable.testAfterConcurrentInvocation();
    }
}
