package com.amazon.disco.agent.event;

/**
 * An event issued to the event bus on Thread entry/exit
 */
public interface ThreadEvent extends Event {
    /**
     * The specific type of ThreadEvent
     */
    enum Operation {
        /**
         * This operation is published when a new worker thread is first entered
         */
        ENTERING,

        /**
         * This operation is published when a worker thread has finished working, right before the Runnable/Callable/etc completes
         */
        EXITING
    }

    /**
     * Get the parent threadId
     * @return the parent threadId
     */
    long getParentId();

    /**
     * Get the child threadId
     * @return the child threadId
     */
    long getChildId();

    /**
     * Get the particular type of ThreadEvent ENTERING or EXITING
     * @return the specific thread Operation
     */
    Operation getOperation();
}
