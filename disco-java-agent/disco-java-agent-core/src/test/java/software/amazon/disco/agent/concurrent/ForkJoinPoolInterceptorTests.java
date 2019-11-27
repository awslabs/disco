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

import software.amazon.disco.agent.concurrent.decorate.DecoratedCallable;
import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class ForkJoinPoolInterceptorTests {
    @Test
    public void testForkJoinPoolClassMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createTypeMatcher()
            .matches(new TypeDescription.ForLoadedType(ForkJoinPool.class)));
    }

    @Test
    public void testSubmitRunnableMethodMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createRunnableMethodsMatcher()
            .matches(new MethodDescription.ForLoadedMethod(ForkJoinPool.class.getDeclaredMethod("submit", Runnable.class))));
    }

    @Test
    public void testExecuteRunnableMethodMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createRunnableMethodsMatcher()
                .matches(new MethodDescription.ForLoadedMethod(ForkJoinPool.class.getDeclaredMethod("execute", Runnable.class))));
    }

    @Test
    public void testSubmitCallableMethodMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createCallableMethodsMatcher()
                .matches(new MethodDescription.ForLoadedMethod(ForkJoinPool.class.getDeclaredMethod("submit", Callable.class))));
    }

    @Test
    public void testInvokeAllCallablesMethodMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createCallableCollectionMethodsMatcher()
                .matches(new MethodDescription.ForLoadedMethod(ForkJoinPool.class.getDeclaredMethod("invokeAll", Collection.class))));
    }

    @Test
    public void testInvokeForkJoinTaskMethodMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createForkJoinTaskMethodsMatcher()
                .matches(new MethodDescription.ForLoadedMethod(ForkJoinPool.class.getDeclaredMethod("invoke", ForkJoinTask.class))));
    }

    @Test
    public void testExecuteForkJoinTaskMethodMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createForkJoinTaskMethodsMatcher()
                .matches(new MethodDescription.ForLoadedMethod(ForkJoinPool.class.getDeclaredMethod("execute", ForkJoinTask.class))));
    }

    @Test
    public void testSubmitForkJoinTaskMethodMatches() throws Exception {
        Assert.assertTrue(ForkJoinPoolInterceptor.createForkJoinTaskMethodsMatcher()
                .matches(new MethodDescription.ForLoadedMethod(ForkJoinPool.class.getDeclaredMethod("submit", ForkJoinTask.class))));
    }

    @Test
    public void testRunnableAdvice() {
        ForkJoinPoolInterceptor.RunnableMethodsAdvice.onMethodEnter(Mockito.mock(Runnable.class));
    }

    @Test
    public void testRunnableAdviceDecorates() {
        Runnable r = Mockito.mock(Runnable.class);
        Runnable d = ForkJoinPoolInterceptor.RunnableMethodsAdvice.methodEnter(r);
        Assert.assertTrue(d instanceof DecoratedRunnable);
    }

    @Test
    public void testCallableAdvice() {
        ForkJoinPoolInterceptor.CallableMethodsAdvice.onMethodEnter(Mockito.mock(Callable.class));
    }

    @Test
    public void testCallableAdviceDecorates() {
        Callable c = Mockito.mock(Callable.class);
        Callable d = ForkJoinPoolInterceptor.CallableMethodsAdvice.methodEnter(c);
        Assert.assertTrue(d instanceof DecoratedCallable);
    }

    @Test
    public void testCallableCollectionAdvice() {
        Callable c = Mockito.mock(Callable.class);
        List<Callable> l = Arrays.asList(c);
        ForkJoinPoolInterceptor.CallableCollectionMethodsAdvice.onMethodEnter(l);
    }

    @Test
    public void testCallableCollectionAdviceDecorates() {
        Callable c = Mockito.mock(Callable.class);
        List<Callable> l = Arrays.asList(c);
        Collection<Callable> collection = ForkJoinPoolInterceptor.CallableCollectionMethodsAdvice.methodEnter(l);
        Assert.assertTrue(collection.iterator().next() instanceof DecoratedCallable);
    }

    @Test
    public void testForkJoinTaskAdviceSafe() {
        //reflection will fail in here, when agent not present, but is handled
        ForkJoinPoolInterceptor.ForkJoinTaskMethodsAdvice.onMethodEnter(Mockito.mock(ForkJoinTask.class));
    }

    @Test
    public void testInstall() {
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        AgentBuilder.Identified.Extendable extendable = Mockito.mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = Mockito.mock(AgentBuilder.Identified.Narrowable.class);
        AgentBuilder.Ignored ignored = Mockito.mock(AgentBuilder.Ignored.class);
        Mockito.when(agentBuilder.ignore(Mockito.any(ElementMatcher.class))).thenReturn(ignored);
        Mockito.when(ignored.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        Mockito.when(narrowable.transform(Mockito.any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        AgentBuilder result = new ForkJoinPoolInterceptor().install(agentBuilder);
        Assert.assertEquals(extendable, result);
    }
}
