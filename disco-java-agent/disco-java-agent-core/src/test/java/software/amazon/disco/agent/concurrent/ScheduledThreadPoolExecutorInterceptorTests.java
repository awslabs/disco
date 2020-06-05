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

package software.amazon.disco.agent.concurrent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnableScheduledFuture;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ScheduledThreadPoolExecutorInterceptorTests {

    private static class TestSubclass extends ScheduledThreadPoolExecutor {
        public TestSubclass() {
            super(1);
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
            return super.decorateTask(runnable, task);
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
            return super.decorateTask(callable, task);
        }
    }

    @Test
    public void testThatTypeMatcherMatches() {
        Assert.assertTrue(ScheduledThreadPoolExecutorInterceptor.createTypeMatcher()
            .matches(new TypeDescription.ForLoadedType(ScheduledThreadPoolExecutor.class)));
    }

    @Test
    public void testThatTypeMatcherMatchesSubclasses() {
        Assert.assertTrue(ScheduledThreadPoolExecutorInterceptor.createTypeMatcher()
                .matches(new TypeDescription.ForLoadedType(TestSubclass.class)));
    }

    @Test
    public void testThatMethodMatchesWithCallable() throws NoSuchMethodException {
        Method m = ScheduledThreadPoolExecutor.class.getDeclaredMethod("decorateTask", Callable.class,
                RunnableScheduledFuture.class);

        Assert.assertTrue(ScheduledThreadPoolExecutorInterceptor.createMethodMatcher()
            .matches(new MethodDescription.ForLoadedMethod(m)));
    }

    @Test
    public void testThatMethodMatchesWithRunnable() throws NoSuchMethodException {
        Method m = ScheduledThreadPoolExecutor.class.getDeclaredMethod("decorateTask", Runnable.class,
                RunnableScheduledFuture.class);

        Assert.assertTrue(ScheduledThreadPoolExecutorInterceptor.createMethodMatcher()
                .matches(new MethodDescription.ForLoadedMethod(m)));
    }

    @Test
    public void testThatMethodMatchesWithCallableOnSubclass() throws NoSuchMethodException {
        Method m = TestSubclass.class.getDeclaredMethod("decorateTask", Callable.class,
                RunnableScheduledFuture.class);

        Assert.assertTrue(ScheduledThreadPoolExecutorInterceptor.createMethodMatcher()
                .matches(new MethodDescription.ForLoadedMethod(m)));
    }

    @Test
    public void testThatMethodMatchesWithRunnableOnSubclass() throws NoSuchMethodException {
        Method m = TestSubclass.class.getDeclaredMethod("decorateTask", Runnable.class,
                RunnableScheduledFuture.class);

        Assert.assertTrue(ScheduledThreadPoolExecutorInterceptor.createMethodMatcher()
                .matches(new MethodDescription.ForLoadedMethod(m)));
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
        AgentBuilder result = new ScheduledThreadPoolExecutorInterceptor().install(agentBuilder);
        Assert.assertEquals(extendable, result);
    }

    @Test
    public void testThatAdviceDecoratesTask() {
        RunnableScheduledFuture scheduledFuture = Mockito.mock(RunnableScheduledFuture.class);
        RunnableScheduledFuture result = ScheduledThreadPoolExecutorInterceptor.DecorateTaskAdvice.decorate(scheduledFuture);
        Assert.assertTrue(result instanceof DecoratedRunnableScheduledFuture);
    }

}
