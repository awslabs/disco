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
package software.amazon.disco.agent.web.apache.event;

import org.apache.http.HttpRequest;
import software.amazon.disco.agent.event.DownstreamRequestHeaderRetrievable;
import software.amazon.disco.agent.event.HeaderReplaceable;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;

import java.util.List;
import java.util.Map;

/**
 * ApacheClient HttpServiceActivityRequestEvent allowing header replacement/retrieval.
 */
class ApacheHttpServiceDownstreamRequestEvent extends HttpServiceDownstreamRequestEvent implements HeaderReplaceable, DownstreamRequestHeaderRetrievable {
    private final HttpRequest request;
    private final ApacheHttpClientRetrievableHeaders headers;

    /**
     * Construct a new ApacheHttpServiceDownstreamRequestEvent
     *
     * @param origin    the origin of the downstream call e.g. 'Web' or 'gRPC'
     * @param service   the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     * @param request   a HttpRequest object capable of header manipulation
     */
    public ApacheHttpServiceDownstreamRequestEvent(String origin, String service, String operation, HttpRequest request) {
        super(origin, service, operation);
        this.request = request;
        this.headers = new ApacheHttpClientRetrievableHeaders(request);
    }

    /**
     * Replace all headers of the given name, with a new single header of the given value
     *
     * @param name  the header name
     * @param value the header value
     * @return true if successful
     */
    @Override
    public boolean replaceHeader(String name, String value) {
        request.removeHeaders(name);
        request.addHeader(name, value);
        return true;
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
