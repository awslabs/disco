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
package software.amazon.disco.agent.web.apache.event;

import org.apache.http.Header;
import org.apache.http.HttpMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes HttpMessage headers to a Map<String, List<String>>
 */
class ApacheHttpClientRetrievableHeaders {
    private final Map<String, List<String>> headers;

    /**
     * Constructor takes HttpMessage param
     *
     * @param httpMessage HttpMessage base class of HttpRequest or HttpResponse
     */
    public ApacheHttpClientRetrievableHeaders(HttpMessage httpMessage) {
        this.headers = createMapFromHttpHeaders(httpMessage);
    }

    /**
     * Serialize headers to a generic Map
     *
     * @param httpMessage HttpMessage containing the HttpHeaders to serialize
     * @return The Map representation of the headers
     */
    private static Map<String, List<String>> createMapFromHttpHeaders(HttpMessage httpMessage) {
        if (httpMessage == null) {
            return Collections.emptyMap();
        }

        Header[] headers = httpMessage.getAllHeaders();
        if (headers == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> headerMap = new HashMap<>();
        for (Header h : headers) {
            String name = h.getName();
            if (!headerMap.containsKey(name)) {
                headerMap.put(name, new ArrayList<>());
            }
            headerMap.get(name).add(h.getValue());
        }

        return headerMap;
    }

    /**
     * Retrieve the first header value
     *
     * @param s header name
     * @return first header value by name s or null otherwise
     */
    public String getFirstHeader(String s) {
        List<String> headerList = getHeaders(s);
        if (headerList == null) {
            return null;
        }
        return headerList.get(0);

    }

    /**
     * Retrieve the headers by header name
     *
     * @param s header name
     * @return header values by name s or null otherwise
     */
    public List<String> getHeaders(String s) {
        return headers.get(s);
    }

    /**
     * Retrieve all the headers
     *
     * @return map of all the headers by key value pair
     */
    public Map<String, List<String>> getAllHeaders() {
        return headers;
    }
}
