package com.amazon.disco.agent.event;

/**
 * A class of event to encapsulate the begin or end of a logical Transaction. For a request-response service, this envelopes
 * the lifetime of the service activities. For other types of service-under-test this might be a receipt of a unit of work
 * by some other non-SAAS means e.g. draining a queue of work items
 */
public abstract class AbstractTransactionEvent extends AbstractEvent implements TransactionEvent {
    /**
     * Create a new TransactionEvent
     * @param origin the origin e.g. 'Coral'
     */
    public AbstractTransactionEvent(String origin) {
        super(origin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractTransactionEvent withData(String key, Object data) {
        super.withData(key, data);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Operation getOperation();
}
