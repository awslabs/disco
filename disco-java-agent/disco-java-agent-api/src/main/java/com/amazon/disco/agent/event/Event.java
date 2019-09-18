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
 * An Event published to the AlphaOne event bus
 */
public interface Event {
    /**
     * Getter for the Event's origin. This is named for the AlphaOne support package name e.g. 'Coral'
     * @return the origin of this event
     */
    String getOrigin();

    /**
     * Retrieve a data value from this event, given a named key
     * @param key the name of the data to retrieve
     * @return the value of the data, or null if absent
     */
    Object getData(String key);
}
