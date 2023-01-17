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

package software.amazon.disco.agent.concurrent;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolInterceptorTests {
    private static class CustomRunnable implements Runnable {
        @Override
        public void run() {
        }
    }

    @Test
    public void testThatTypeMatcherMatches() {
        Assert.assertTrue(ThreadPoolInterceptor.createTypeMatcher()
            .matches(new TypeDescription.ForLoadedType(ThreadPoolExecutor.class)));
    }

    @Test
    public void testThatBeforeExecuteMethodMatches() throws NoSuchMethodException {
        Method m = ThreadPoolExecutor.class.getDeclaredMethod("beforeExecute", Thread.class, Runnable.class);
        Assert.assertTrue(ThreadPoolInterceptor.createBeforeExecuteMethodMatcher()
            .matches(new MethodDescription.ForLoadedMethod(m)));
    }

    @Test
    public void testThatAfterExecuteMethodMatches() throws NoSuchMethodException {
        Method m = ThreadPoolExecutor.class.getDeclaredMethod("afterExecute", Runnable.class, Throwable.class);
        Assert.assertTrue(ThreadPoolInterceptor.createAfterExecuteMethodMatcher()
            .matches(new MethodDescription.ForLoadedMethod(m)));
    }

    @Test
    public void testThatRemoveMethodMatches() throws NoSuchMethodException {
        Method m = ThreadPoolExecutor.class.getDeclaredMethod("remove", Runnable.class);
        Assert.assertTrue(ThreadPoolInterceptor.createRemoveMethodMatcher()
                .matches(new MethodDescription.ForLoadedMethod(m)));
    }

    @Test
    public void testThatShutdownNowMethodMatches() throws NoSuchMethodException {
        Method m = ThreadPoolExecutor.class.getDeclaredMethod("shutdownNow");
        Assert.assertTrue(ThreadPoolInterceptor.createShutdownNowMethodMatcher()
                .matches(new MethodDescription.ForLoadedMethod(m)));
    }

    @Test
    public void testThatAdviceRemovesDecoration() {
        Runnable decoratedRunnable = DecoratedRunnable.maybeCreate(new CustomRunnable());
        Assert.assertTrue(ThreadPoolInterceptor.unDecorate(decoratedRunnable) instanceof CustomRunnable);
    }

    @Test
    public void testThatAdviceIgnoresUndecoratedRunnable() {
        Assert.assertTrue(ThreadPoolInterceptor.unDecorate(new CustomRunnable()) instanceof CustomRunnable);
    }

    @Test
    public void testInstall() {
        TestUtils.testInstallableCanBeInstalled(new ThreadPoolInterceptor());
    }
}
