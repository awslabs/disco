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
 * Specialization of NetworkProtocolEvent for network requests. Requests and responses are dispatched separately,
 * before and after the actual invocation of the intercepted behavior occurs. At the network-level,
 * the requester can be seen as the requester to a network server, while the response is the
 * processed result of the request.
 */
public interface NetworkProtocolRequestEvent extends NetworkProtocolEvent, ProtocolRequestEvent {
    /**
     * Get the socket-level source port; the port that the caller used to send data.
     * @return The source port for this network request
     */
    int getSourcePort();

    /**
     * Get the socket-level destination port; the port that the receiving end is using to retrieve data;.
     * @return The destination port for this network request.
     */
    int getDestinationPort();

    /**
     * Get the source IP address.
     * @return The source IP address
     */
    String getSourceIP();

    /**
     * Get the destination IP address.
     * @return The destination IP address
     */
    String getDestinationIP();
}
