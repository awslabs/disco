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

package com.amazon.disco.agent.interception;

import com.amazon.disco.agent.config.AgentConfig;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class InterceptionInstallerTests {
    @Test
    public void testIgnoreMatcherMatchesJavaInternals() throws Exception {
        //random selections from each of the sun, com.sun and jdk namespaces
        //ForName is used to prevent warnings such as "warning: AbstractMultiResolutionImage is internal proprietary API and may be removed in a future release"
        Assert.assertTrue(classMatches(Class.forName("sun.awt.image.AbstractMultiResolutionImage")));
        Assert.assertTrue(classMatches(Class.forName("com.sun.awt.SecurityWarning")));
        Assert.assertTrue(classMatches(Class.forName("jdk.nashorn.api.scripting.AbstractJSObject")));
    }

    @Test
    public void testIgnoreMatcherMatches3rdParty() {
        //just something random from junit
        Assert.assertTrue(classMatches(org.junit.Before.class));
        //also matches against jacoco and springframework, but we don't have those available here
    }

    @Test
    public void testIgnoreMatcherMatchesDiSCo() {
        Assert.assertTrue(classMatches(this.getClass()));
    }

    @Test
    public void testIgnoreMatcherNotMatches() {
        //just an arbitrary class that happened to be in scope
        Assert.assertFalse(classMatches(org.hamcrest.BaseMatcher.class));
    }

    @Test
    public void testNullAgentBuilderIsSafe() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        Installable installable = (agentBuilder)->null;
        Set<Installable> installables = new HashSet<>();
        installables.add(installable);
        interceptionInstaller.install(null, installables, new AgentConfig(null), ElementMatchers.none());
    }

    private boolean classMatches(Class clazz) {
        return InterceptionInstaller.createIgnoreMatcher(ElementMatchers.none()).matches(new TypeDescription.ForLoadedType(clazz));
    }
}
