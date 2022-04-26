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
 * An event issued to the event bus on a protocol-level call. Protocol isn't defined here
 * in the general sense of the OSI layer. It can be described in terms of such events like
 * Command Line invocations, File System events, and network events.
 */
public interface ProtocolEvent extends Event {
    /**
     * The type of this ProtocolEvent
     */
    enum Type {
        /**
         * If the event is 'I'm sending/receiving some data through the network layer'
         */
        NETWORK
    }

    /**
     * Get the value of header information tied to this event.
     * @param key the name of the header
     * @return the header value associated with the key.
     *
     * @deprecated deprecated in favour of {@link HeaderRetrievable} which should be used wherever possible instead.
     */
    @Deprecated
    String getHeaderData(String key);

    /**
     * Get the type of this ProtocolRequestEvent
     * @return NETWORK for now
     */
    Type getType();
}