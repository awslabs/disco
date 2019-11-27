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

package software.amazon.disco.agent.concurrent.decorate;

/**
 * Given a Runnable object used for thread hand-off, decorate it with thread-info metadata, to allow propagation
 * of DiSCo TransactionContext.
 */
public class DecoratedRunnable extends Decorated implements Runnable {
    Runnable target;

    /**
     * Create a new DecoratedRunnable. Package-private, to enforce use of factory methods.
     * @param target the Runnable to decorate
     */
    DecoratedRunnable(Runnable target) {
        super();
        this.target = target;
    }

    /**
     * Factory method to decorate a Runnable only if it is not already a DecoratedRunnable
     * @param target the Runnable to consider for decoration
     * @return a DecoratedRunnable representing the input Runnable
     */
    public static Runnable maybeCreate(Runnable target) {
        if (target == null) {
            return null;
        }

        if (target instanceof DecoratedRunnable) {
            return target;
        }

        return new DecoratedRunnable(target);
    }

    /**
     * When the DecoratedRunnable is executed, perform DiSCo TransactionContext propagation, as necessary
     *
     * {@inheritDoc}
     */
    @Override
    public void run() {
        before();
        try {
            target.run();
        } catch (Throwable t) {
            throw t;
        } finally {
            after();
        }
    }
}



