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

package software.amazon.disco.agent.web.apache.httpclient.utils;

import java.lang.invoke.MethodHandle;
import java.util.AbstractMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Concrete accessor for the methods reflectively accessed within HttpRequest.
 */
public class HttpRequestAccessor extends AccessorBase {
    private static final String HTTP_REQUEST_CLASS_NAME = "org.apache.http.HttpRequest";
    private static final String REQUEST_LINE_CLASS_NAME = "org.apache.http.RequestLine";

    private static AtomicReference<MethodHandle> addHeaderMethodHandle = new AtomicReference<>();
    private static AtomicReference<MethodHandle> removeHeadersMethodHandle = new AtomicReference<>();
    private static AtomicReference<MethodHandle> getRequestLineMethodHandle = new AtomicReference<>();

    static Class httpRequestClass = null;
    static Class requestLineClass = null;

    AtomicReference<RequestLineAccessor> requestLineAccessor = new AtomicReference<>();

    /**
     * Construct a new HttpRequestAccessor with a concrete HttpRequest object.
     *
     * @param args The args of HttpClient.execute, in which contains a concrete HttpRequest object to inspect
     */
    public HttpRequestAccessor(final Object...args) {
        super(findRequestObject(args));
    }

    /**
     * @return The real class which this accessor accesses
     */
    @Override
    protected Class<?> getClassOf() {
        return httpRequestClass;
    }

    /**
     * Removes all headers with a certain name from this request.
     *
     * @param name The name of the headers to remove
     */
    public void removeHeaders(final String name) {
        maybeInitAndCall(removeHeadersMethodHandle,
                        MethodNames.REMOVE_HEADERS,
                        void.class,
                        new AbstractMap.SimpleImmutableEntry<>(String.class, name));
    }

    /**
     * Adds a header to this message. The header will be appended to the end of the list.
     *
     * @param name The name of the header
     * @param value The value of the header
     */
    public void addHeader(final String name, final String value) {
        maybeInitAndCall(addHeaderMethodHandle,
                        MethodNames.ADD_HEADER,
                        void.class,
                        new AbstractMap.SimpleImmutableEntry<>(String.class, name),
                        new AbstractMap.SimpleImmutableEntry<>(String.class, value));
    }

    /**
     * Helper method to safely try to call the getMethod method of RequestLine within this request.
     *
     * @return The http method of this request
     */
    public String getMethod() {
        maybeInitRequestLineAccessor();
        if (requestLineAccessor.get() != null) {
            return requestLineAccessor.get().getMethod();
        }
        return null;
    }

    /**
     * Helper method to safely try to call the getUri method of RequestLine within this request.
     *
     * @return The http uri of this request
     */
    public String getUri() {
        maybeInitRequestLineAccessor();
        if (requestLineAccessor.get() != null) {
            return requestLineAccessor.get().getUri();
        }
        return null;
    }

    /**
     * @return The HttpRequest object.
     */
    public Object getRequest() {
        return getObject();
    }

    /**
     * Helper method to safely try to initialize the RequestLineAccessor prior to calling it.
     */
    private void maybeInitRequestLineAccessor() {
        if (requestLineAccessor.get() != null) {
            return;
        }
        requestLineClass = maybeFindClass(requestLineClass, REQUEST_LINE_CLASS_NAME);
        final Object requestLineObject = maybeInitAndCall(getRequestLineMethodHandle, MethodNames.GET_REQUEST_LINE, requestLineClass);
        if (requestLineObject == null) {
            // try next time
            return;
        }
        requestLineAccessor.compareAndSet(null, new RequestLineAccessor(requestLineObject, requestLineClass));
    }

    /**
     * Find the FIRST concrete HttpRequest object from a list of objects.
     *
     * @param args The args of HttpClient.execute, in which contains a concrete HttpRequest object to inspect
     * @return The first concrete HttpRequest object, or null if cannot find any
     */
    static Object findRequestObject(final Object... args) {
        httpRequestClass = maybeFindClass(httpRequestClass, HTTP_REQUEST_CLASS_NAME);

        // prefer array access via indexes over foreach for the sake of performance
        for (int i = 0; i < args.length; i++) {
            if (httpRequestClass != null && httpRequestClass.isInstance(args[i])) {
                return args[i];
            }
        }
        return null;
    }

    /**
     * Lookup a Class by name if encountering for the first time.
     *
     * @param accessingClass The Class which this accessor accesses, could be null for the first time
     * @param accessClassName The class name which this accessor accesses
     * @return The Class which this accessor accesses
     */
    private static Class maybeFindClass(final Class accessingClass, final String accessClassName) {
        if (accessingClass == null) {
            try {
                return Class.forName(accessClassName, true, ClassLoader.getSystemClassLoader());
            } catch (ClassNotFoundException e) {
                // do nothing
            }
        }
        return accessingClass;
    }

    /**
     * Constants of the method names which will be reflectively called on the HttpRequest objects
     */
    private static class MethodNames {
        private static final String ADD_HEADER = "addHeader";
        private static final String REMOVE_HEADERS = "removeHeaders";
        private static final String GET_REQUEST_LINE = "getRequestLine";
    }

    /**
     * Concrete accessor for the methods reflectively accessed within RequestLine.
     */
    static class RequestLineAccessor extends AccessorBase {
        private static AtomicReference<MethodHandle> getMethodMethodHandle = new AtomicReference<>();
        private static AtomicReference<MethodHandle> getUriMethodHandle = new AtomicReference<>();
        private static Class requestLineClass = null;

        /**
         * Construct a new RequestLineAccessor with a concrete RequestLine object.
         *
         * @param requestLine The RequestLine object to inspect
         * @param clazz The RequestLine class
         */
        RequestLineAccessor(final Object requestLine, final Class clazz) {
            super(requestLine);
            requestLineClass = clazz;
        }

        /**
         * @return The real class which this accessor accesses.
         */
        @Override
        protected Class<?> getClassOf() {
            return requestLineClass;
        }

        /**
         * @return The http method of this request line.
         */
        public String getMethod() {
            return (String) maybeInitAndCall(getMethodMethodHandle, MethodNames.GET_METHOD, String.class);
        }

        /**
         * @return The http uri of this request line.
         */
        public String getUri() {
            return (String) maybeInitAndCall(getUriMethodHandle, MethodNames.GET_URI, String.class);
        }

        /**
         * Constants of the method names which will be reflectively called on the RequestLine objects
         */
        private static class MethodNames {
            private static final String GET_METHOD = "getMethod";
            private static final String GET_URI = "getUri";
        }
    }
}
