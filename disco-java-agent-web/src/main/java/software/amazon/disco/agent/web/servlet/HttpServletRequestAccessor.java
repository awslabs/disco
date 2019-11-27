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

import software.amazon.disco.agent.reflect.MethodHandleWrapper;
import software.amazon.disco.agent.web.HeaderAccessor;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Concrete accessor for the methods reflectively accessed within HttpServletRequest
 */
public class HttpServletRequestAccessor implements HeaderAccessor {
    private static final String SERVLET_REQUEST_CLASS_NAME = "javax.servlet.http.HttpServletRequest";
    private static final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    private static final MethodHandleWrapper getHeaderNames = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getHeaderNames", Enumeration.class);
    private static final MethodHandleWrapper getHeader = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getHeader", String.class, String.class);
    private static final MethodHandleWrapper getRemotePort = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getRemotePort", int.class);
    private static final MethodHandleWrapper getLocalPort = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getLocalPort", int.class);
    private static final MethodHandleWrapper getRemoteAddr = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getRemoteAddr", String.class);
    private static final MethodHandleWrapper getLocalAddr = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getLocalAddr", String.class);
    private static final MethodHandleWrapper getMethod = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getMethod", String.class);
    private static final MethodHandleWrapper getRequestURL = new MethodHandleWrapper(SERVLET_REQUEST_CLASS_NAME, classLoader, "getRequestURL", StringBuffer.class);

    private final Object requestObject;

    /**
     * Construct a new HttpServletRequestAccessor with a concrete request object
     * @param request the HttpServletRequest to inspect
     */
    HttpServletRequestAccessor(Object request) {
        this.requestObject = request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(String name) {
        try {
            return (String) getHeader.invoke(requestObject, name);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> retrieveHeaderMap() {
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

    /**
     * Get the IP port from the client-side request
     * @return the IP port used by the remote client
     */
    int getRemotePort() {
        return (int)getRemotePort.invoke(requestObject);
    }

    /**
     * Get the IP address from the client-side request
     * @return the IP address used by the remote client
     */
    String getRemoteAddr() {
        return (String)getRemoteAddr.invoke(requestObject);
    }

    /**
     * Get the IP port on the server-side
     * @return the IP port used by the service to receive the request
     */
    int getLocalPort() {
        return (int)getLocalPort.invoke(requestObject);
    }

    /**
     * Get the IP address on the server-side
     * @return the IP address used by the service to receive the request
     */
    String getLocalAddr() {
        return (String)getLocalAddr.invoke(requestObject);
    }

    /**
     * Return the HTTP method verb which was used for the request e.g. GET, PUT, ...
     * @return the HTTP verb used in the request
     */
    String getMethod() {
        return (String)getMethod.invoke(requestObject);
    }

    /**
     * Get the full URL used in the request
     * @return the request URL
     */
    String getRequestURL() {
        StringBuffer requestUrl = (StringBuffer)getRequestURL.invoke(requestObject);
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
        return (Enumeration<String>)getHeaderNames.invoke(requestObject);
    }
}
