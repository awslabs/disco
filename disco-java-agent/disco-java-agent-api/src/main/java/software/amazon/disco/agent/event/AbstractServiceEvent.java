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
 * Base class for service events both Activities (my service is handling a request) and Downstream (I am calling another Service)
 * A 'Service' in the downstream sense may not necessarily be a remote endpoint in the sense of a REST service etc. It
 * could be an in-process client for e.g. a Cache. It means a service in the sense of anything with an API,
 * whether that is remote or local.
 */
public abstract class AbstractServiceEvent extends AbstractEvent implements ServiceEvent {

    /**
     * The data which ServiceEvents may contain
     */
    enum DataKey {
        /**
         * The service name, e.g. 'WeatherService'
         */
        SERVICE,

        /**
         * The operation name e.g. 'getWeather'
         */
        OPERATION
    }

    /**
     * Constructor for a ServiceEvent
     * @param origin the origin of this event e.g. 'Web' or 'gRPC'
     * @param service the service name e.g. WeatherService
     * @param operation the operation name e.g getWeather
     */
    public AbstractServiceEvent(String origin, String service, String operation) {
        super(origin);
        withData(DataKey.SERVICE.name(), service);
        withData(DataKey.OPERATION.name(), operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getService() {
        return String.class.cast(getData(DataKey.SERVICE.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperation() {
        return String.class.cast(getData(DataKey.OPERATION.name()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Type getType();
}
