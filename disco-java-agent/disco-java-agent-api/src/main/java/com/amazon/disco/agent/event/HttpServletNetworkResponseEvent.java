package com.amazon.disco.agent.event;

import java.util.Map;

/**
 * A concrete implementation of the HttpNetworkProtocolResponseEvent to express
 * after an HTTP Servlet has been intercepted. This event occurs after the request
 * has been served.
 */
public class HttpServletNetworkResponseEvent extends AbstractNetworkProtocolResponseEvent implements HttpNetworkProtocolResponseEvent {

    public HttpServletNetworkResponseEvent(String origin, HttpNetworkProtocolRequestEvent requestEvent) {
        super(origin, requestEvent);
    }

    /**
     * Store all the contents of inputMap into the header map.
     * @param inputMap the source header map.
     * @return "This" for method chaining.
     */
    public HttpServletNetworkResponseEvent withHeaderMap(Map<String, String> inputMap) {
        super.withHeaderMap(inputMap);
        return this;
    }

    /**
     * Store the status code that resulted from the HTTP response.
     * @param statusCode the status code
     * @return "This" for method chaining.
     */
    public HttpServletNetworkResponseEvent withStatusCode(int statusCode) {
        super.withStatusIndicator(String.valueOf(statusCode));
        return this;
    }

    /**
     * Store the response object associated with this http response.
     * @param response the response object
     * @return "This" for method chaining.
     */
    public HttpServletNetworkResponseEvent withResponse(Object response) {
        super.withResponse(response);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getStatusCode() {
        // We stored it as a string integer, now we parse it out as an integer.
        return Integer.parseInt(super.getStatusIndicator());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpNetworkProtocolRequestEvent getHttpRequestEvent() {
        return (HttpNetworkProtocolRequestEvent) super.getProtocolRequestEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkType getNetworkType() {
        return NetworkType.TCP;
    }
}
