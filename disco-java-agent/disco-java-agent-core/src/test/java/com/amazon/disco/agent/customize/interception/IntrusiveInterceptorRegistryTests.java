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

package com.amazon.disco.agent.customize.interception;

import com.amazon.disco.agent.interception.IntrusiveInterceptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class IntrusiveInterceptorRegistryTests {
    @Before
    public void before() {
        IntrusiveInterceptorRegistry.uninstall();
    }

    @After
    public void after() {
        IntrusiveInterceptorRegistry.uninstall();
    }
    @Test
    public void testInstall() {
        IntrusiveInterceptor i = Mockito.mock(IntrusiveInterceptor.class);
        IntrusiveInterceptor old = IntrusiveInterceptorRegistry.install(i);
        Assert.assertEquals(old.getClass().getName(), "com.amazon.alphaone.agent.interception.IntrusiveInterceptorRegistry$DefaultIntrusiveInterceptor");
        old = IntrusiveInterceptorRegistry.uninstall();
        Assert.assertEquals(i, old);
    }
}
