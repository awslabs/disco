package com.amazon.disco.agent.concurrent.decorate;

import com.amazon.disco.agent.concurrent.ConcurrentUtils;
import com.amazon.disco.agent.concurrent.TransactionContext;

import java.util.concurrent.ConcurrentMap;

/**
 * To propagate context, we decorate or otherwise adorn classes such as Runnable, Callable and ForkJoinTask
 * with extra metadata regarding thread provenance. This metadata is encapsulated in this abstraction.
 */
public abstract class Decorated {
    protected Long parentThreadId;
    ConcurrentMap parentTransactionContext;

    /**
     * Construct a new object to hold thread provenance information.
     */
    protected Decorated() {
        this.parentThreadId = Thread.currentThread().getId();
        this.parentTransactionContext = TransactionContext.getPrivateMetadata();
    }

    /**
     * Convenience method to call before the execution of the dispatched object method eg. run() or call()
     */
    public void before() {
        ConcurrentUtils.set(parentThreadId, parentTransactionContext);
    }

    /**
     * Convenience method to call after the execution of the dispatched object method eg. run() or call()
     */
    public void after() {
        ConcurrentUtils.clear(parentThreadId, parentTransactionContext);
    }
}
