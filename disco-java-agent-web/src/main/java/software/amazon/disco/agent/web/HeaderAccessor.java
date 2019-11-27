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

package software.amazon.disco.agent.web;

import java.util.Map;

/**
 * HTTP requests and responses need reflective access to their headers and list of header names
 * and more generally we reflectively look into them whilst composing events to publish. This interface is a base
 * for reflective accessors for all kinds of HTTP requests and responses, upstream and downstream
 */
public interface HeaderAccessor {
    /**
     * Return a map of all headers in this request or response
     * @return a String-to-String map of headers
     */
    Map<String, String> retrieveHeaderMap();

    /**
     * Get a named header from the servlet request or response
     * @param name the name of the header
     * @return the value of the header, or null if absent
     */
    String getHeader(String name);
}
