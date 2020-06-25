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
 * Specialization of a ServiceDownstreamResponseEvent, to encapsulate data specific to HTTP-like downstream call responses.
 */
public class HttpServiceDownstreamResponseEvent extends ServiceDownstreamResponseEvent {
    /**
     * Keys to use in the data map
     */
    enum DataKey {
        /**
         * The HTTP status code eg 200, 404, ...
         */
        STATUS_CODE,

        /**
         * The content length value for the returned HTTP payload
         */
        CONTENT_LENGTH
    }

    /**
     * Construct a new HttpServiceDownstreamResponseEvent
     * @param origin the origin of the downstream call e.g. 'Web' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     * @param requestEvent the associated request event
     */
    public HttpServiceDownstreamResponseEvent(String origin, String service, String operation, ServiceDownstreamRequestEvent requestEvent) {
        super(origin, service, operation, requestEvent);
    }

    /**
     * Add a status code to this Event
     * @param statusCode the HTTP status code
     * @return 'this' for method chaining
     */
    public HttpServiceDownstreamResponseEvent withStatusCode(int statusCode) {
        withData(DataKey.STATUS_CODE.name(), statusCode);
        return this;
    }

    /**
     * Add a contentLength to this Event
     * @param contentLength the HTTP content-length
     * @return 'this' for method chaining
     */
    public HttpServiceDownstreamResponseEvent withContentLength(long contentLength) {
        withData(DataKey.CONTENT_LENGTH.name(), contentLength);
        return this;
    }

    /**
     * Get the status code stored in the Event
     * @return the HTTP status code, or -1 if not available
     */
    public int getStatusCode() {
        Object statusCode = getData(DataKey.STATUS_CODE.name());
        return statusCode == null ? -1 : (int)statusCode;
    }

    /**
     * Get the content length stored in the Event
     * @return the HTTP content length, or -1 if not available
     */
    public long getContentLength() {
        Object contentLength = getData(DataKey.CONTENT_LENGTH.name());
        return contentLength == null ? -1L : (long)contentLength;
    }
}
