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

package software.amazon.disco.agent.reflect;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReflectiveCallTests {
    private static final String TEST_CLASS_NAME = "software.amazon.disco.agent.reflect.ReflectiveCallTests$TestClass";

    @Before
    public void before() {
        // set the value of this field to true for testing purposes. Otherwise, all reflective calls will return the default value or null.
        ReflectiveCall.setDiscoTemplateClassFound(true);
    }

    @After
    public void after() {
        ReflectiveCall.resetCache();
    }

    @Test
    public void testAgentIsNotPresent() {
        ReflectiveCall.resetCache();

        // assert that this value is null which will allow a reflective call to be made to check whether 'DiscoAgentTemplate' can be resolved or not.
        assertNull(ReflectiveCall.getDiscoTemplateClassFound());

        assertFalse(ReflectiveCall.isAgentPresent());

        // after 'DiscoAgentTemplate' fails to be resolved due to the absence of the Disco agent, the static Boolean 'discoTemplateClassFound' will be
        // set to false and prevent further reflective calls from being made.
        assertFalse(ReflectiveCall.getDiscoTemplateClassFound());
    }

    @Test
    public void testResetCacheSetsDiscoTemplateClassFoundToNullAndClearsTypeCache(){
        ReflectiveCall.setDiscoTemplateClassFound(true);
        ReflectiveCall.getCachedTypes().put(String.class.getName(), String.class);

        assertTrue(ReflectiveCall.isAgentPresent());
        assertEquals(1, ReflectiveCall.getCachedTypes().size());

        ReflectiveCall.resetCache();

        assertNull(ReflectiveCall.getDiscoTemplateClassFound());
        assertFalse(ReflectiveCall.isAgentPresent());
        assertTrue(ReflectiveCall.getCachedTypes().isEmpty());
    }

    @Test
    public void testSetUncaughtExceptionHandler() {
        UncaughtExceptionHandler handler = Mockito.mock(UncaughtExceptionHandler.class);
        ReflectiveCall.installUncaughtExceptionHandler(handler);
        assertEquals(handler, ReflectiveCall.uncaughtExceptionHandler);

        ReflectiveCall.installUncaughtExceptionHandler(null);
        assertNull(ReflectiveCall.uncaughtExceptionHandler);
    }

    @Test
    public void testBeanGetters() {
        ReflectiveCall call = ReflectiveCall.returning(Long.class)
            .ofClass(".Foo")
            .ofMethod("Bar")
            .withArgTypes(Integer.class);
        assertEquals(Long.class, call.getReturnType());
        assertEquals("software.amazon.disco.agent.Foo", call.getClassName());
        assertEquals("Bar", call.getMethodName());
        assertEquals(Integer.class, call.getArgTypes()[0]);
        assertEquals(1, call.getArgTypes().length);
    }

    @Test
    public void testToString() {
        ReflectiveCall call = ReflectiveCall.returning(Long.class)
            .ofClass(".package.Foo")
            .ofMethod("Bar")
            .withArgTypes(Integer.class, Boolean.class);
        assertEquals("java.lang.Long software.amazon.disco.agent.package.Foo::Bar(java.lang.Integer, java.lang.Boolean)", call.toString());
    }

    @Test
    public void testMethodFound() {
        ReflectiveCall reflectiveCall = ReflectiveCall.returningVoid()
            .ofClass(".reflect.ReflectiveCallTests")
            .ofMethod("testMethod");
        assertTrue(reflectiveCall.methodFound());
    }

    @Test
    public void testMethodFoundNotPerformReflectiveCallsWhenAgentAbsent() {
        ReflectiveCall.setDiscoTemplateClassFound(false);
        assertFalse(ReflectiveCall.isAgentPresent());

        ReflectiveCall reflectiveCall = ReflectiveCall.returningVoid()
            .ofClass(".reflect.ReflectiveCallTests")
            .ofMethod("testMethod");

        // this would've returned true and cached the test class if 'ReflectiveCall.isAgentPresent()' returned true, proving that no reflective calls will be made
        // when the check returns false.
        assertFalse(reflectiveCall.methodFound());
        assertTrue(ReflectiveCall.getCachedTypes().isEmpty());
    }

    @Test
    public void testReflectiveCallDispatchesException() {
        AtomicReference<Throwable> caught = new AtomicReference<>(null);
        UncaughtExceptionHandler.install((call, args, thrown) -> {
            caught.set(thrown);
        });

        ReflectiveCall.returningVoid()
            .ofClass(".reflect.ReflectiveCallTests")
            .ofMethod("testMethod")
            .call();

        assertTrue(caught.get() instanceof RuntimeException);
        assertEquals("Test", caught.get().getMessage());
    }

    @Test
    public void testIllegalAccessIsSafe() {
        Object result = ReflectiveCall.returning(Object.class)
            .ofClass(".reflect.ReflectiveCallTests")
            .ofMethod("privateTestMethod")
            .call();
        assertNull(result);
    }

    @Test
    public void testRetrieveCachedTypeReturnsValidType() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class loadedType = ReflectiveCall.retrieveCachedType(TEST_CLASS_NAME);
        assertNotNull(loadedType);

        String result = (String) loadedType.getDeclaredMethod("foo").invoke(null);
        assertEquals("foo", result);

        assertEquals(1, ReflectiveCall.getCachedTypes().size());
        assertNotNull(ReflectiveCall.getCachedTypes().get(TEST_CLASS_NAME));
        assertEquals(TestClass.class, ReflectiveCall.getCachedTypes().get(TEST_CLASS_NAME));
    }

    @Test
    public void testRetrieveCachedTypeNotReloadCachedType() {
        // populate the map with an mismatched entry. The value associated to this entry would've been overridden by the correct one
        // if 'retrieveCachedType()' ignored the cached value and reloaded the class anyway via Reflection.
        ReflectiveCall.getCachedTypes().put(TEST_CLASS_NAME, this.getClass());

        Class loadedType = ReflectiveCall.retrieveCachedType(TEST_CLASS_NAME);

        // verify that the returned value and the cached value haven't been modified.
        assertNotEquals(TestClass.class, loadedType);
        assertNotEquals(TestClass.class, ReflectiveCall.getCachedTypes().get(TEST_CLASS_NAME));
    }

    @Test
    public void testRetrieveCachedTypeWithInvalidClassName() {
        Class loadedType = ReflectiveCall.retrieveCachedType("non.existing.class");
        assertNull(loadedType);
        assertTrue(ReflectiveCall.getCachedTypes().isEmpty());
    }

    //methods to call reflectively, just for the test
    public static void testMethod() {
        throw new RuntimeException("Test");
    }

    private static Object privateTestMethod() {
        return new Object();
    }

    static class TestClass {
        public static String foo() {
            return "foo";
        }
    }
}
