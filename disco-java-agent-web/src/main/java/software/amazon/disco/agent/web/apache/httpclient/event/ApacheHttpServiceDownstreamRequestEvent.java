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

import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.web.apache.httpclient.utils.HttpRequestAccessor;

/**
 * Specialization allowing header replacement.
 */
class ApacheHttpServiceDownstreamRequestEvent extends HttpServiceDownstreamRequestEvent {
    private final HttpRequestAccessor accessor;
    /**
     * Construct a new ApacheHttpServiceDownstreamRequestEvent
     * @param origin the origin of the downstream call e.g. 'Web' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     * @param accessor a request accessor capable of header manipulation
     */
    public ApacheHttpServiceDownstreamRequestEvent(String origin, String service, String operation, HttpRequestAccessor accessor) {
        super(origin, service, operation);
        this.accessor = accessor;
    }

    /**
     * Replace all headers of the given name, with a new single header of the given value
     * @param name the header name
     * @param value the header value
     * @return true if successful
     */
    @Override
    public boolean replaceHeader(String name, String value) {
        accessor.removeHeaders(name);
        accessor.addHeader(name, value);
        return true;
    }
}
