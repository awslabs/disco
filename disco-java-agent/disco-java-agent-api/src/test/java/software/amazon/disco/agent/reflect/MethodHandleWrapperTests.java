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
package software.amazon.disco.agent.reflect;

import org.junit.Assert;
import org.junit.Test;

public class MethodHandleWrapperTests {

    @Test
    public void testMethodFound() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        MethodHandleWrapper handler = new MethodHandleWrapper(Object.class.getCanonicalName(),
                classLoader,
                "toString",
                String.class);
        Assert.assertTrue(handler.isHandleLoaded());
    }

    @Test
    public void testClassFoundMethodNotFound() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        MethodHandleWrapper handler = new MethodHandleWrapper(Object.class.getCanonicalName(),
                classLoader,
                "noMethod",
                Integer.class);
        Assert.assertFalse(handler.isHandleLoaded());
    }

    @Test
    public void testClassNotFound() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        MethodHandleWrapper handler = new MethodHandleWrapper("com.FakeClass",
                classLoader,
                "noMethod",
                Integer.class);
        Assert.assertFalse(handler.isHandleLoaded());
    }
}
