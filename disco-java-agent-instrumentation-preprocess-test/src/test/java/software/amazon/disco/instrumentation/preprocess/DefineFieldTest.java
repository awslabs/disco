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

package software.amazon.disco.instrumentation.preprocess;

import org.junit.Test;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestDefineFieldTarget;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented the target class {@link IntegTestDefineFieldTarget} to have 3 additional fields of different modifiers.
 *
 * @see IntegTestDefineFieldInterceptor for the interceptor used.
 */
public class DefineFieldTest {
    private static Class INSTRUMENTED_CLASS = IntegTestDefineFieldTarget.class;

    @Test
    public void testDefinePrivateField() throws Exception {
        Field decoratedField = INSTRUMENTED_CLASS.getDeclaredField("privateField");

        assertEquals(Object.class, decoratedField.getType());
        assertEquals(Modifier.PRIVATE, decoratedField.getModifiers());
    }

    @Test
    public void testDefineProtectedField() throws NoSuchFieldException {
        Field decoratedField = INSTRUMENTED_CLASS.getDeclaredField("protectedField");

        assertEquals(String.class, decoratedField.getType());
        assertEquals(Modifier.PROTECTED, decoratedField.getModifiers());
    }

    @Test
    public void testDefinePublicField() throws NoSuchFieldException {
        Field decoratedField = INSTRUMENTED_CLASS.getDeclaredField("publicField");

        assertEquals(Map.class, decoratedField.getType());
        assertEquals(Modifier.PUBLIC, decoratedField.getModifiers());
    }
}
