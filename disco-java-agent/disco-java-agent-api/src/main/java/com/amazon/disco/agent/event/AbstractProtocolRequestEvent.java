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
 * This event stores information about the invoker of the protocol event.
 */
public abstract class AbstractProtocolRequestEvent extends AbstractProtocolEvent implements ProtocolRequestEvent {

    public AbstractProtocolRequestEvent(String origin, String srcAddr, String dstAddr) {
        super(origin);
        withData(DataKey.SRC_ADDRESS.name(), srcAddr);
        withData(DataKey.DST_ADDRESS.name(), dstAddr);
    }

    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The literal underlying protocol request object.
         */
        REQUEST,

        /**
         * The source address for this protocol.
         */
        SRC_ADDRESS,

        /**
         * The destination address for this protocol.
         */
        DST_ADDRESS
    }

    /**
     * The literal request object stored in the protocol event.
     *
     * @param request The request object associated with this protocol.
     * @return The request object.
     */
    public AbstractProtocolRequestEvent withRequest(Object request) {
        withData(DataKey.REQUEST.name(), request);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getRequest() {
        return getData(DataKey.REQUEST.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSourceAddress() {
        return (String) getData(DataKey.SRC_ADDRESS.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDestinationAddress() {
        return (String) getData(DataKey.DST_ADDRESS.name());
    }

}
