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

import software.amazon.disco.agent.concurrent.preprocess.DiscoRunnableDecorator;

import java.util.function.BiFunction;

/**
 * Given a Runnable object used for thread hand-off, decorate it with thread-info metadata, to allow propagation
 * of DiSCo TransactionContext.
 */
public class DecoratedRunnable extends Decorated implements Runnable {
    Runnable target;

    /**
     * Create a new DecoratedRunnable. Package-private, to enforce use of factory methods.
     *
     * @param target the Runnable to decorate
     */
    DecoratedRunnable(Runnable target) {
        super();
        this.target = target;
    }

    /**
     * Factory method to decorate a Runnable only if it is not already a DecoratedRunnable
     *
     * @param target the Runnable to consider for decoration
     * @return a DecoratedRunnable representing the input Runnable
     */
    public static DecoratedRunnable maybeCreate(Runnable target) {
        if (target == null) {
            return null;
        }

        if (target instanceof DecoratedRunnable) {
            return (DecoratedRunnable) target;
        }

        return new DecoratedRunnable(target);
    }

    /**
     * Factory method to decorate a Runnable only if it is not already a DecoratedRunnable
     *
     * @param target the Runnable to consider for decoration
     * @param removeTX whether to remove the transaction context of the decorated Runnable or not
     * @return a DecoratedRunnable representing the input Runnable
     */
    public static DecoratedRunnable maybeCreate(Runnable target, boolean removeTX) {
        DecoratedRunnable decoratedRunnable = maybeCreate(target);

        if (decoratedRunnable != null && removeTX) {
            decoratedRunnable.removeTransactionContext(true);
        }
        return decoratedRunnable;
    }

    /**
     * Get the underlying runnable that is the target of decoration.
     *
     * @return the target runnable.
     */
    public Runnable getTarget() {
        return target;
    }

    /**
     * Compare equal to another DecoratedRunnable if the target compares equal.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        return (other instanceof DecoratedRunnable) &&
                (target.equals(((DecoratedRunnable) other).target));
    }

    /**
     * A hash code to match the equals() implementation.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return target.hashCode();
    }

    /**
     * When the DecoratedRunnable is executed, perform DiSCo TransactionContext propagation, as necessary
     * <p>
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

    /**
     * A function to be passed to {@link DiscoRunnableDecorator} in order to be applied to decorate Runnables. This
     * function simply invokes the static {@link DecoratedRunnable#maybeCreate(Runnable)}.
     */
    public static class RunnableDecorateFunction implements BiFunction<Runnable, Boolean, Runnable> {
        @Override
        public Runnable apply(Runnable target, Boolean removeTX) {
            return maybeCreate(target, removeTX);
        }
    }
}



