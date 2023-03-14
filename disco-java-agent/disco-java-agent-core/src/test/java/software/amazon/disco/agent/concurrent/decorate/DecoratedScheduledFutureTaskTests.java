/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.concurrent.decorate;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

public class DecoratedScheduledFutureTaskTests {
    @Test
    public void testCreate() {
        DecoratedScheduledFutureTask decoratedSft = DecoratedScheduledFutureTask.create();
        Assert.assertNotNull(decoratedSft);
    }

    @Test
    public void testMethodNames() {
        Method[] methods = DecoratedScheduledFutureTask.Accessor.class.getDeclaredMethods();
        Assert.assertEquals(2, methods.length);
        String[] names = new String[] {methods[0].getName(), methods[1].getName()};
        Arrays.sort(names);
        Assert.assertEquals(DecoratedScheduledFutureTask.Accessor.GET_DISCO_DECORATION_METHOD_NAME, names[0]);
        Assert.assertEquals(DecoratedScheduledFutureTask.Accessor.SET_DISCO_DECORATION_METHOD_NAME, names[1]);
    }
}