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
import org.mockito.Mockito;
import software.amazon.disco.agent.concurrent.decorate.DecoratedForkJoinTask;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinTask;

public class ForkJoinTaskSubclassInterceptorTests {
    @Test
    public void testForkJoinTaskSubclassTypeMatcherMatches() throws Exception {
        Assert.assertTrue(ForkJoinTaskSubclassInterceptor.createForkJoinTaskSubclassTypeMatcher().matches(
                new TypeDescription.ForLoadedType(CountedCompleter.class)
        ));
    }

    @Test
    public void testExecMethodMatcherMatches() throws Exception {
        Assert.assertTrue(ForkJoinTaskSubclassInterceptor.createExecMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(CountedCompleter.class.getDeclaredMethod("exec"))
        ));
    }

    @Test
    public void testExecMethodMatcherNotMatchesAbstract() throws Exception {
        Assert.assertFalse(ForkJoinTaskSubclassInterceptor.createExecMethodMatcher().matches(
                new MethodDescription.ForLoadedMethod(ForkJoinTask.class.getDeclaredMethod("exec"))
        ));
    }

    @Test
    public void testExecAdviceEnterSafe() {
        ForkJoinTaskSubclassInterceptor.ExecAdvice.onMethodEnter(Mockito.mock(DecoratedForkJoinTask.class));
    }

    @Test
    public void testExecAdviceExitSafe() {
        ForkJoinTaskSubclassInterceptor.ExecAdvice.onMethodExit(Mockito.mock(DecoratedForkJoinTask.class));
    }

    @Test
    public void testInstall() {
        TestUtils.testInstallableCanBeInstalled(new ForkJoinTaskSubclassInterceptor());
    }
}
