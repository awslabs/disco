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

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.agent.concurrent.preprocess.DiscoRunnableDecorator;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.InterceptionInstaller;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.BiFunction;

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
            .matches(new MethodDescription.ForLoadedMethod(Thread.class.getDeclaredMethod("start"))));
    }

    @Test
    public void testStartAdviceSafe() {
        ThreadInterceptor.StartAdvice.onStartEnter(Mockito.mock(Runnable.class));
    }

    @Test
    public void testStartAdviceInvokesDiscoRunnableDecorator() {
        TestRunnableDecorateFunction testRunnableDecorateFunction = new TestRunnableDecorateFunction();
        DiscoRunnableDecorator.setDecorateFunction(testRunnableDecorateFunction);

        Assert.assertFalse(testRunnableDecorateFunction.decorateFunctionCalled);
        Assert.assertFalse(testRunnableDecorateFunction.removeTX);
        Assert.assertNull(testRunnableDecorateFunction.target);

        Runnable runnable = Mockito.mock(Runnable.class);

        ThreadInterceptor.StartAdvice.onStartEnter(runnable);

        Assert.assertTrue(testRunnableDecorateFunction.decorateFunctionCalled);
        Assert.assertTrue(testRunnableDecorateFunction.removeTX);
        Assert.assertSame(runnable, testRunnableDecorateFunction.target);
    }

    @Test
    public void testInstall() {
        TestUtils.testInstallableCanBeInstalled(new ThreadInterceptor());
    }

    @Test
    public void testInstallationInstallerAppliesThenRemoves() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Mockito.when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});
        Installable threadInterceptor = new ThreadInterceptor();
        interceptionInstaller.install(instrumentation, new HashSet<>(Collections.singleton(threadInterceptor)), new AgentConfig(null), ElementMatchers.none());
        ArgumentCaptor<ClassFileTransformer> transformerCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        Mockito.verify(instrumentation).addTransformer(transformerCaptor.capture());
        Mockito.verify(instrumentation).removeTransformer(Mockito.eq(transformerCaptor.getValue()));
    }

    static class ThreadSubclass extends Thread {
    }

    static class TestRunnableDecorateFunction implements BiFunction<Runnable, Boolean, Runnable>{
        public boolean removeTX;
        public boolean decorateFunctionCalled;
        public Runnable target;

        @Override
        public Runnable apply(Runnable runnable, Boolean aBoolean) {
            removeTX = aBoolean;
            decorateFunctionCalled = true;
            target = runnable;

            return runnable;
        }
    }
}
