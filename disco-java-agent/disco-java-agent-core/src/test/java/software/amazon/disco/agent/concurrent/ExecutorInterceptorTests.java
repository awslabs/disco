
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
        TestUtils.testInstallableCanBeInstalled(new ExecutorInterceptor());
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

