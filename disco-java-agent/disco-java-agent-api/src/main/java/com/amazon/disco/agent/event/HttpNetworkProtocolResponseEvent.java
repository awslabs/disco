package com.amazon.disco.agent.event;

/**
 * An event that occurs after to serving a http, network request. This
 * event holds information about the result after serving that request.
 */
public interface HttpNetworkProtocolResponseEvent extends NetworkProtocolResponseEvent {
    /**
     * The HTTP status code
     * @return The http status code of the response.
     */
    int getStatusCode();

    /**
     * Get the HTTP Request event that came before this response event.
     * @return The HTTP Request event that occurred before this response event.
     */
    HttpNetworkProtocolRequestEvent getHttpRequestEvent();
}
