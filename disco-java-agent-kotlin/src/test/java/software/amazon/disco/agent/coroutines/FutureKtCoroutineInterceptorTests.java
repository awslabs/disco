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

package software.amazon.disco.agent.coroutines;

import kotlinx.coroutines.future.FutureKt;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.concurrent.TransactionContext;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.disco.agent.coroutines.MatcherUtil.classMatches;
import static software.amazon.disco.agent.coroutines.MatcherUtil.methodMatches;

public class FutureKtCoroutineInterceptorTests {
    private FutureKtCoroutineInterceptor interceptor;

    @Before
    public void before() {
        interceptor = new FutureKtCoroutineInterceptor();
        TransactionContext.create();
    }

    @After
    public void after() {
        TransactionContext.destroy();
    }

    @Test
    public void testInstall() {
        AgentBuilder agentBuilder = mock(AgentBuilder.class);
        AgentBuilder.Identified.Extendable extendable = mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = mock(AgentBuilder.Identified.Narrowable.class);

        when(agentBuilder.with(any(ByteBuddy.class))).thenReturn(extendable);
        when(extendable.type(any(ElementMatcher.class))).thenReturn(narrowable);
        when(narrowable.transform(any(AgentBuilder.Transformer.class))).thenReturn(extendable);

        AgentBuilder result = interceptor.install(agentBuilder);

        assertSame(extendable, result);
    }

    @Test
    public void testClassTypeMatchers() {
        classMatches(FutureKt.class, FutureKtCoroutineInterceptor.buildFutureKtClassTypeMatcher());
    }

    @Test
    public void testCreateFutureMethodMatcher() {
        methodMatches("future", FutureKt.class, interceptor.createFutureMethodMatcher());
        methodMatches("future$default", FutureKt.class, interceptor.createFutureMethodMatcher());
    }
    
    @Test(expected = AssertionError.class)
    public void testClassMatcherFails() {
        classMatches(String.class, FutureKtCoroutineInterceptor.buildFutureKtClassTypeMatcher());
    }
}