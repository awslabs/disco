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

package software.amazon.disco.agent.integtest.concurrent.source;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents user scenarios where they already employ a decoration in the execute() method of a custom Executor.
 * We want to ensure that if the user has any instanceof checks against *their* type of Decorated runnable, that the disco
 * runnable is 'underneath' it such that their checks do not suddenly fail.
 */
public class UserDecoratedExecutorFactory implements ExecutorServiceFactory {

    @Override
    public ExecutorService createExecutorService() {
        return new UserDecoratedExecutor();
    }

    public static class UserDecoratedExecutor extends ThreadPoolExecutor {
        private final RuntimeException exception;

        public UserDecoratedExecutor(RuntimeException toThrow) {
            super(2, 2, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
            exception = toThrow;
        }

        public UserDecoratedExecutor() {
            this(null);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (!(r instanceof UserDecoratedRunnable)) {
                //customer code expects it to be one of these, not the disco DecoratedRunnable which should be 'beneath' their abstraction, not above it.
                throw new IllegalStateException();
            }
            super.beforeExecute(t, r);
        }

        @Override
        public void execute(Runnable command) {
            super.execute(new UserDecoratedRunnable(command));
            if (exception != null) {
                throw exception;
            }
        }

        static class UserDecoratedRunnable implements Runnable {
            Runnable target;
            UserDecoratedRunnable(Runnable target) {
                this.target = target;
            }

            @Override
            public void run() {
                target.run();
            }
        }
    }
}
