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

package software.amazon.disco.agent.web.apache.httpclient.source;

import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class MockEventBusListener implements Listener {


    public List<Event> getReceivedEvents() {
        return receivedEvents;
    }

    private List<Event> receivedEvents = new ArrayList<>();

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void listen(Event event) {
        receivedEvents.add(event);
    }
}
