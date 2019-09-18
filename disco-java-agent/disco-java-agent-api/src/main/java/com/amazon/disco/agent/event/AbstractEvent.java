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

import java.util.HashMap;
import java.util.Map;

/**
 * All Events inherit from this base class. It contains a Map of data which can be inspected by Listeners
 */
public abstract class AbstractEvent implements Event {
    protected final String origin; //e.g. Coral
    protected final Map<String, Object> data; //needed at all, or should model more concretely in subclasses?

    /**
     * Construct a new AbstractEvent
     * @param origin a string indicating the origin of the Event such as 'Coral' or 'Concurrency'. Designed to agree
     * with the AlphaOne support package names, and may be used for logging or decision making in Listeners.
     */
    public AbstractEvent(String origin) {
        this.origin = origin;
        this.data = new HashMap<>();
    }

    /**
     * Add a data item to this event with the given name and value
     * @param key the name of this data
     * @param data the value of this data
     * @return the 'this' of the Event, to allow method chaining
     */
    public AbstractEvent withData(String key, Object data) {
        this.data.put(key, data);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOrigin() {
        return origin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getData(String key) {
        return data.get(key);
    }
}
