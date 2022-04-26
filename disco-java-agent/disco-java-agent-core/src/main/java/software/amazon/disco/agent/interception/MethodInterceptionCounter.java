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

package software.amazon.disco.agent.interception;

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to conveniently track method interception states.
 * This can help to avoid interceptor doing business logic multiple times in some method chaining
 */
public class MethodInterceptionCounter {
    private static final Logger log = LogManager.getLogger(MethodInterceptionCounter.class);
    final ThreadLocal<AtomicInteger> localReferenceCounter;

    /**
     * Construct a new MethodInterceptionCounter.
     */
    public MethodInterceptionCounter() {
        localReferenceCounter = ThreadLocal.withInitial(() -> new AtomicInteger(0));
    }

    /**
     * Increase the counter by 1.
     * @return the new value
     */
    public int increment() {
        return localReferenceCounter.get().incrementAndGet();
    }

    /**
     * Decrease the counter by 1.
     * @return the new value. May be less than zero, indicating a counting error, but the method will internally correct
     * the real value to zero in this case
     */
    public int decrement() {
        int ret = localReferenceCounter.get().decrementAndGet();
        if (ret < 0) {
            if (LogManager.isDebugEnabled()) {
                log.debug("Disco(Core) method interception counter dropped below zero, indicating increment/decrement mismatch", new RuntimeException());
            }
            localReferenceCounter.get().set(0);
        }
        return ret;
    }

    /**
     * @return true if current method has intercepted already.
     */
    public boolean hasIntercepted() {
        return localReferenceCounter.get().get() > 0;
    }
}
