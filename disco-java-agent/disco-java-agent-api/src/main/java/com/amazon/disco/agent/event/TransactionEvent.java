package com.amazon.disco.agent.event;

/**
 * An event issued to the event bus at the beginning and end of logical 'transactions'
 */
public interface TransactionEvent extends Event {
    /**
     * The kind of specific operation of this transaction event
     */
    enum Operation {
        /**
         * When the transaction logically begins
         */
        BEGIN,

        /**
         * When the transaction logically ends
         */
        END
    }

    /**
     * Get the type of TransactionEvent, BEGIN or END
     * @return the type of this TransactionEvent
     */
    Operation getOperation();
}
