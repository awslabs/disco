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
 * Base class for service requests, whether from an activity or a downstream
 */
abstract public class AbstractServiceRequestEvent extends AbstractServiceEvent implements ServiceRequestEvent {
    /**
     * Data keys
     */
    enum DataKey {
        /**
         * The complete request object to the service
         */
        REQUEST
    }

    /**
     * Create a new AbstractServiceRequestEvent
     * @param origin the origin of the event
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     */
    public AbstractServiceRequestEvent(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * Add a request object to this event
     * @param request the request object
     * @return the 'this' for method chaining
     */
    public AbstractServiceRequestEvent withRequest(Object request) {
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
}
