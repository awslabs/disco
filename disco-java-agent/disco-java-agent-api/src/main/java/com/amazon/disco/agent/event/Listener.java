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
 * Implement this class to supply a listener to the EventBus
 */
public interface Listener {
    /**
     * All Listeners have a priority. Events will be published to Listeners in priority order, but within the same
     * priority the order of dispatch is not defined.
     *
     * The extremes of high and low values are System priorities, which cannot be used by 3rd parties
     * You may not use Integer.MIN_VALUE or Integer.MAX_VALUE
     *
     * @return the priority of this Listener
     */
    int getPriority();

    /**
     * The receiver of the Event at the time the Event is published
     * @param e the Event being listened to
     */
    void listen(Event e);
}

