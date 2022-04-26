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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestInvocable;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestListener;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestMethodDelegationTarget;

/**
 * Instrument the target class {@link IntegTestMethodDelegationTarget} which contains 2 methods to be intercepted using the method delegation strategy.
 * Both delegation classes returning non-void and void will are tested.
 *
 * @see IntegTestMethodDelegationInterceptor for the interceptor used.
 */
public class MethodDelegationTest extends IntegtestBase{
    private static IntegTestInvocable transformed;
    private static IntegTestListener listener;

    @BeforeClass
    public static void beforeAll() {
        transformed = new IntegTestMethodDelegationTarget();

        listener = new IntegTestListener();
        listener.register();
    }

    @After
    public void after() {
        listener.events.clear();
    }


    @AfterClass
    public static void afterAll() {
        listener.unRegister();
    }

    @Test
    public void testInjectableDelegationClassesLoaded() {
        verifyClassLoaded("software.amazon.disco.instrumentation.preprocess.IntegTestDelegation");
        verifyClassLoaded("software.amazon.disco.instrumentation.preprocess.IntegTestDelegationVoid");
    }

    @Test
    public void testApplyMethodDelegationToMethodReturningString() {
        Object returned = transformed.invokeInstrumentedMethod(ARGUMENT);

        verifyInstrumentedMethodReturnObject(returned, transformed.getClass().getName(), listener, true);
    }

    @Test
    public void testApplyMethodDelegationToMethodReturningVoid() {
        transformed.invokeVoidInstrumentedMethod(ARGUMENT);

        verifyInstrumentedMethodReturnObject(null, transformed.getClass().getName(), listener, true);
    }
}
