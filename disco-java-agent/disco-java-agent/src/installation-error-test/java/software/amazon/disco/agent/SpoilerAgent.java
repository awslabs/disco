/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An agent designed to test Disco agent's detection of a failure to instrument the class
 * java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask. The agent loads the class which would prevent
 * instrumentation of the class by Disco if Disco is loaded after this agent, because Disco instruments the class in
 * a way that cannot work if the class is already loaded (e.g. adds a field).
 */
public class SpoilerAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        ScheduledThreadPoolExecutor e = new ScheduledThreadPoolExecutor(1);
        // Schedule work just to force loading of ScheduledFutureTask
        e.schedule(new Runnable() {
            @Override
            public void run() {}
        }, 1, TimeUnit.SECONDS);
        e.shutdownNow();
    }
}
