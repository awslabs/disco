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

package software.amazon.disco.instrumentation.preprocess.event;

import software.amazon.disco.agent.event.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class IntegTestEvent implements Event {
    private final String origin;
    private final Map<String, Object> data;

    public IntegTestEvent(String origin, Object... arg) {
        this.origin = origin;
        this.data = Collections.singletonMap(
            "arg",
            arg.length == 1 ? arg[0] : new ArrayList<>(Arrays.asList(arg))
        );
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public Object getData(String key) {
        return data.get(key);
    }
}
