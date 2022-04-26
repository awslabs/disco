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

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import software.amazon.disco.agent.reflect.event.EventBus;
import software.amazon.disco.instrumentation.preprocess.event.IntegTestEvent;

import java.util.concurrent.Callable;

public class IntegTestDelegation {
    public static String invokeMethodDelegation(@AllArguments final Object[] args,
                                                @This Object invoker,
                                                @SuperCall Callable<String> zuper) {
        EventBus.publish(new IntegTestEvent(invoker.getClass().getName(), args));
        return invoker.getClass().getName();
    }
}
