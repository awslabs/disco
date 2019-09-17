package com.amazon.disco.agent.concurrent.decorate;

import java.util.concurrent.Callable;

/**
 * Given a Callable object used for thread hand-off, decorate it with thread-info metadata, to allow propagation
 * of AlphaOne TransactionContext.
 */
public class DecoratedCallable extends Decorated implements Callable {
    Callable target;

    /**
     * Construct a DecoratedCallable from the given target
     * @param target the Callable to decorate
     */
    DecoratedCallable(Callable target) {
        super();
        this.target = target;
    }

    /**
     * Factory method to decorate a Runnable only if it is not already a DecoratedRunnable
     * @param target the Runnable to consider for decoration
     * @return a DecoratedRunnable representing the input Runnable
     */
    public static Callable maybeCreate(Callable target) {
        if (target == null) {
            return null;
        }

        if (target instanceof DecoratedCallable) {
            return target;
        }

        return new DecoratedCallable(target);
    }

    /**
     * When the DecoratedCallable is executed, perform AlphaOne TransactionContext propagation, as necessary
     * {@inheritDoc}
     */
    @Override
    public Object call() throws Exception {
        before();
        try {
            return target.call();
        } catch (Throwable t) {
            throw t;
        }finally {
            after();
        }
    }
}
