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

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.lang.reflect.Method;

/**
 * Extension of {@link ServiceDownstreamRequestEvent} that implements a AWS SDK specific replace header method.
 */
public class AwsV1ServiceDownstreamRequestEventImpl extends ServiceDownstreamRequestEvent implements HeaderReplaceable {
    private static final Logger log = LogManager.getLogger(AwsV1ServiceDownstreamRequestEventImpl.class);
    private static final String ADD_HEADER = "addHeader";

    /**
     * {@inheritDoc}
     */
    public AwsV1ServiceDownstreamRequestEventImpl(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replaceHeader(String key, String value) {
        Object awsSdkRequest = this.getRequest();
        try {
            Method addHeader = awsSdkRequest.getClass().getDeclaredMethod(ADD_HEADER, String.class, String.class);
            addHeader.invoke(awsSdkRequest, key, value);
            return true;
        } catch (Exception e) {
            log.warn("Disco(AWSv1) Failed to add header '" + key + "' to AWS SDK Request", e);
            return false;
        }
    }
}
