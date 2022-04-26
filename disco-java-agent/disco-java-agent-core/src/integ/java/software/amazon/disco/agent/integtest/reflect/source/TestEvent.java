/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.integtest.reflect.source;

import software.amazon.disco.agent.event.AbstractEvent;
import software.amazon.disco.agent.event.Event;

public class TestEvent extends AbstractEvent implements Event {
    /**
     * Construct a new AbstractEvent
     *
     * @param origin a string indicating the origin of the Event such as 'Concurrency'. Designed to agree
     *               with the DiSCo support package names, and may be used for logging or decision making in Listeners.
     */
    public TestEvent(String origin) {
        super(origin);
    }
}
