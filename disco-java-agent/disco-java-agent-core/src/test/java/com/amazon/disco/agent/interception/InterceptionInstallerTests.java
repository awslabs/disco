package com.amazon.disco.agent.interception;

import com.amazon.disco.agent.config.AgentConfig;
import net.bytebuddy.description.type.TypeDescription;
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
    public void testIgnoreMatcherMatchesAmazonInternals() {
        //todo
        //matches against amazon.actiontrace, com.amazon.profiler, com.amazon.aaa, com.amazon.coral.spring,
        //but none are available here
    }

    @Test
    public void testIgnoreMatcherMatchesAlphaOne() {
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
        interceptionInstaller.install(null, installables, new AgentConfig(null));
    }

    private boolean classMatches(Class clazz) {
        return InterceptionInstaller.createIgnoreMatcher().matches(new TypeDescription.ForLoadedType(clazz));
    }
}
