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

import java.util.List;
import java.util.Map;

/**
 * Generic interface of the Aws Service Downstream Events. Unifies common APIs that exist in both the request
 * and response events.
 */
public interface AwsServiceDownstreamEvent {

    /**
     * Keys to use in the data map
     */
    enum DataKey {
        HEADER_MAP,
    }

    /**
     * Obtain the Field value for the corresponding event. This is broken down into either its request or response
     * stream, where the request event obtains field values from the SdkRequest object and the response event obtains
     * field values from the SdkResponse object.
     * @param fieldName - The field name for the Sdk Request/Response
     * @param clazz - The class the return object should be casted into upon returning
     * @return The object in the Sdk request/response field value.
     */
    Object getValueForField(String fieldName, Class clazz);

    /**
     * Obtain the header map that the particular event holds.
     * @return A key-value pairing map of the headers.
     */
    Map<String, List<String>> getHeaderMap();
}
