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

package software.amazon.disco.agent.interception;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.config.AgentConfigParser;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class InterceptionInstallerTests {
    @Test
    public void testIgnoreMatcherMatchesJavaInternals() throws Exception {
        //random selections from each of the sun, com.sun and jdk namespaces
        //ForName is used to prevent warnings such as "warning: such-and-such-class is internal proprietary API and may be removed in a future release", or deprecation warnings.
        Assert.assertTrue(classMatches(Class.forName("sun.misc.Unsafe")));
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
        Installable installable = (agentBuilder) -> null;
        Set<Installable> installables = new HashSet<>();
        installables.add(installable);
        interceptionInstaller.install(null, installables, new AgentConfig(null), ElementMatchers.none());
    }

    @Test
    public void testAgentBuilderHasDefaultIgnoreMatcher() {
        ElementMatcher<? super TypeDescription> ignoreMatcher = InterceptionInstaller.createIgnoreMatcher(ElementMatchers.none());
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        AgentBuilder agentBuilder = Mockito.mock(AgentBuilder.class);
        Supplier<AgentBuilder> original = interceptionInstaller.setAgentBuilderFactory(()->agentBuilder);
        interceptionInstaller.install(Mockito.mock(Instrumentation.class), new HashSet<>(Arrays.asList((a)->a)), new AgentConfig(null), ElementMatchers.none());
        Mockito.verify(agentBuilder).ignore(Mockito.eq(ignoreMatcher));
        interceptionInstaller.setAgentBuilderFactory(original);
    }

    @Test
    public void testAgentBuilderNotHasListenerWhenNotVerbose() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        MockAgentBuilderFactory factory = new MockAgentBuilderFactory();
        Supplier<AgentBuilder> original = interceptionInstaller.setAgentBuilderFactory(factory);
        interceptionInstaller.install(Mockito.mock(Instrumentation.class), new HashSet<>(Arrays.asList((a)->a)), new AgentConfig(null), ElementMatchers.none());
        Mockito.verify(factory.agentBuilder, Mockito.never()).with(Mockito.any(AgentBuilder.Listener.class));
        interceptionInstaller.setAgentBuilderFactory(original);
    }

    @Test
    public void testAgentBuilderHasListenerWhenVerbose() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        MockAgentBuilderFactory factory = new MockAgentBuilderFactory();
        Supplier<AgentBuilder> original = interceptionInstaller.setAgentBuilderFactory(factory);
        AgentConfig agentConfig = new AgentConfigParser().parseCommandLine("extraverbose");
        interceptionInstaller.install(Mockito.mock(Instrumentation.class), new HashSet<>(Arrays.asList((a)->a)), agentConfig, ElementMatchers.none());
        Mockito.verify(factory.agentBuilder).with(Mockito.any(AgentBuilder.Listener.class));
        interceptionInstaller.setAgentBuilderFactory(original);
    }

    @Test
    public void testAgentBuilderIsInstalledOnInstrumentation() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        MockAgentBuilderFactory factory = new MockAgentBuilderFactory();
        Supplier<AgentBuilder> original = interceptionInstaller.setAgentBuilderFactory(factory);
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        interceptionInstaller.install(instrumentation, new HashSet<>(Arrays.asList((a)->a)), new AgentConfig(null), ElementMatchers.none());
        Mockito.verify(factory.agentBuilder).installOn(instrumentation);
        interceptionInstaller.setAgentBuilderFactory(original);
    }


    @Test
    public void testInstallablesInstallMethodCalled() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
        MockAgentBuilderFactory factory = new MockAgentBuilderFactory();
        Supplier<AgentBuilder> original = interceptionInstaller.setAgentBuilderFactory(factory);
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Set<Installable> installables = new HashSet<>(Arrays.asList(Mockito.mock(Installable.class)));
        interceptionInstaller.install(instrumentation, installables, new AgentConfig(null), ElementMatchers.none());
        for (Installable installable: installables) {
            Mockito.verify(installable).install(factory.agentBuilder);
        }
        interceptionInstaller.setAgentBuilderFactory(original);
    }

    @Test
    public void testInstallWorksWithACollectionOfInstallables() {
        InterceptionInstaller interceptionInstaller = Mockito.spy(InterceptionInstaller.getInstance());
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);

        Installable installable_a = Mockito.mock(Installable.class);
        Installable installable_b = Mockito.mock(Installable.class);
        AgentBuilder builder_a = Mockito.mock(AgentBuilder.class);
        AgentBuilder builder_b = Mockito.mock(AgentBuilder.class);

        Mockito.doReturn(builder_a).when(installable_a).install(Mockito.any());
        Mockito.doReturn(builder_b).when(installable_b).install(Mockito.any());

        interceptionInstaller.install(instrumentation, new HashSet<>(Arrays.asList(installable_a, installable_b)), new AgentConfig(null), null);

        Mockito.verify(installable_a).install(Mockito.any());
        Mockito.verify(installable_b).install(Mockito.any());

        Mockito.verify(builder_a).installOn(instrumentation);
        Mockito.verify(builder_b).installOn(instrumentation);
    }

    @Test
    public void testDefaultAgentBuilderTransformerDoesNothing() {
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();

        MockAgentBuilderFactory factory = new MockAgentBuilderFactory();
        interceptionInstaller.setAgentBuilderFactory(factory);

        AgentBuilder builder = factory.get();
        AgentConfig agentConfig = Mockito.spy(new AgentConfig(null));
        Installable installable = Mockito.mock(Installable.class);

        //spy on the real default transformer
        BiFunction<AgentBuilder, Installable, AgentBuilder> agentBuilderTransformer = Mockito.spy(agentConfig.getAgentBuilderTransformer());

        interceptionInstaller.setAgentBuilderFactory(factory);

        //return the spied instance of the default transformer instead.
        Mockito.when(agentConfig.getAgentBuilderTransformer()).thenReturn(agentBuilderTransformer);

        interceptionInstaller.install(
                Mockito.mock(Instrumentation.class),
                new HashSet(Arrays.asList(installable)),
                agentConfig,
                ElementMatchers.none());

        Mockito.verify(agentBuilderTransformer).apply(builder, installable);

        //first interaction occurs when an ignore matcher is being added to the builder which is an intended operation.
        Mockito.verify(builder).ignore(Mockito.any(ElementMatcher.class));
        Mockito.verifyNoMoreInteractions(builder);
        Mockito.verify(installable).install(Mockito.eq(builder));
    }

    @Test
    public void testNonDefaultAgentBuilderTransformerReturningSameInstance(){
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();

        MockAgentBuilderFactory factory = new MockAgentBuilderFactory();
        interceptionInstaller.setAgentBuilderFactory(factory);

        AgentBuilder builder = factory.get();
        AgentConfig agentConfig = Mockito.spy(new AgentConfig(null));
        Installable installable = Mockito.mock(Installable.class);

        BiFunction<AgentBuilder, Installable, AgentBuilder> agentBuilderTransformer = Mockito.spy(new BiFunction<AgentBuilder, Installable, AgentBuilder>() {
            @Override
            public AgentBuilder apply(AgentBuilder agentBuilder, Installable installable) {
                agentBuilder.disableClassFormatChanges();
                return agentBuilder;
            }
        });

        Mockito.when(agentConfig.getAgentBuilderTransformer()).thenReturn(agentBuilderTransformer);

        interceptionInstaller.install(
                Mockito.mock(Instrumentation.class),
                new HashSet(Arrays.asList(installable)),
                agentConfig,
                ElementMatchers.none());

        Mockito.verify(agentBuilderTransformer).apply(builder, installable);

        //first interaction occurs when an ignore matcher is being added to the builder which is an intended operation.
        Mockito.verify(builder).ignore(Mockito.any(ElementMatcher.class));
        Mockito.verify(builder).disableClassFormatChanges();
        Mockito.verify(installable).install(Mockito.eq(builder));
    }

    @Test
    public void testNonDefaultAgentBuilderTransformerReturningDifferentInstance(){
        InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();

        MockAgentBuilderFactory factory = new MockAgentBuilderFactory();
        interceptionInstaller.setAgentBuilderFactory(factory);

        AgentBuilder originalBuilder = factory.get();
        AgentBuilder differentBuilder = Mockito.mock(AgentBuilder.class);
        AgentConfig agentConfig = Mockito.spy(new AgentConfig(null));
        Installable installable = Mockito.mock(Installable.class);

        BiFunction<AgentBuilder, Installable, AgentBuilder> agentBuilderTransformer = Mockito.spy(new BiFunction<AgentBuilder, Installable, AgentBuilder>() {
            @Override
            public AgentBuilder apply(AgentBuilder agentBuilder, Installable installable) {
                return differentBuilder;
            }
        });

        Mockito.when(agentConfig.getAgentBuilderTransformer()).thenReturn(agentBuilderTransformer);

        interceptionInstaller.install(
                Mockito.mock(Instrumentation.class),
                new HashSet(Arrays.asList(installable)),
                agentConfig,
                ElementMatchers.none());

        Mockito.verify(agentBuilderTransformer).apply(originalBuilder, installable);

        //first interaction occurs when an ignore matcher is being added to the builder which is an intended operation.
        Mockito.verify(originalBuilder).ignore(Mockito.any(ElementMatcher.class));

        ArgumentCaptor<AgentBuilder> agentBuilderArgumentCaptor = ArgumentCaptor.forClass(AgentBuilder.class);
        Mockito.verify(installable).install(agentBuilderArgumentCaptor.capture());
        Assert.assertEquals(differentBuilder, agentBuilderArgumentCaptor.getValue());
        Assert.assertNotEquals(originalBuilder, differentBuilder);
    }

    private boolean classMatches(Class clazz) {
        return InterceptionInstaller.createIgnoreMatcher(ElementMatchers.none()).matches(new TypeDescription.ForLoadedType(clazz));
    }

    private static class MockAgentBuilderFactory implements Supplier<AgentBuilder> {
        public final AgentBuilder agentBuilder;

        public MockAgentBuilderFactory() {
            AgentBuilder.Ignored agentBuilder = Mockito.mock(AgentBuilder.Ignored.class);
            Mockito.when(agentBuilder.ignore(Mockito.any(ElementMatcher.class))).thenReturn(agentBuilder);
            this.agentBuilder = agentBuilder;
        }
        @Override
        public AgentBuilder get() {
            return agentBuilder;
        }
    }
}
