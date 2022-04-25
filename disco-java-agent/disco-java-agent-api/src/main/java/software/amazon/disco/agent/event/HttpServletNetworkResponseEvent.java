/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.agent.event;

import java.util.List;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFirstHeader(String key) {
        List<String> headers = getHeaders(key);
        return headers !=null && !headers.isEmpty() ? headers.get(0) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getHeaders(String key) {
        return getAllHeaders().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getAllHeaders() {
        return getHeaderMap();
    }
}
