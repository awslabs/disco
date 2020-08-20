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

import static software.amazon.disco.agent.event.AwsServiceDownstreamEvent.DataKey.HEADER_MAP;

/**
 * Specialization of a ServiceDownstreamRequestEvent, to encapsulate data specific to Aws downstream call requests.
 * Implementation of inherited methods are in the disco-java-agent-aws package.
 */
public abstract class AwsServiceDownstreamRequestEvent extends ServiceDownstreamRequestEvent implements AwsServiceDownstreamEvent, HeaderReplaceable {
    /**
     * Keys to use in the data map
     */
    enum DataKey {
        /**
         * The region of the request
         */
        REGION,
    }

    /**
     * Construct a new AwsServiceDownstreamRequestEvent
     * @param origin the origin of the downstream call e.g. 'AWSv1' or 'AWSv2'
     * @param service the service name e.g. 'DynamoDb'
     * @param operation the operation name e.g. 'ListTables'
     */
    public AwsServiceDownstreamRequestEvent(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * Retrieve the region for this event
     * @return the region the downstream call is making to.
     */
    public String getRegion() {
        return (String)getData(DataKey.REGION.name());
    }

    /**
     * Retrieve the underlying Http header map for the outgoing request.
     * @return an immutable header map
     */
    public Map<String, List<String>> getHeaderMap() {
        return (Map<String, List<String>>) getData(HEADER_MAP.name());
    }

}
