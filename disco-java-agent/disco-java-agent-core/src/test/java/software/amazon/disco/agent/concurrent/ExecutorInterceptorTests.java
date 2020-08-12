
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

package software.amazon.disco.agent.concurrent;

import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ExecutorInterceptorTests {
    ElementMatcher<? super TypeDescription> typeMatcher = ExecutorInterceptor.createTypeMatcher();
    ElementMatcher<? super MethodDescription> methodMatcher = ExecutorInterceptor.createMethodMatcher();

    @Test
    public void testTypeMatcherMatchesInterface() {
        Assert.assertTrue(typeMatcher.matches(new TypeDescription.ForLoadedType(Executor.class)));
    }

    @Test
    public void testTypeMatcherMatchesAbstractBase() {
        Assert.assertTrue(typeMatcher.matches(new TypeDescription.ForLoadedType(AbstractExecutorService.class)));
    }

    @Test
    public void testTypeMatcherMatchesConcrete() {
        Assert.assertTrue(typeMatcher.matches(new TypeDescription.ForLoadedType(ForkJoinPool.class)));
    }

    @Test
    public void testTypeMatcherDoesNotMatchScheduledThreadPoolExecutor() {
        Assert.assertFalse(typeMatcher.matches(new TypeDescription.ForLoadedType(ScheduledThreadPoolExecutor.class)));
    }

    @Test
    public void testMethodMatcherNotMatchesInterface() throws Exception {
        Method execute = Executor.class.getDeclaredMethod("execute", Runnable.class);
        Assert.assertFalse(methodMatcher.matches(new MethodDescription.ForLoadedMethod(execute)));
    }

    @Test
    public void testMethodMatcherMatchesConcrete() throws Exception {
        Method execute = ForkJoinPool.class.getDeclaredMethod("execute", Runnable.class);
        Assert.assertTrue(methodMatcher.matches(new MethodDescription.ForLoadedMethod(execute)));
    }

    @Test
    public void testInstall() {
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        AgentBuilder.RedefinitionListenable.WithoutBatchStrategy withoutBatchStrategy = Mockito.mock(AgentBuilder.RedefinitionListenable.WithoutBatchStrategy.class);
        AgentBuilder.Identified.Extendable extendable = Mockito.mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = Mockito.mock(AgentBuilder.Identified.Narrowable.class);
        Mockito.when(agentBuilder.with(Mockito.any(AgentBuilder.InitializationStrategy.class))).thenReturn(agentBuilder);
        Mockito.when(agentBuilder.with(Mockito.any(AgentBuilder.RedefinitionStrategy.class))).thenReturn(withoutBatchStrategy);
        Mockito.when(withoutBatchStrategy.with(Mockito.any(AgentBuilder.TypeStrategy.class))).thenReturn(agentBuilder);
        Mockito.when(agentBuilder.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        Mockito.when(narrowable.transform(Mockito.any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        AgentBuilder result = new ExecutorInterceptor().install(agentBuilder);
        Assert.assertEquals(extendable, result);
    }

    @Test
    public void testExecuteAdvice() {
        ExecutorInterceptor.ExecuteAdvice.onMethodEnter(Mockito.mock(Runnable.class));
        ExecutorInterceptor.ExecuteAdvice.onMethodExit(); //ensure interception counter is decremented
    }

    @Test
    public void testCaptureThrowable() {
        ExecutorInterceptor.ExecuteAdvice.captureThrowableForDebugging(new RuntimeException());
    }

    @Test
    public void testExecuteAdviceDecorates() {
        Runnable r = Mockito.mock(Runnable.class);
        Runnable d = ExecutorInterceptor.ExecuteAdvice.methodEnter(r);
        Assert.assertTrue(d instanceof DecoratedRunnable);
        ExecutorInterceptor.ExecuteAdvice.onMethodExit(); //ensure interception counter is decremented
    }
}

