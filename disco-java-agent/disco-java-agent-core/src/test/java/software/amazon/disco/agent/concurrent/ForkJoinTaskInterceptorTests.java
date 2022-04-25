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
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.InterceptionInstaller;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashSet;
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
    public void testForkMethodMatcherMatches() throws Exception {
        Assert.assertTrue(ForkJoinTaskInterceptor.createForkMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(ForkJoinTask.class.getDeclaredMethod("fork"))
        ));
    }

    @Test
    public void testForkAdviceSafe() {
        //reflection will fail in here, when agent not present, but is handled
        ForkJoinTaskInterceptor.ForkAdvice.onMethodEnter(Mockito.mock(ForkJoinTask.class));
    }

    @Test
    public void testInstall() {
        TestUtils.testInstallableCanBeInstalled(new ForkJoinTaskInterceptor());
    }

    @Test
    public void testInstallationInstallerAppliesThenRemoves() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Mockito.when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});
        Installable fjtInterceptor = new ForkJoinTaskInterceptor();
        interceptionInstaller.install(instrumentation, new HashSet<>(Collections.singleton(fjtInterceptor)), new AgentConfig(null), ElementMatchers.none());
        ArgumentCaptor<ClassFileTransformer> transformerCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        Mockito.verify(instrumentation).addTransformer(transformerCaptor.capture());
        Mockito.verify(instrumentation).removeTransformer(Mockito.eq(transformerCaptor.getValue()));
    }
}
