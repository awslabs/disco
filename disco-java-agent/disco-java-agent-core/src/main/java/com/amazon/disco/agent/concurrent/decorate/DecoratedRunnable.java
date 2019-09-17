package com.amazon.disco.agent.concurrent.decorate;

/**
 * Given a Runnable object used for thread hand-off, decorate it with thread-info metadata, to allow propagation
 * of AlphaOne TransactionContext.
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
     * When the DecoratedRunnable is executed, perform AlphaOne TransactionContext propagation, as necessary
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



