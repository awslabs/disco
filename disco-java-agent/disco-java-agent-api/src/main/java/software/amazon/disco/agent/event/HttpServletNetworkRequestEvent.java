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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A concrete implementation of the HttpNetworkProtocolRequestEvent to express
 * when an HTTP Servlet has been intercepted. This event occurs prior to the request
 * being served.
 */
public class HttpServletNetworkRequestEvent extends AbstractNetworkProtocolRequestEvent implements HttpNetworkProtocolRequestEvent {
    private String method;
    private String httpURL;

    private static final String DATE_HEADER = "date";
    private static final String HOST_HEADER = "host";
    private static final String ORIGIN_HEADER = "origin";
    private static final String REFERER_HEADER = "referer";
    private static final String USER_AGENT_HEADER = "user-agent";

    public HttpServletNetworkRequestEvent(String origin, int srcPort, int dstPort, String srcIP, String dstIP) {
        super(origin, srcPort, dstPort, srcIP, dstIP);
    }

    /**
     * Helper method to allow for chaining with thsi method call.
     * This method stores the key-value pair into the header map
     * @param key the associated header key
     * @param data the associated header value
     * @return "This" for method chaining.
     */
    private HttpServletNetworkRequestEvent withHeaderInput(String key, String data) {
        super.withHeaderData(key, data);
        return this;
    }

    /**
     * Common HTTP Metadata: Store the date into the header map
     * @param date the date value
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withDate(String date) {
        return withHeaderInput(DATE_HEADER, date);
    }

    /**
     * Common HTTP Metadata: Store the host into the header map
     * @param host the host value
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withHost(String host) {
        return withHeaderInput(HOST_HEADER, host);
    }

    /**
     * Common HTTP Metadata: Store the origin into the header map
     * @param origin the Origin value
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withHTTPOrigin(String origin) {
        return withHeaderInput(ORIGIN_HEADER, origin);
    }

    /**
     * Common HTTP Metadata: Store the referer into the header map
     * @param referer the referer value
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withReferer(String referer) {
        return withHeaderInput(REFERER_HEADER, referer);
    }

    /**
     * Common HTTP Metadata: Store the user-agent into the header map
     * @param userAgent the user-agent value
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withUserAgent(String userAgent) {
        return withHeaderInput(USER_AGENT_HEADER, userAgent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpServletNetworkRequestEvent withHeaderMap(Map<String, String> inputMap) {
        super.withHeaderMap(inputMap);
        return this;
    }

    /**
     * Store the HTTP method/verb into this event
     * @param method The HTTP method/verb. Can be "POST", "GET", "PUT", etc
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withMethod(String method) {
        this.method = method;
        return this;
    }

    /**
     * Store the HTTP URL from the incoming request into this event.
     * @param url the URL from the request.
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withURL(String url) {
        this.httpURL = url;
        return this;
    }

    /**
     * Store the literal HTTP request object into this event.
     * @param request the HTTP request object. Most commonly the ServletRequest object.
     * @return "This" for method chaining.
     */
    public HttpServletNetworkRequestEvent withRequest(Object request) {
        super.withRequest(request);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated deprecated in favour of {@link HeaderRetrievable} which should be used wherever possible instead.
     */
    @Deprecated
    @Override
    public String getHeaderData(String key) {
        return super.getHeaderData(key);
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

    /**
     * Common HTTP Metadata
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDate() {
        return getHeaderData(DATE_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return getHeaderData(HOST_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHTTPOrigin() {
        return getHeaderData(ORIGIN_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReferer() {
        return getHeaderData(REFERER_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgent() {
        return getHeaderData(USER_AGENT_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMethod() {
        return this.method;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getURL() {
        return this.httpURL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteIPAddress() {
        return super.getSourceIP();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalIPAddress() {
        return super.getDestinationIP();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkType getNetworkType() {
        return NetworkType.TCP;
    }
}
