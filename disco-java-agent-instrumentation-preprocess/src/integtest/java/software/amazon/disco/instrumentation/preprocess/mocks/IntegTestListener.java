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

package software.amazon.disco.instrumentation.preprocess.mocks;

import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.util.LinkedHashMap;

public class IntegTestListener implements Listener {
    private LinkedHashMap<Class, Integer> eventsRegistry;

    public IntegTestListener() {
        eventsRegistry = new LinkedHashMap<>();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void listen(Event e) {
        eventsRegistry.put(e.getClass(), eventsRegistry.getOrDefault(e.getClass(), 0) + 1);
    }

    public LinkedHashMap<Class, Integer> getEventsRegistry(){
        return eventsRegistry;
    }

    public void register(){
        EventBus.addListener(this);
    }
}
