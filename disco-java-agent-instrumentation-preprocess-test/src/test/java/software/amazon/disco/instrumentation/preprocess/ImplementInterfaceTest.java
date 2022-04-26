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
import software.amazon.disco.instrumentation.preprocess.source.IntegTestImplementInterfaceTarget;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestInvocable;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestListener;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Instrument the target class {@link IntegTestImplementInterfaceTarget} making it implement an interface which contains 2 default methods, one to be inherited as such
 * and one to be overridden via method delegation.
 *
 * @see IntegTestImplementInterfaceInterceptor for the interceptor used.
 */
public class ImplementInterfaceTest extends IntegtestBase{
    private final static String INTERFACE_NAME = "software.amazon.disco.instrumentation.preprocess.IntegTestImplementInterfaceInterceptor$IntegTestInterface";
    private static IntegTestInvocable transformed;
    private static IntegTestListener listener;

    @BeforeClass
    public static void beforeAll() {
        transformed = new IntegTestImplementInterfaceTarget();

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
    public void testTargetImplementInterface() {
        assertEquals(1, Arrays.asList(transformed.getClass().getInterfaces()).stream().filter(
            aClass -> aClass.getName().equals(INTERFACE_NAME)).count());
    }

    @Test
    public void testInjectableDelegationClassesLoaded() {
        verifyClassLoaded("software.amazon.disco.instrumentation.preprocess.IntegTestDelegation");
        verifyClassLoaded("software.amazon.disco.instrumentation.preprocess.IntegTestDelegationNoSuperCall");
    }

    @Test
    public void testTargetInheritsInterfaceMethodAndOverridesDefaultMethodViaMethodDelegation() throws Exception{
        String returned = (String) transformed.getClass()
            .getDeclaredMethod("getTargetTypeName")
            .invoke(transformed);

        assertEquals(transformed.getClass().getName(), returned);
        verifyInstrumentedMethodReturnObject(
            returned,
            transformed.getClass().getName(),
            listener,
            false
        );
    }

    @Test
    public void testTargetInheritsInterfaceMethodAndOverridesAbstractMethodViaAdvice() throws Exception{
        String returned = (String) transformed.getClass()
            .getDeclaredMethod("abstractMethod", String.class)
            .invoke(transformed, ARGUMENT);

        assertEquals(transformed.getClass().getName(), returned);
        verifyInstrumentedMethodReturnObject(
            returned,
            transformed.getClass().getName(),
            listener,
            true
        );
    }

    @Test
    public void testTargetInheritsInterfaceDefaultMethod() throws Exception{
        String returned = (String) transformed.getClass()
            .getMethod("getInterfaceName")
            .invoke(transformed);

        assertEquals(INTERFACE_NAME, returned);
    }
}
