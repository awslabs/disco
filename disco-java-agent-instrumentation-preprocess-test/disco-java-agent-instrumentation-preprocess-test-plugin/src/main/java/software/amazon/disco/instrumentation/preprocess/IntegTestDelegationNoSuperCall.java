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
import net.bytebuddy.implementation.bind.annotation.This;
import software.amazon.disco.agent.reflect.event.EventBus;
import software.amazon.disco.instrumentation.preprocess.event.IntegTestEvent;

/**
 * This Delegation class has no reference to the original method since it was implemented using instrumentation. Using this delegation
 * to intercept 'non-existing' methods prevents the following exception from throwing: 'java.lang.IllegalStateException: Cannot call super (or default) method...`
 */
public class IntegTestDelegationNoSuperCall {
    public static String invokeMethodDelegation(@AllArguments final Object[] args,
                                                @This Object invoker) {
        EventBus.publish(new IntegTestEvent(invoker.getClass().getName(), args));
        return invoker.getClass().getName();
    }
}