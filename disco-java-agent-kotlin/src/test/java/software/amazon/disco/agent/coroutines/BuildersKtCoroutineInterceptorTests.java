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

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.ThreadContextElement;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.concurrent.TransactionContext;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.disco.agent.coroutines.MatcherUtil.classMatches;
import static software.amazon.disco.agent.coroutines.MatcherUtil.methodMatches;

public class BuildersKtCoroutineInterceptorTests {
    private BuildersKtCoroutineInterceptor interceptor;

    @Before
    public void before() {
        interceptor = new BuildersKtCoroutineInterceptor();
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

        when(agentBuilder.type(any(ElementMatcher.class))).thenReturn(narrowable);
        when(narrowable.transform(any(AgentBuilder.Transformer.class))).thenReturn(extendable);

        AgentBuilder result = interceptor.install(agentBuilder);

        assertSame(extendable, result);
    }

    @Test
    public void testClassTypeMatchers() {
        classMatches(BuildersKt.class, BuildersKtCoroutineInterceptor.buildBuilderKtClassTypeMatcher());
    }

    @Test
    public void testCreateAsyncMethodMatcher() {
        methodMatches("async", BuildersKt.class, interceptor.createAsyncMethodMatcher());
        methodMatches("async$default", BuildersKt.class, interceptor.createAsyncMethodMatcher());
    }

    @Test
    public void testCreateRunBlockingMethodMatcher() {
        methodMatches("runBlocking", BuildersKt.class, interceptor.createRunBlockingMethodMatcher());
        methodMatches("runBlocking$default", BuildersKt.class, interceptor.createRunBlockingMethodMatcher());
    }

    @Test
    public void testCreateLaunchMethodMatcher() {
        methodMatches("launch", BuildersKt.class, interceptor.createLaunchMethodMatcher());
        methodMatches("launch$default", BuildersKt.class, interceptor.createLaunchMethodMatcher());
    }

    @Test(expected = AssertionError.class)
    public void testClassMatcherFails() {
        classMatches(String.class, BuildersKtCoroutineInterceptor.buildBuilderKtClassTypeMatcher());
    }

    @Test
    public void testAsyncAdviceEnter() {
        CoroutineContext coroutineContext = mock(CoroutineContext.class);

        BuildersKtCoroutineInterceptor.AsyncAdvice.enter(coroutineContext);

        verify(coroutineContext).plus(any(ThreadContextElement.class));
        verifyNoMoreInteractions(coroutineContext);
    }

    @Test
    public void testLaunchAdviceEnter() {
        CoroutineContext coroutineContext = mock(CoroutineContext.class);

        BuildersKtCoroutineInterceptor.LaunchAdvice.enter(coroutineContext);

        verify(coroutineContext).plus(any(ThreadContextElement.class));
        verifyNoMoreInteractions(coroutineContext);
    }

    @Test
    public void testRunBlockingAdviceEnter() {
        CoroutineContext coroutineContext = mock(CoroutineContext.class);

        BuildersKtCoroutineInterceptor.RunBlockingAdvice.enter(coroutineContext);

        verify(coroutineContext).plus(any(ThreadContextElement.class));
        verifyNoMoreInteractions(coroutineContext);
    }
}