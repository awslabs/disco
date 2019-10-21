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
 * An event issued to the event bus on a service activity or downstream call
 */
public interface ServiceEvent extends Event {
    /**
     * The type of this ServiceEvent
     */
    enum Type {
        /**
         * If the event is 'my service is handling a request'
         */
        ACTIVITY,

        /**
         * If the event is 'I am making a request of a dependency'
         */
        DOWNSTREAM
    }

    /**
     * Get the service name
     * @return the service name e.g. 'WeatherService'
     */
    String getService();

    /**
     * Get the operation name
     * @return the operation name e.g. 'getWeather'
     */
    String getOperation();

    /**
     * Get the type of this ServiceEvent
     * @return either ACTIVITY or DOWNSTREAM
     */
    Type getType();
}
