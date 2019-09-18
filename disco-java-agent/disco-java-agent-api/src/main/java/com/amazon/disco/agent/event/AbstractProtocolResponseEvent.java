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
 * This event stores information about the resultant outcome of the requested invocation.
 */
public abstract class AbstractProtocolResponseEvent extends AbstractProtocolEvent implements ProtocolResponseEvent {

    public AbstractProtocolResponseEvent(String origin, ProtocolRequestEvent requestEvent) {
        super(origin);
        withData(DataKey.REQUEST_EVENT.name(), requestEvent);
    }

    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The request event that became before this response.
         */
        REQUEST_EVENT,

        /**
         * The literal underlying literal protocol response object.
         */
        RESPONSE,

        /**
         * The status indicator for the protocol event.
         */
        STATUS_INDICATOR
    }

    /**
     * Add the response object into this protocol event.
     *
     * @param response The response object associated with this event.
     * @return "This" for the method chaining.
     */
    public AbstractProtocolResponseEvent withResponse(Object response) {
        withData(DataKey.RESPONSE.name(), response);
        return this;
    }

    /**
     * Add the status indicator into this event.
     *
     * @param statusIndicator The string status indicator.
     * @return "This" for the method chaining.
     */
    public AbstractProtocolResponseEvent withStatusIndicator(String statusIndicator) {
        withData(DataKey.STATUS_INDICATOR.name(), statusIndicator);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProtocolRequestEvent getProtocolRequestEvent() {
        return (ProtocolRequestEvent) getData(DataKey.REQUEST_EVENT.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getResponse() {
        return getData(DataKey.RESPONSE.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatusIndicator() {
        return (String) getData(DataKey.STATUS_INDICATOR.name());
    }

}
