package com.amazon.disco.agent.event;

/**
 * Concrete ThreadEvent for when a worker thread is finished, and the inner FunctionalInterface is about to return
 */
public class ThreadExitEvent extends AbstractThreadEvent {
    /**
     * Construct a new ThreadExitEvent
     * @param origin origin of the event, presumably 'Concurrency'
     * @param parentId the threadId of the parent thread
     * @param childId the threadId of the worker thread
     */
    public ThreadExitEvent(String origin, Long parentId, Long childId) {
        super(origin, parentId, childId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation getOperation() {
        return Operation.EXITING;
    }
}
