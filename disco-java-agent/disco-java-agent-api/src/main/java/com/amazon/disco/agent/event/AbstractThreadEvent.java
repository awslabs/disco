package com.amazon.disco.agent.event;

/**
 * Abstract Event to encapsulate information when execution crosses a thread boundary, within an AlphaOne transaction
 */
public abstract class AbstractThreadEvent extends AbstractEvent implements ThreadEvent {
    /**
     * The data items contained in thread events
     */
    enum DataKey {
        /**
         * The threadId of the parent thread
         */
        PARENT_ID,

        /**
         * The threadId of the child thread
         */
        CHILD_ID
    }

    /**
     * Create a new ThreadEvent
     * @param origin the origin of the Event, presumably 'Concurrency'
     * @param parentId threadId of parent thread
     * @param childId threadId of worker thread
     */
    public AbstractThreadEvent(String origin, Long parentId, Long childId) {
        super(origin);
        withData(DataKey.PARENT_ID.name(), parentId);
        withData(DataKey.CHILD_ID.name(), childId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractThreadEvent withData(String key, Object data) {
        super.withData(key, data);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getParentId() {
        return Long.class.cast(getData(DataKey.PARENT_ID.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getChildId() {
        return Long.class.cast(getData(DataKey.CHILD_ID.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Operation getOperation();
}
