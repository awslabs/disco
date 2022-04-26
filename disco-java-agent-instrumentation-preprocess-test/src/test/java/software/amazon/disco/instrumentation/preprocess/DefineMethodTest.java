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
import software.amazon.disco.instrumentation.preprocess.source.IntegTestDefineMethodTarget;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestInvocable;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestListener;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * Instrument the target class {@link IntegTestDefineMethodTarget} to have a getter and a setter for accessing/setting an existing
 * private field as well as defining a new method.
 *
 * @see IntegTestDefineMethodInterceptor for the interceptor used.
 */
public class DefineMethodTest extends IntegtestBase {
    private static IntegTestListener listener;
    private static IntegTestInvocable instrumentedTarget;

    @BeforeClass
    public static void beforeAll() {
        instrumentedTarget = new IntegTestDefineMethodTarget();

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
    public void testInjectableDelegationClassesLoaded() throws ClassNotFoundException {
        verifyClassLoaded("software.amazon.disco.instrumentation.preprocess.IntegTestDelegation");
        verifyClassLoaded("software.amazon.disco.instrumentation.preprocess.IntegTestDelegationNoSuperCall");
    }

    @Test
    public void testDefineMethod_withFieldAccessor() throws Exception {
        Method getter = instrumentedTarget.getClass().getDeclaredMethod("getPrivateField");
        Method setter = instrumentedTarget.getClass().getDeclaredMethod("setPrivateField", String.class);

        assertEquals("PRIVATE_FIELD", getter.invoke(instrumentedTarget));

        setter.invoke(instrumentedTarget, "NEW_VALUE");
        assertEquals("NEW_VALUE", getter.invoke(instrumentedTarget));
    }

    @Test
    public void testDefineMethod_withMethodDelegation() throws Exception {
        Object result = instrumentedTarget.getClass().getDeclaredMethod("invokeMethodDelegation", Object.class)
            .invoke(instrumentedTarget,ARGUMENT);

        verifyInstrumentedMethodReturnObject(
            result,
            instrumentedTarget.getClass().getName(),
            listener,
            true
        );
    }
}
