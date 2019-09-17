package com.amazon.disco.agent.event;

/**
 * Concrete ThreadEvent for when a worker thread is entered
 */
public class ThreadEnterEvent extends AbstractThreadEvent {
    /**
     * Construct a new ThreadEnterEvent
     * @param origin origin of the event, presumably 'Concurrency'
     * @param parentId the threadId of the parent thread
     * @param childId the threadId of the worker thread
     */
    public ThreadEnterEvent(String origin, Long parentId, Long childId) {
        super(origin, parentId, childId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation getOperation() {
        return Operation.ENTERING;
    }
}
