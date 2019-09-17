package com.amazon.disco.agent.event;

/**
 * An abstract event that defines network-based request occurrences. In a network context, this can mean that
 * a client is requesting a stream connection with a server, or a packet was received at a certain port.
 */
public abstract class AbstractNetworkProtocolRequestEvent extends AbstractProtocolRequestEvent implements NetworkProtocolRequestEvent {
    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The Source port of the network entity. e.g. the port the client used to send the packet.
         */
        SOURCE_PORT,

        /**
         * The destination port of the network entity. e.g. the port the server is listening on.
         */
        DESTINATION_PORT,
    }

    public AbstractNetworkProtocolRequestEvent(String origin, int srcPort, int dstPort, String srcIP, String dstIP) {
        super(origin, srcIP, dstIP);
        withData(DataKey.SOURCE_PORT.name(), srcPort);
        withData(DataKey.DESTINATION_PORT.name(), dstPort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSourcePort() {
        return (int) getData(DataKey.SOURCE_PORT.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDestinationPort() {
        return (int) getData(DataKey.DESTINATION_PORT.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSourceIP() {
        return super.getSourceAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDestinationIP() {
        return super.getDestinationAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.NETWORK;
    }

}
