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

package software.amazon.disco.instrumentation.preprocess.source;

import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ThreadEvent;
import software.amazon.disco.agent.reflect.event.EventBus;
import software.amazon.disco.instrumentation.preprocess.event.IntegTestEvent;

import java.util.LinkedList;

public class IntegTestListener implements Listener {
    public LinkedList<IntegTestEvent> events;

    public IntegTestListener() {
        events = new LinkedList<>();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void listen(Event e) {
        if (!(e instanceof ThreadEvent)) {
            events.add((IntegTestEvent) e);
        }
    }

    public void register() {
        EventBus.addListener(this);
    }
    public void unRegister() {EventBus.removeListener(this);}
}
