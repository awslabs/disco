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

package software.amazon.disco.agent.web.apache.utils;

import software.amazon.disco.agent.interception.annotations.DataAccessPath;

/**
 * Data Accessor for any subtype of HttpRequest
 */
public interface HttpRequestAccessor {
    /**
     * Get the HTTP method from the request's request line.
     * The method is explicitly named, because of the range of concrete classes implementing HttpRequest. Some, like HttpGet,
     * implement a method named 'getMethod' explicitly. Others, like BasicHttpRequest, do not.
     */
    @DataAccessPath("getRequestLine()/getMethod()")
    String getMethodFromRequestLine();

    /**
     * Get the URI from the request's request line
     * @return the URI
     */
    @DataAccessPath("getRequestLine()/getUri()")
    String getUriFromRequestLine();


    /**
     * Add a new HTTP header to the request
     * @param name the header name
     * @param value the header value
     */
    void addHeader(String name, String value);

    /**
     * Remove all headers with the given name from the request
     * @param name the header name
     */
    void removeHeaders(String name);
}
