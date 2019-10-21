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
 * Base class for service responses, whether from an activity or a downstream
 */
public abstract class AbstractServiceResponseEvent extends AbstractServiceEvent implements ServiceResponseEvent {
    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The complete request object to the service
         */
        REQUEST,

        /**
         * The complete response object, if execution completed normally
         */
        RESPONSE,

        /**
         * The thrown exception, if execution completed abnormally
         */
        THROWN,
    }

    /**
     * Construct a new AbstractServiceResponseEvent
     * @param origin the origin of the event
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     * @param requestEvent the associated request event
     */
    public AbstractServiceResponseEvent(String origin, String service, String operation, ServiceRequestEvent requestEvent) {
        super(origin, service, operation);
        withData(DataKey.REQUEST.name(), requestEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractServiceResponseEvent withData(String key, Object data) {
        super.withData(key, data);
        return this;
    }

    /**
     * Add a response object to this event
     * @param response the response object
     * @return the 'this' for method chaining
     */
    public AbstractServiceResponseEvent withResponse(Object response) {
        withData(DataKey.RESPONSE.name(), response);
        return this;
    }

    /**
     * Add a thrown exception to this event
     * @param thrown the thrown exception
     * @return the 'this' for method chaining
     */
    public AbstractServiceResponseEvent withThrown(Throwable thrown) {
        withData(DataKey.THROWN.name(), thrown);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceRequestEvent getRequest() {
        return ServiceRequestEvent.class.cast(getData(DataKey.REQUEST.name()));
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
    public Throwable getThrown() {
        return Throwable.class.cast(getData(DataKey.THROWN.name()));
    }
}
