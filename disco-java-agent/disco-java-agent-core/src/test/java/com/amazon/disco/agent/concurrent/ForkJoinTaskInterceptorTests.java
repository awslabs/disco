package com.amazon.disco.agent.concurrent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinTask;

public class ForkJoinTaskInterceptorTests {
    @Test
    public void testForkJoinTaskTypeMatcherMatches() throws Exception {
        Assert.assertTrue(ForkJoinTaskInterceptor.createForkJoinTaskTypeMatcher().matches(
                new TypeDescription.ForLoadedType(ForkJoinTask.class)
        ));
    }

    @Test
    public void testForkJoinTaskTypeMatcherNotMatchesSubclass() throws Exception {
        Assert.assertFalse(ForkJoinTaskInterceptor.createForkJoinTaskTypeMatcher().matches(
                new TypeDescription.ForLoadedType(CountedCompleter.class)
        ));
    }

    @Test
    public void testForkJoinTaskSubclassTypeMatcherMatches() throws Exception {
        Assert.assertTrue(ForkJoinTaskInterceptor.createForkJoinTaskSubclassTypeMatcher().matches(
                new TypeDescription.ForLoadedType(CountedCompleter.class)
        ));
    }

    @Test
    public void testForkMethodMatcherMatches() throws Exception {
        Assert.assertTrue(ForkJoinTaskInterceptor.createForkMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(ForkJoinTask.class.getDeclaredMethod("fork"))
        ));
    }

    @Test
    public void testExecMethodMatcherMatches() throws Exception {
        Assert.assertTrue(ForkJoinTaskInterceptor.createExecMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(CountedCompleter.class.getDeclaredMethod("exec"))
        ));
    }

    @Test
    public void testExecMethodMatcherNotMatchesAbstract() throws Exception {
        Assert.assertFalse(ForkJoinTaskInterceptor.createExecMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(ForkJoinTask.class.getDeclaredMethod("exec"))
        ));
    }

    @Test
    public void testForkAdviceSafe() {
        //reflection will fail in here, when agent not present, but is handled
        ForkJoinTaskInterceptor.ForkAdvice.onMethodEnter(Mockito.mock(ForkJoinTask.class));
    }

    @Test
    public void testExecAdviceEnterSafe() {
        ForkJoinTaskInterceptor.ExecAdvice.onMethodEnter(Mockito.mock(ForkJoinTask.class));
    }

    @Test
    public void testExecAdviceExitSafe() {
        ForkJoinTaskInterceptor.ExecAdvice.onMethodExit(Mockito.mock(ForkJoinTask.class));
    }

    @Test
    public void testInstall() {
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        AgentBuilder.Identified.Extendable extendable = Mockito.mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = Mockito.mock(AgentBuilder.Identified.Narrowable.class);
        Mockito.when(agentBuilder.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        Mockito.when(narrowable.transform(Mockito.any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        Mockito.when(extendable.type(Mockito.any(ElementMatcher.class))).thenReturn(narrowable);
        AgentBuilder result = new ForkJoinTaskInterceptor().install(agentBuilder);
        Assert.assertEquals(extendable, result);
    }
}
