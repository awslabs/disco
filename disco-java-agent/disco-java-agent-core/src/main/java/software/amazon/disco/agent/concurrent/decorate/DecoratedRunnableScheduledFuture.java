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

package software.amazon.disco.agent.concurrent.decorate;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Given a RunnableScheduledFuture object, decorate it with thread-info metadata to allow propagation of DiSCo
 * TransactionContext across thread hand-off in a ScheduledThreadPoolExecutor.
 */
public class DecoratedRunnableScheduledFuture<V> extends Decorated implements RunnableScheduledFuture<V> {

    final RunnableScheduledFuture<V> target;

    /**
     * Create a new DecoratedRunnableScheduledFuture. Private, use factory to create.
     *
     * @param target the RunnableScheduledFuture to decorate
     */
    private DecoratedRunnableScheduledFuture(final RunnableScheduledFuture<V> target) {
        this.target = target;
    }

    /**
     * Factory method to decorate a RunnableScheduledFuture only if it is not already a DecoratedRunnableScheduledFuture.
     *
     * @param target the RunnableScheduledFuture to consider for decoration
     * @return a DecoratedRunnableScheduledFuture representing the input RunnableScheduledFuture
     */
    public static RunnableScheduledFuture maybeCreate(RunnableScheduledFuture target) {
        if (target == null) {
            return null;
        }

        if (target instanceof DecoratedRunnableScheduledFuture) {
            return target;
        }

        return new DecoratedRunnableScheduledFuture(target);
    }

    /**
     * When the RunnableScheduledFuture is executed, perform DiSCo TransactionContext propagation, as necessary
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

    // Delegate all abstract methods

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Delayed o) {
        if (o instanceof DecoratedRunnableScheduledFuture) {
            return target.compareTo(((DecoratedRunnableScheduledFuture) o).target);
        }
        return target.compareTo(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPeriodic() {
        return this.target.isPeriodic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDone() {
        return this.target.isDone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return this.target.getDelay(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.target.cancel(mayInterruptIfRunning);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled() {
        return this.target.isCancelled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        return this.target.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.target.get(timeout, unit);
    }
}
