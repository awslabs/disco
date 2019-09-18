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
 * Concrete ServiceEvent to express a service downstream call which is about to occur, i.e. when the service-under-test
 * is making a request of an intercepted subsystem e.g. a remote service or an in-process Cache client.
 */
public class ServiceDownstreamRequestEvent extends AbstractServiceRequestEvent {
    /**
     * Construct a ServiceDownstreamRequestEvent
     * @param origin the origin of the downstream call e.g. 'Coral' or 'gRPC'
     * @param service the service name e.g. 'WeatherService'
     * @param operation the operation name e.g. 'getWeather'
     */
    public ServiceDownstreamRequestEvent(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return Type.DOWNSTREAM;
    }
}

