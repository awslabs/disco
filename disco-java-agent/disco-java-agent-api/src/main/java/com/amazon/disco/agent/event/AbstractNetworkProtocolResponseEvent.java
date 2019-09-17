package com.amazon.disco.agent.event;

/**
 * An abstract event that defines network-based response occurrences. In a network context, this can mean that
 * a client request has been served.
 */
public abstract class AbstractNetworkProtocolResponseEvent extends AbstractProtocolResponseEvent implements NetworkProtocolResponseEvent {

    public AbstractNetworkProtocolResponseEvent(String origin, NetworkProtocolRequestEvent requestEvent) {
        super(origin, requestEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkProtocolRequestEvent getNetworkRequestEvent() {
        return (NetworkProtocolRequestEvent) super.getProtocolRequestEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.NETWORK;
    }
}
