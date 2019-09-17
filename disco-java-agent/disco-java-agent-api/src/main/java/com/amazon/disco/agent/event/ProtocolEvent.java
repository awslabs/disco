package com.amazon.disco.agent.event;

/**
 * An event issued to the event bus on a protocol-level call. Protocol isn't defined here
 * in the general sense of the OSI layer. It can be described in terms of such events like
 * Command Line invocations, File System events, and network events.
 */
public interface ProtocolEvent extends Event {
    /**
     * The type of this ProtocolEvent
     */
    enum Type {
        /**
         * If the event is 'I'm sending/receiving some data through the network layer'
         */
        NETWORK
    }

    /**
     * Get the value of header information tied to this event.
     * @return the header value associated with the key.
     */
    String getHeaderData(String key);

    /**
     * Get the type of this ProtocolRequestEvent
     * @return NETWORK for now
     */
    Type getType();
}