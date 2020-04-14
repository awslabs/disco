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
 * A streamlined Data Accessor for requests such as HttpGet, HttpDelete and family, to avoid the expensive creation of
 * a RequestLine, when getting method and URI
 */
public interface HttpRequestBaseAccessor {
    /**
     * Get the HTTP method from the HttpRequestBase concrete class
     * @return the HTTP method e.g. "GET".
     */
    String getMethod();

    /**
     * Get the URI from the HttpRequestBase concrete class.
     * @return the URI from this request
     */
    @DataAccessPath("getURI()/toString()")
    String getUri();
}
