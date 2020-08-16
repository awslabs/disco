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
import java.util.Optional;

import static software.amazon.disco.agent.event.AwsServiceDownstreamEvent.DataKey.HEADER_MAP;

/**
 * Specialization of a ServiceDownstreamResponseEvent, to encapsulate data specific to Aws downstream call responses.
 * Implementation of inherited methods are in the disco-java-agent-aws package.
 */
public abstract class AwsServiceDownstreamResponseEvent extends ServiceDownstreamResponseEvent implements AwsServiceDownstreamEvent {
    /**
     * Keys to use in the data map
     */
    enum DataKey {
        /**
         * The Request Id of the current sdk request
         */
        REQUEST_ID,

        /**
         * The number of retries the AWS SDK used to perform this request
         */
        RETRIES
    }

    /**
     * Construct a new AwsServiceDownstreamResponseEvent
     * @param origin the origin of the downstream call e.g. 'AWSv1' or 'AWSv2'
     * @param service the service name e.g. 'DynamoDb'
     * @param operation the operation name e.g. 'ListTables'
     * @param requestEvent the associated request event
     */
    public AwsServiceDownstreamResponseEvent(String origin, String service, String operation, ServiceDownstreamRequestEvent requestEvent) {
        super(origin, service, operation, requestEvent);
    }

    /**
     * Obtain the overall status code of the downstream call. If we can't obtain it, we return -1.
     * @return The status code of the call
     */
    public abstract int getStatusCode();

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getHeaderMap() {
        return (Map<String, List<String>>) getData(HEADER_MAP.name());
    }

    /**
     * Retrieve the request ID of the downstream call
     * @return the request ID
     */
    public String getRequestId() {
        return (String) getData(DataKey.REQUEST_ID.name());
    }

    /**
     * Retrieve the number of times the downstream call had retried.
     * @return The retry count.
     */
    public int getRetryCount() {
        Object retryCountObj = getData(DataKey.RETRIES.name());
        if (retryCountObj == null) {
            return 0; // Assume if we've never set it, then we've never retried.
        }

        return (int) retryCountObj;
    }
}
