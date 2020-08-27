/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * An event issued to the event bus when service request is cancelled
 */
public abstract class AbstractServiceCancellationEvent  extends AbstractServiceEvent implements ServiceCancellationEvent {
    /**
     * Constructor for a AbstractServiceCancellationEvent
     *
     * @param origin    the origin of this event e.g. 'Web' or 'gRPC'
     * @param service   the service name e.g. WeatherService
     * @param operation the operation name e.g getWeather
     * @param requestEvent  the associated cancelled request Event
     */
    public AbstractServiceCancellationEvent(String origin, String service, String operation, ServiceRequestEvent requestEvent) {
        super(origin, service, operation);
        withRequest(requestEvent);
    }

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
     * Add a request object to this event
     * @param request the downstream service request object
     * @return the 'this' for method chaining
     */
    public AbstractServiceCancellationEvent withRequest(ServiceRequestEvent request) {
        withData(DataKey.REQUEST.name(), request);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceRequestEvent getRequest() {
        return ServiceRequestEvent.class.cast(getData(DataKey.REQUEST.name()));
    }
}
