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
 * An abstract event that defines network-based response occurrences. In a network context, this can mean that
 * a client request has been served.
 */
public abstract class AbstractNetworkProtocolResponseEvent extends AbstractProtocolResponseEvent implements NetworkProtocolResponseEvent {

    public AbstractNetworkProtocolResponseEvent(String origin, NetworkProtocolRequestEvent requestEvent) {
        super(origin, requestEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkProtocolRequestEvent getNetworkRequestEvent() {
        return (NetworkProtocolRequestEvent) super.getProtocolRequestEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.NETWORK;
    }
}
