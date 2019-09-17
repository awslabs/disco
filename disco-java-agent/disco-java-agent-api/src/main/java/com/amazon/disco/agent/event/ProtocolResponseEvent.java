package com.amazon.disco.agent.event;

/**
 * Specialization of ProtocolEvent for protocol requests. Requests and responses are dispatched separately,
 * before and after the actual invocation of the intercepted behavior occurs. At the protocol-level,
 * the requester can be seen as the invoker, and the response is the result of the invocation.
 */
public interface ProtocolResponseEvent extends Event {
    ProtocolRequestEvent getProtocolRequestEvent();

    /**
     * Get the protocol-level response object
     * @return the request object
     */
    Object getResponse();

    /**
     * Returns the status indicator of this response event.
     * @return String representation of the status indicator
     */
    String getStatusIndicator();
}
