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

import org.junit.Test;
import software.amazon.disco.agent.coroutines.BuildersKtCoroutineInterceptor;
import software.amazon.disco.agent.coroutines.CoroutinesSupport;
import software.amazon.disco.agent.coroutines.FutureKtCoroutineInterceptor;
import software.amazon.disco.agent.interception.Installable;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CoroutinesSupportTest  {
    @Test
    public void testCoroutinesSupport() {
        List<Installable> pkg = (List<Installable>) (new CoroutinesSupport().get());
        assertEquals(2, pkg.size());
        assertTrue(pkg.get(0) instanceof BuildersKtCoroutineInterceptor);
        assertTrue(pkg.get(1) instanceof FutureKtCoroutineInterceptor);
    }
}