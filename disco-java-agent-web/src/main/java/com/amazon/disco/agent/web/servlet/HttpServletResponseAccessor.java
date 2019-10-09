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

package com.amazon.disco.agent.web.servlet;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Concrete accessor for the methods reflectively accessed within HttpServletResponse
 */
public class HttpServletResponseAccessor extends HeaderAccessorBase {
    static AtomicReference<MethodHandle> getHeaderNamesHandle = new AtomicReference<>();
    static AtomicReference<MethodHandle> getHeaderHandle = new AtomicReference<>();

    static AtomicReference<MethodHandle> getStatusHandle = new AtomicReference<>();

    static Class responseClass = null;

    /**
     * Construct a new HttpServletResponseAccessor with a concrete request object
     * @param response the HttpServletResponse to inspect
     */
    HttpServletResponseAccessor(Object response) {
        super(response);
        if (responseClass == null) {
            try {
                responseClass = Class.forName("javax.servlet.http.HttpServletResponse", true, ClassLoader.getSystemClassLoader());
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
        Collection<String> headerNames = getHeaderNames();
        if (headerNames == null) {
            return ret;
        }

        for (String name: headerNames) {
            ret.put(name, getHeader(name));
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getClassOf() {
        return responseClass;
    }

    /**
     * Get the HTTP status code from the response
     * @return the status code e.g. 200
     */
    int getStatus() {
        return (int)maybeInitAndCall(getStatusHandle, MethodNames.GET_STATUS, int.class);
    }

    /**
     * Helper method to retrieve a collection of header names from the response
     * @return an iterable collection of all named headers
     */
    private Collection<String> getHeaderNames() {
        return (Collection<String>)maybeInitAndCall(getHeaderNamesHandle, MethodNames.GET_HEADER_NAMES, Collection.class);
    }
}
