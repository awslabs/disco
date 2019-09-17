package com.amazon.disco.agent.event;

/**
 * Concrete TransactionEvent to denote a transaction logically beginning
 */
public class TransactionBeginEvent extends AbstractTransactionEvent {

    /**
     * Create a TransactionBeginEvent
     * @param origin the origin of the event e.g. Coral
     */
    public TransactionBeginEvent(String origin) {
        super(origin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation getOperation() {
        return Operation.BEGIN;
    }
}
