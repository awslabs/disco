package com.amazon.disco.agent.event;

/**
 * Concrete TransactionEvent to denote a transaction logically ending
 */
public class TransactionEndEvent extends AbstractTransactionEvent {

    /**
     * Create a TransactionEndEvent
     * @param origin the origin of the event e.g. Coral
     */
    public TransactionEndEvent(String origin) {
        super(origin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation getOperation() {
        return Operation.END;
    }
}
