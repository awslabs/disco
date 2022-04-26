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
 * An event that occurs after to serving a http, network request. This
 * event holds information about the result after serving that request.
 */
public interface HttpNetworkProtocolResponseEvent extends NetworkProtocolResponseEvent, HeaderRetrievable {
    /**
     * The HTTP status code
     * @return The http status code of the response.
     */
    int getStatusCode();

    /**
     * Get the HTTP Request event that came before this response event.
     * @return The HTTP Request event that occurred before this response event.
     */
    HttpNetworkProtocolRequestEvent getHttpRequestEvent();
}
