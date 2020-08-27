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

package software.amazon.disco.agent.web.servlet;

import software.amazon.disco.agent.interception.annotations.DataAccessPath;
import software.amazon.disco.agent.web.HeaderAccessor;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Accessor for HttpServletRequest subclasses
 */
public interface HttpServletRequestAccessor extends HeaderAccessor {
    /**
     * Get an enumeration of header names from the request
     * @return the header names
     */
    Enumeration<String> getHeaderNames();

    /**
     * Get the value of a named header
     * @param name the name of the header
     * @return the value of the named header
     */
    String getHeader(String name);

    /**
     * Get the remote port number from the request
     * @return the remote port number
     */
    int getRemotePort();

    /**
     * Get the local port number from the request
     * @return the local port number
     */
    int getLocalPort();

    /**
     * Get the remote address, IP address or DNS name, from the request
     * @return the remote address
     */
    String getRemoteAddr();

    /**
     * Get the local address, IP address or DNS name, from the request
     * @return the local address
     */
    String getLocalAddr();

    /**
     * Get the HTTP method name, e.g. "GET" from the request
     * @return the method name
     */
    String getMethod();

    /**
     * Get the URL from the request
     * @return the URL
     */
    @DataAccessPath("getRequestURL()/toString()")
    String getRequestUrl();

    /**
     * {@inheritDoc}
     */
    @Override
    default Map<String, String> retrieveHeaderMap() {
        Map<String, String> ret = new HashMap<>();
        try {
            Enumeration<String> headerNames = getHeaderNames();
            if (headerNames == null) {
                return ret;
            }

            for (String name : Collections.list(headerNames)) {
                ret.put(name, getHeader(name));
            }
        } catch (Throwable t) {
            //do nothing
        }
        return ret;
    }
}
