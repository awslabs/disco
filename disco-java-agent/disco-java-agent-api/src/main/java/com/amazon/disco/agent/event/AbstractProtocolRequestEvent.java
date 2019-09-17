package com.amazon.disco.agent.event;

/**
 * This event stores information about the invoker of the protocol event.
 */
public abstract class AbstractProtocolRequestEvent extends AbstractProtocolEvent implements ProtocolRequestEvent {

    public AbstractProtocolRequestEvent(String origin, String srcAddr, String dstAddr) {
        super(origin);
        withData(DataKey.SRC_ADDRESS.name(), srcAddr);
        withData(DataKey.DST_ADDRESS.name(), dstAddr);
    }

    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The literal underlying protocol request object.
         */
        REQUEST,

        /**
         * The source address for this protocol.
         */
        SRC_ADDRESS,

        /**
         * The destination address for this protocol.
         */
        DST_ADDRESS
    }

    /**
     * The literal request object stored in the protocol event.
     *
     * @param request The request object associated with this protocol.
     * @return The request object.
     */
    public AbstractProtocolRequestEvent withRequest(Object request) {
        withData(DataKey.REQUEST.name(), request);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getRequest() {
        return getData(DataKey.REQUEST.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSourceAddress() {
        return (String) getData(DataKey.SRC_ADDRESS.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDestinationAddress() {
        return (String) getData(DataKey.DST_ADDRESS.name());
    }

}
