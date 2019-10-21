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
 * An abstract event that defines network-based request occurrences. In a network context, this can mean that
 * a client is requesting a stream connection with a server, or a packet was received at a certain port.
 */
public abstract class AbstractNetworkProtocolRequestEvent extends AbstractProtocolRequestEvent implements NetworkProtocolRequestEvent {
    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The Source port of the network entity. e.g. the port the client used to send the packet.
         */
        SOURCE_PORT,

        /**
         * The destination port of the network entity. e.g. the port the server is listening on.
         */
        DESTINATION_PORT,
    }

    public AbstractNetworkProtocolRequestEvent(String origin, int srcPort, int dstPort, String srcIP, String dstIP) {
        super(origin, srcIP, dstIP);
        withData(DataKey.SOURCE_PORT.name(), srcPort);
        withData(DataKey.DESTINATION_PORT.name(), dstPort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSourcePort() {
        return (int) getData(DataKey.SOURCE_PORT.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDestinationPort() {
        return (int) getData(DataKey.DESTINATION_PORT.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSourceIP() {
        return super.getSourceAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDestinationIP() {
        return super.getDestinationAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.NETWORK;
    }

}
