
package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
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
    }
}

