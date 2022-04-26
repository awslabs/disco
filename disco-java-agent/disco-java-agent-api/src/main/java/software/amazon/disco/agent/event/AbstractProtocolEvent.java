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

package software.amazon.disco.agent.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for protocol events; These events here typically occur around service events and
 * provide the mechanisms that make them work.
 */
public abstract class AbstractProtocolEvent extends AbstractEvent implements ProtocolEvent {

    public AbstractProtocolEvent(String origin) {
        super(origin);
        withData(DataKey.HEADER_MAP.name(), new HashMap<String, List<String>>());
    }

    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The map used to store header data.
         */
        HEADER_MAP
    }

    /**
     * Get the internal header map.
     *
     * @return the internal header map.
     */
    protected Map<String, List<String>> getHeaderMap() {
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headerMap = (Map<String, List<String>>) getData(DataKey.HEADER_MAP.name());
        return headerMap;
    }

    /**
     * Populate the contents of the inputMap into the header map.
     *
     * @param inputMap the input map to add onto the header map.
     * @return the 'this' for method chaining
     */
    public AbstractProtocolEvent withHeaderMap(Map<String, String> inputMap) {
        Map<String, List<String>> transformedInputMap = new HashMap<>();
        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            transformedInputMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
        }
        getHeaderMap().putAll(transformedInputMap);
        return this;
    }

    /**
     * Stores the key-value pair into the header map.
     *
     * @param key   the key used to associate a header mapping.
     * @param value The header value associated with the key in the header.
     * @return "This" for method chaining.
     */
    public AbstractProtocolEvent withHeaderData(String key, String value) {
        if (data == null) {
            // When it's null, we don't populate the map.
            // Retrieval will return null, so the listener will know it's unable to retrieve the data.
            return this;
        }
        Map<String, List<String>> headerMap = getHeaderMap();
        headerMap.put(key, Collections.singletonList(value));
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated deprecated in favour of {@link HeaderRetrievable} which should be used wherever possible instead.
     */
    @Deprecated
    @Override
    public String getHeaderData(String key) {
        List<String> headers = getHeaderMap().get(key);
        return headers !=null && !headers.isEmpty() ? headers.get(0) : null;
    }

}
