package com.amazon.disco.agent.event;

/**
 * An event issued to the event bus on a network-level call. The network event here
 * is driven by anything that may communicate through sockets using a transport-layer protocol.
 */
public interface NetworkProtocolEvent extends ProtocolEvent {
    /**
     * The underlying network protocol used
     */
    enum NetworkType {
        /**
         * The layer being observed uses TCP as its transport protocol
         */
        TCP,

        /**
         * The layer being observed uses UDP as its transport protocol
         */
        UDP
    }

    /**
     * Get the network type of this ProtocolNetworkEvent
     * @return return either TCP or UDP.
     */
    NetworkType getNetworkType();
}
