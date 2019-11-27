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
 * An event that occurs prior to serving a http, network request. This
 * event holds pertinent http data that identifies a specific request.
 */
public interface HttpNetworkProtocolRequestEvent extends NetworkProtocolRequestEvent {
     /**
     * Common HTTP metadata: return the date entry from the header.
     * @return The date value
     */
    String getDate();

    /**
     * Common HTTP metadata: return the host entry from the header.
     * @return The host value
     */
    String getHost();

    /**
     * Common HTTP metadata: return the origin entry from the header.
     * @return The origin value
     */
    String getHTTPOrigin();

    /**
     * Common HTTP metadata: return the referer entry from the header. (Please note the word "referer" vs "referrer")
     * @return The referer value
     */
    String getReferer();

    /**
     * Common HTTP metadata: return the user-agent entry from the header.
     * @return The user-agent value
     */
    String getUserAgent();

    /**
     * Get the http verb/method used from the incoming request.
     * @return The method value (POST, GET, PUT, etc)
     */
    String getMethod();

    /**
     * Get the URL associated with the incoming request
     * @return The incoming URL request
     */
    String getURL();

    /**
     * Retrieve the IP address of the client that sent the request.
     * @return The remote IP address
     */
    String getRemoteIPAddress();

    /**
     * Retrieve the IP address of the interface on which the request was received.
     * @return The local IP address
     */
    String getLocalIPAddress();
}
