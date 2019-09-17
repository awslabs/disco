package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ThreadInterceptorTests {
    @Test
    public void testThreadMatcherMatchesThread() {
        Assert.assertTrue(ThreadInterceptor.createThreadTypeMatcher()
            .matches(new TypeDescription.ForLoadedType(Thread.class)));
    }

    @Test
    public void testThreadMatcherNotMatchesThreadSubclass() {
        Assert.assertFalse(ThreadInterceptor.createThreadTypeMatcher()
            .matches(new TypeDescription.ForLoadedType(ThreadSubclass.class)));
    }

    @Test
    public void testStartMethodMatcherMatches() throws Exception {
        Assert.assertTrue(ThreadInterceptor.createStartMethodMatcher()
            .matches(new MethodDescription.ForLoadedMethod(Thread.class.getDeclaredMethod( "start"))));
    }

    @Test
    public void testStartAdviceSafe() {
        ThreadInterceptor.StartAdvice.onStartEnter(Mockito.mock(Runnable.class));
    }

    @Test
    public void testStartAdviceDecorates() {
        Runnable r = Mockito.mock(Runnable.class);
        Runnable d = ThreadInterceptor.StartAdvice.wrap(r);
        Assert.assertTrue(d instanceof DecoratedRunnable);
    }

    @Test
    public void testInstall() throws Exception {
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        AgentBuilder.Ignored ignored = Mockito.mock(AgentBuilder.Ignored.class);
        AgentBuilder.RedefinitionListenable.WithoutBatchStrategy withoutBatchStrategy = Mockito.mock(AgentBuilder.RedefinitionListenable.WithoutBatchStrategy.class);
        AgentBuilder.Identified.Narrowable narrowable = Mockito.mock(AgentBuilder.Identified.Narrowable.class);
        Mockito.when(agentBuilder.ignore(Mockito.any(ElementMatcher.class))).thenReturn(ignored);
        Mockito.when(ignored.with(Mockito.any(AgentBuilder.InitializationStrategy.class))).thenReturn(ignored);
        Mockito.when(ignored.with(Mockito.any(AgentBuilder.RedefinitionStrategy.class))).thenReturn(withoutBatchStrategy);
        Mockito.when(withoutBatchStrategy.with(Mockito.any(AgentBuilder.TypeStrategy.class))).thenReturn(ignored);
        Mockito.when(ignored.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        new ThreadInterceptor().install(agentBuilder);
    }

    static class ThreadSubclass extends Thread {
    }
}
