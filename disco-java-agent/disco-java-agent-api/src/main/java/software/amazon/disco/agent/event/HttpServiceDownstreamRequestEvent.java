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

/**
 * Specialization of a ServiceDownstreamRequestEvent, to encapsulate data specific to HTTP-like downstream call requests.
 */
public class HttpServiceDownstreamRequestEvent extends ServiceDownstreamRequestEvent {
    /**
     * Keys to use in the data map
     */
    enum DataKey {
        /**
         * The HTTP method e.g GET, POST, ...
         */
        METHOD,

        /**
         * The URI of the request
         */
        URI,
    }

    /**
     * Construct a new HttpServiceDownstreamRequestEvent
     * @param origin the origin of the downstream call e.g. 'Web' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     */
    public HttpServiceDownstreamRequestEvent(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * Set the HTTP method in this event
     * @param method the method e.g. GET, POST etc
     * @return 'this' for method chaining
     */
    public HttpServiceDownstreamRequestEvent withMethod(String method) {
        withData(DataKey.METHOD.name(), method);
        return this;
    }

    /**
     * Set the URI in this event
     * @param uri the URI
     * @return 'this' for method chaining
     */
    public HttpServiceDownstreamRequestEvent withUri(String uri) {
        withData(DataKey.URI.name(), uri);
        return this;
    }

    /**
     * Get the HTTP method from the event
     * @return the HTTP method
     */
    public String getMethod() {
        return (String)getData(DataKey.METHOD.name());
    }

    /**
     * Get the URI from the event
     * @return the URI
     */
    public String getUri() {
        return (String)getData(DataKey.URI.name());
    }

    /**
     * This method is deprecated. If you are authoring a Disco Event and would like to override this method,
     * implement {@link HeaderReplaceable} instead. If you are invoking replaceHeader, you should invoke it from its
     * implementing event class or by casting to {@link HeaderReplaceable} instead of invoking it from this class.
     *
     * @param name the header name
     * @param value the header value
     * @return true if successful
     */
    public boolean replaceHeader(String name, String value) {
        return false;
    }
}
