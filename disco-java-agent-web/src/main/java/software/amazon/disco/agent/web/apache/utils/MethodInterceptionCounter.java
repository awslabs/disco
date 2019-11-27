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

package software.amazon.disco.agent.web.apache.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to conveniently track method interception states.
 * This can help to avoid interceptor doing business logic multiple times in some method chaining
 */
public class MethodInterceptionCounter {
    private final ThreadLocal<AtomicInteger> localReferenceCounter;

    /**
     * Construct a new MethodInterceptionCounter.
     */
    public MethodInterceptionCounter() {
        localReferenceCounter = ThreadLocal.withInitial(() -> new AtomicInteger(0));
    }

    /**
     * Increase the counter by 1.
     */
    public void increment() {
        if (getReferenceCounter() == null) {
            clear();
        }
        getReferenceCounter().getAndIncrement();
    }

    /**
     * Decrease the counter by 1.
     */
    public void decrement() {
        if (getReferenceCounter() == null) {
            clear();
        } else {
            if (getReferenceCounter().decrementAndGet() <= 0) {
                clear();
            }
        }
    }

    /**
     * @return true if current method has intercepted already.
     */
    public boolean hasIntercepted() {
        if (getReferenceCounter() == null) {
            clear();
            return false;
        }
        return getReferenceCounter().get() > 0;
    }

    /**
     * @return The counter value, which suggests how many times this method has intercepted.
     */
    AtomicInteger getReferenceCounter() {
        return localReferenceCounter.get();
    }

    /**
     * Clears the value of the counter, and restores it to its initial value.
     */
    private void clear() {
        localReferenceCounter.remove();
    }
}
