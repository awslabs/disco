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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class to 'persuade' the Java runtime to create new pooled threads.
 * A lock is held at the time of object creation. A method which attempts to take this lock is dispatched to a thread.
 * The calling thread unlocks the lock, once the object-under-test has been forked to a thread pool.
 *
 * This 'forces' the two tasks to be parallelized. It's likely to be a brittle or unreliable mechanism. I don't know
 * any particularly better ways.
 */
public class ForceConcurrency {
    public static class Context {
        ReentrantLock lock = new ReentrantLock();
        public ForkJoinTask fjt;
    }

    public static Context before() {
        Context ctx = createContext();
        ctx.fjt.fork();
        return ctx;
    }

    public static void after(Context ctx) {
        ctx.lock.unlock();
        ctx.fjt.join();
    }

    public static Context createContext() {
        Context ctx = new Context();
        ctx.lock.lock();

        Runnable r = ()-> {
            ctx.lock.lock();
            ctx.lock.unlock();
        };
        ForkJoinTask fjt = ForkJoinTask.adapt(r);
        ctx.fjt = fjt;

        return ctx;
    }

    public static class RetryRule implements TestRule {
        @Override
        public Statement apply(Statement base, Description target) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    //seems like a high number of retries, but still fails occasionally, and requires the test suite to be rerun
                    boolean success = false;
                    for (int i = 0; i < 31; i++) {
                        try {
                            base.evaluate();
                            success = true;
                        } catch (ConcurrencyCanBeRetriedException t) {
                            System.out.println("DiSCo(Core-integtests): Retrying test in case of concurrency flakiness, retry "+i);
                        } catch (Throwable t) {
                            throw t;
                        }

                        if (success) {
                            return;
                        }
                    }

                    //last chance
                    base.evaluate();
                }
            };
        }
    }
}
