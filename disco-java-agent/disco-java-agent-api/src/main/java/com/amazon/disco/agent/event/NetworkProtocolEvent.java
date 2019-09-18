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

package com.amazon.disco.agent.event;

/**
 * An event issued to the event bus on a network-level call. The network event here
 * is driven by anything that may communicate through sockets using a transport-layer protocol.
 */
public interface NetworkProtocolEvent extends ProtocolEvent {
    /**
     * The underlying network protocol used
     */
    enum NetworkType {
        /**
         * The layer being observed uses TCP as its transport protocol
         */
        TCP,

        /**
         * The layer being observed uses UDP as its transport protocol
         */
        UDP
    }

    /**
     * Get the network type of this ProtocolNetworkEvent
     * @return return either TCP or UDP.
     */
    NetworkType getNetworkType();
}
