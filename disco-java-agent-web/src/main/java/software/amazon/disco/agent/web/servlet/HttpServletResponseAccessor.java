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

import software.amazon.disco.agent.web.HeaderAccessor;
import software.amazon.disco.agent.web.MethodHandleWrapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Concrete accessor for the methods reflectively accessed within HttpServletResponse
 */
public class HttpServletResponseAccessor implements HeaderAccessor {
    private static final String SERVLET_RESPONSE_CLASS_NAME = "javax.servlet.http.HttpServletResponse";

    private static final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    static final MethodHandleWrapper getHeaderNames = new MethodHandleWrapper(SERVLET_RESPONSE_CLASS_NAME, classLoader, "getHeaderNames", Collection.class);
    static final MethodHandleWrapper getHeader = new MethodHandleWrapper(SERVLET_RESPONSE_CLASS_NAME, classLoader, "getHeader", String.class, String.class);
    static final MethodHandleWrapper getStatus = new MethodHandleWrapper(SERVLET_RESPONSE_CLASS_NAME, classLoader, "getStatus", int.class);


    private final Object responseObject;

    /**
     * Construct a new HttpServletResponseAccessor with a concrete request object
     * @param response the HttpServletResponse to inspect
     */
    HttpServletResponseAccessor(Object response) {
        this.responseObject = response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(String name) {
        try {
            return (String) getHeader.invoke(responseObject, name);
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
            Collection<String> headerNames = getHeaderNames();
            if (headerNames == null) {
                return ret;
            }

            for (String name : headerNames) {
                try {
                    ret.put(name, getHeader(name));
                } catch (Throwable t) {
                    //do nothing
                }
            }
        } catch (Throwable t) {
            //do nothing
        }

        return ret;
    }

    /**
     * Get the HTTP status code from the response
     * @return the status code e.g. 200
     */
    int getStatus() {
        return (int)getStatus.invoke(responseObject);
    }

    /**
     * Helper method to retrieve a collection of header names from the response
     * @return an iterable collection of all named headers
     */
    private Collection<String> getHeaderNames() {
        return (Collection<String>)getHeaderNames.invoke(responseObject);
    }
}
