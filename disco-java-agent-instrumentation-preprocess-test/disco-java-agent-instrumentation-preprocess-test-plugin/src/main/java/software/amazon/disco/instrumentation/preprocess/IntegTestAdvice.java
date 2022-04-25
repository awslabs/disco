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

package software.amazon.disco.instrumentation.preprocess;

import net.bytebuddy.asm.Advice;
import software.amazon.disco.agent.reflect.event.EventBus;
import software.amazon.disco.instrumentation.preprocess.event.IntegTestEvent;

public class IntegTestAdvice {
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onMethodExit(@Advice.This Object thiz,
                                    @Advice.Return(readOnly = false) Object returned,
                                    @Advice.AllArguments(readOnly = false) Object... args) {
        returned = thiz.getClass().getName();
        EventBus.publish(new IntegTestEvent(thiz.getClass().getName(), args.length == 0 ? null : args[0]));
    }
}
