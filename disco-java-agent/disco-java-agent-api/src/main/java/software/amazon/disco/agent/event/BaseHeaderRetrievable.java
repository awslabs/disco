/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.disco.agent.event;

import java.util.List;
import java.util.Map;

/**
 * A base interface for event consumers that wish to retrieve headers of the request/response associated with the event.
 */
public interface BaseHeaderRetrievable {
    /**
     * Returns the first header value that matches the provided key if it exists
     *
     * @param key - key of the headers to retrieve
     * @return the first header value if it exists, null otherwise
     */
    String getFirstHeader(String key);

    /**
     * Returns the header values that matches the provided key if it exists
     *
     * @param key - key of the headers to retrieve
     * @return header values if it exists, null otherwise
     */
    List<String> getHeaders(String key);

    /**
     * Returns all headers
     *
     * @return all headers as a Map with header names as keys and header values as values
     */
    Map<String, List<String>> getAllHeaders();
}
