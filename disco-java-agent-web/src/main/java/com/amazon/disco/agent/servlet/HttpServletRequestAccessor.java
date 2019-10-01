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

package com.amazon.disco.agent.servlet;

import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Concrete accessor for the methods reflectively accessed within HttpServletRequest
 */
public class HttpServletRequestAccessor extends HeaderAccessorBase {
    static AtomicReference<MethodHandle> getHeaderNamesHandle = new AtomicReference<>();
    static AtomicReference<MethodHandle> getHeaderHandle = new AtomicReference<>();

    static AtomicReference<MethodHandle> getRemotePortHandle = new AtomicReference<>();
    static AtomicReference<MethodHandle> getRemoteAddrHandle = new AtomicReference<>();
    static AtomicReference<MethodHandle> getLocalPortHandle = new AtomicReference<>();
    static AtomicReference<MethodHandle> getLocalAddrHandle = new AtomicReference<>();
    static AtomicReference<MethodHandle> getMethodHandle = new AtomicReference<>();
    static AtomicReference<MethodHandle> getRequestURLHandle = new AtomicReference<>();

    static Class requestClass = null;

    /**
     * Construct a new HttpServletRequestAccessor with a concrete request object
     * @param request the HttpServletRequest to inspect
     */
    HttpServletRequestAccessor(Object request) {
        super(request);
        if (requestClass == null) {
            try {
                requestClass = Class.forName("javax.servlet.http.HttpServletRequest", true, ClassLoader.getSystemClassLoader());
            } catch (ClassNotFoundException e) {
                //try again next time?
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(String name) {
        return (String)maybeInitAndCall(getHeaderHandle, MethodNames.GET_HEADER, String.class, String.class, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> retrieveHeaderMap() {
        Map<String, String> ret = new HashMap<>();
        Enumeration<String> headerNames = getHeaderNames();
        if (headerNames == null) {
            return ret;
        }

        for (String name: Collections.list(headerNames)) {
            ret.put(name, getHeader(name));
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getClassOf() {
        return requestClass;
    }

    /**
     * Get the IP port from the client-side request
     * @return the IP port used by the remote client
     */
    int getRemotePort() {
        return (int)maybeInitAndCall(getRemotePortHandle, MethodNames.GET_REMOTE_PORT, int.class);
    }

    /**
     * Get the IP address from the client-side request
     * @return the IP address used by the remote client
     */
    String getRemoteAddr() {
        return (String)maybeInitAndCall(getRemoteAddrHandle, MethodNames.GET_REMOTE_ADDR, String.class);
    }

    /**
     * Get the IP port on the server-side
     * @return the IP port used by the service to receive the request
     */
    int getLocalPort() {
        return (int)maybeInitAndCall(getLocalPortHandle, MethodNames.GET_LOCAL_PORT, int.class);
    }

    /**
     * Get the IP address on the server-side
     * @return the IP address used by the service to receive the request
     */
    String getLocalAddr() {
        return (String)maybeInitAndCall(getLocalAddrHandle, MethodNames.GET_LOCAL_ADDR, String.class);
    }

    /**
     * Return the HTTP method verb which was used for the request e.g. GET, PUT, ...
     * @return the HTTP verb used in the request
     */
    String getMethod() {
        return (String)maybeInitAndCall(getMethodHandle, MethodNames.GET_METHOD, String.class);
    }

    /**
     * Get the full URL used in the request
     * @return the request URL
     */
    String getRequestURL() {
        StringBuffer requestUrl = (StringBuffer) maybeInitAndCall(getRequestURLHandle, MethodNames.GET_REQUEST_URL, StringBuffer.class);
        if (requestUrl == null) {
            return null;
        }
        return requestUrl.toString();
    }

    /**
     * Helper method to retrieve an enumeration of header names from the response
     * @return an enumeration of all named headers
     */
    private Enumeration<String> getHeaderNames() {
        return (Enumeration<String>)maybeInitAndCall(getHeaderNamesHandle, MethodNames.GET_HEADER_NAMES, Enumeration.class);
    }
}
