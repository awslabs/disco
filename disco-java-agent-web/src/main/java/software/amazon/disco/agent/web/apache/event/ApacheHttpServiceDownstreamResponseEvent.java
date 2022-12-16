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

import org.apache.http.HttpResponse;
import software.amazon.disco.agent.event.DownstreamResponseHeaderRetrievable;
import software.amazon.disco.agent.event.HttpServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;

import java.util.List;
import java.util.Map;

/**
 * ApacheClient HttpServiceActivityResponseEvent allowing header retrieval.
 */
public class ApacheHttpServiceDownstreamResponseEvent extends HttpServiceDownstreamResponseEvent implements DownstreamResponseHeaderRetrievable {
    private final ApacheHttpClientRetrievableHeaders headers;

    /**
     * Construct a new ApacheHttpServiceDownstreamResponseEvent
     *
     * @param origin       the origin of the downstream call e.g. 'Web' or 'gRPC'
     * @param service      the service name e.g. 'WeatherService'
     * @param operation    the operation name e.g. 'getWeather'
     * @param requestEvent the associated request event
     * @param response     a HttpResponse object
     */
    public ApacheHttpServiceDownstreamResponseEvent(String origin, String service, String operation, final ServiceDownstreamRequestEvent requestEvent, HttpResponse response) {
        super(origin, service, operation, requestEvent);
        this.headers = new ApacheHttpClientRetrievableHeaders(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFirstHeader(String key) {
        return headers.getFirstHeader(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getHeaders(String key) {
        return headers.getHeaders(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getAllHeaders() {
        return headers.getAllHeaders();
    }
}
