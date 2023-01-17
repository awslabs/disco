/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.InterceptionInstaller;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduledFutureTaskInterceptorTests {
    private static ScheduledThreadPoolExecutor testExecutor;

    @BeforeClass
    public static void beforeClass() {
        testExecutor = new ScheduledThreadPoolExecutor(1);
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        testExecutor.shutdownNow();
        testExecutor.awaitTermination(1, TimeUnit.DAYS);
    }

    @Test
    public void testScheduledFutureTaskTypeMatcherMatches() throws Exception {
        Assert.assertTrue(ScheduledFutureTaskInterceptor.createScheduledFutureTaskTypeMatcher().matches(
                new TypeDescription.ForLoadedType(ScheduledFutureTaskInterceptor.getTargetClass())
        ));
    }

    @Test
    public void testRunMethodMatcherMatches() throws Exception {
        Assert.assertTrue(ScheduledFutureTaskInterceptor.createRunMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(ScheduledFutureTaskInterceptor.getTargetClass().getMethod("run"))
        ));
    }

    @Test
    public void testConstructorAdviceMethodExitSafe() {
        ScheduledFutureTaskInterceptor.ConstructorAdvice.onMethodExit(getTestFuture());
    }

    @Test
    public void testRunAdviceMethodEnterSafe() {
        ScheduledFutureTaskInterceptor.RunAdvice.onMethodEnter(getTestFuture());
    }

    @Test
    public void testRunAdviceMethodExitSafe() {
        ScheduledFutureTaskInterceptor.RunAdvice.onMethodExit(getTestFuture());
    }

    @Test
    public void testInstall() {
        TestUtils.testInstallableCanBeInstalled(new ScheduledFutureTaskInterceptor());
    }

    @Test
    public void testInstallationInstallerAppliesThenRemoves() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Mockito.when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});
        Installable sftInterceptor = new ScheduledFutureTaskInterceptor();
        interceptionInstaller.install(instrumentation, new HashSet<>(Collections.singleton(sftInterceptor)),
                new AgentConfig(null), ElementMatchers.none());
        ArgumentCaptor<ClassFileTransformer> transformerCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        Mockito.verify(instrumentation).addTransformer(transformerCaptor.capture());
        Mockito.verify(instrumentation).removeTransformer(Mockito.eq(transformerCaptor.getValue()));
    }

    private Future<?> getTestFuture() {
        return testExecutor.schedule(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "Hello!";
            }
        }, 1, TimeUnit.DAYS); // This delay is meant to never expire while a test case runs
    }
}
