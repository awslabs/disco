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

package software.amazon.disco.agent.web.apache.httpclient.event;

import software.amazon.disco.agent.web.apache.httpclient.utils.HttpRequestAccessor;

/**
 * Create our private events, so that listeners do not have public access to them
 */
public class ApacheEventFactory {
    public static ApacheHttpServiceDownstreamRequestEvent createDownstreamRequestEvent(String service, String operation, HttpRequestAccessor accessor) {
        return new ApacheHttpServiceDownstreamRequestEvent("ApacheHttpClient", service, operation, accessor);
    }
}
