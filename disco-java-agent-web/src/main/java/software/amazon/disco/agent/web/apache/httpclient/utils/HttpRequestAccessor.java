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

import software.amazon.disco.agent.web.MethodHandleWrapper;

/**
 * Concrete accessor for the methods reflectively accessed within HttpRequest.
 */
public class HttpRequestAccessor {
    private static final String HTTP_REQUEST_CLASS_NAME = "org.apache.http.HttpRequest";

    private static final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    //these methods only use simple types, so can be initialized inline without any exception handling
    private static MethodHandleWrapper addHeader = new MethodHandleWrapper(HTTP_REQUEST_CLASS_NAME, classLoader, "addHeader", void.class, String.class, String.class);
    private static MethodHandleWrapper removeHeaders = new MethodHandleWrapper(HTTP_REQUEST_CLASS_NAME, classLoader, "removeHeaders", void.class, String.class);

    private static MethodHandleWrapper getRequestLine;
    private static MethodHandleWrapper getMethod;
    private static MethodHandleWrapper getUri;

    private final Object requestObject;

    {
        try {
            //TODO unsafe to leave these as null if they fail?
            getRequestLine = new MethodHandleWrapper(HTTP_REQUEST_CLASS_NAME, classLoader, "getRequestLine", "org.apache.http.RequestLine");
            getMethod = new MethodHandleWrapper(getRequestLine.getRtype().getName(), classLoader, "getMethod", String.class);
            getUri = new MethodHandleWrapper(getRequestLine.getRtype().getName(), classLoader, "getUri", String.class);
        } catch (Throwable t) {
            //do nothing?
        }
    }

    /**
     * Construct a new HttpRequestAccessor with a concrete HttpRequest object.
     *
     * @param args The args of HttpClient.execute, in which contains a concrete HttpRequest object to inspect
     */
    public HttpRequestAccessor(final Object...args) {
        this.requestObject = findRequestObject(args);
    }

    /**
     * Removes all headers with a certain name from this request.
     *
     * @param name The name of the headers to remove
     */
    public void removeHeaders(final String name) throws Throwable {
        removeHeaders.invoke(requestObject, name);
    }

    /**
     * Adds a header to this message. The header will be appended to the end of the list.
     *
     * @param name The name of the header
     * @param value The value of the header
     */
    public void addHeader(final String name, final String value) {
        addHeader.invoke(requestObject, name, value);
    }

    /**
     * Helper method to safely try to call the getMethod method of RequestLine within this request.
     *
     * @return The http method of this request
     */
    public String getMethod() {
        return (String)getMethod.invoke(getRequestLine.invoke(requestObject));
    }

    /**
     * Helper method to safely try to call the getUri method of RequestLine within this request.
     *
     * @return The http uri of this request
     */
    public String getUri() {
        return (String)getUri.invoke(getRequestLine.invoke(requestObject));
    }

    /**
     * Find the FIRST concrete HttpRequest object of the given type from a list of objects.
     *
     * @param args The args of HttpClient.execute, in which contains a concrete object to inspect
     * @return The first concrete object of the given type, or null if cannot find any
     */
    static Object findRequestObject(final Object... args) {
        Class<?> httpRequestClass = maybeFindClass(HTTP_REQUEST_CLASS_NAME);

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
     * @param accessClassName The class name which this accessor accesses
     * @return The Class which this accessor accesses
     */
    private static Class maybeFindClass(final String accessClassName) {
        try {
            return Class.forName(accessClassName, true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            // do nothing
        }

        return null;
    }
}
