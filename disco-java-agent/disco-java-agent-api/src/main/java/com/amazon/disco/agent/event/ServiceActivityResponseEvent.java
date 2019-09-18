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
 * Concrete ServiceEvent to express a service activity response, i.e. when the service-under-test has completed processing a transaction
 */
public class ServiceActivityResponseEvent extends AbstractServiceResponseEvent {
    /**
     * Construct a ServiceActivityResponseEvent
     * @param origin the origin of the activity e.g. 'Coral' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     * @param requestEvent the associated request event
     */
    public ServiceActivityResponseEvent(String origin, String service, String operation, ServiceRequestEvent requestEvent) {
        super(origin, service, operation, requestEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.ACTIVITY;
    }
}
