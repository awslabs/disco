package com.amazon.disco.agent.event;

/**
 * Specialization of NetworkProtocolEvent for network requests. Requests and responses are dispatched separately,
 * before and after the actual invocation of the intercepted behavior occurs. At the network-level,
 * the requester can be seen as the requester to a network server, while the response is the
 * processed result of the request.
 */
public interface NetworkProtocolResponseEvent extends NetworkProtocolEvent, ProtocolResponseEvent {
    /**
     * Get the prior network request event
     * @return The network request event.
     */
    NetworkProtocolRequestEvent getNetworkRequestEvent();
}
