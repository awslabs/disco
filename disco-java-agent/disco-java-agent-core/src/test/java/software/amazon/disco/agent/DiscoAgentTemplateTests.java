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

package software.amazon.disco.agent;

import org.mockito.Spy;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.concurrent.decorate.DecoratedRunnable;
import software.amazon.disco.agent.concurrent.preprocess.DiscoRunnableDecorator;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.interception.EffectVerificationStrategy;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.InstallationError;
import software.amazon.disco.agent.interception.InterceptionInstaller;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.plugin.PluginOutcome;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class DiscoAgentTemplateTests {
    @Spy
    private InterceptionInstaller mockInterceptionInstaller = InterceptionInstaller.getInstance();

    @Mock
    private Instrumentation instrumentation;

    @Captor
    private ArgumentCaptor<Set<Installable>> installableSetArgumentCaptor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        Mockito.doCallRealMethod().when(mockInterceptionInstaller).install(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        TransactionContext.create();
        DiscoAgentTemplate.setEffectVerificationStrategy(EffectVerificationStrategy.Standard.DELEGATE_TO_PLUGINS);
    }

    @After
    public void after() {
        TransactionContext.clear();
        DiscoAgentTemplate.setAgentConfigFactory(null);
    }

    @Test
    public void testRuntimeOnly() {
        Installable mockInstallable = new DummyInstallable();
        Set<Installable> installables = new HashSet<>();
        installables.add(mockInstallable);
        install(createDiscoAgentTemplate("runtimeOnly"), installables);
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any(), Mockito.any());
        Mockito.verifyNoInteractions(instrumentation);
        Assert.assertTrue(installableSetArgumentCaptor.getValue().isEmpty());
    }

    @Test
    public void testVerboseLogging() {
        install(createDiscoAgentTemplate("verbose"));
        Assert.assertEquals(Logger.Level.DEBUG, LogManager.getMinimumLevel());
    }

    @Test
    public void testExtraVerboseLogging() {
        install(createDiscoAgentTemplate("extraverbose"));
        assertEquals(Logger.Level.TRACE, LogManager.getMinimumLevel());
    }

    @Test
    public void testArgumentHandlerCalled() {
        Installable mock = Mockito.mock(Installable.class);
        install(createDiscoAgentTemplate("key=value"), new HashSet<>(Arrays.asList(Installable.class.cast(mock))));
        List<String> args = new LinkedList<>(Arrays.asList("key=value", "domain=DOMAIN", "realm=REALM"));
        Mockito.verify(mock).handleArguments(Mockito.eq(args));
    }

    @Test
    public void testSetAgentConfigFactory(){
        Assert.assertNull(DiscoAgentTemplate.getAgentConfigFactory());
        DiscoAgentTemplate.setAgentConfigFactory(()->null);
        Assert.assertNotNull(DiscoAgentTemplate.getAgentConfigFactory());
    }

    @Test
    public void testConstructorInvokesAgentConfigFactory(){
        List<String> args = Mockito.mock(List.class);
        Supplier<AgentConfig> factory = Mockito.mock(Supplier.class);

        Mockito.when(factory.get()).thenReturn(new AgentConfig(args));
        DiscoAgentTemplate.setAgentConfigFactory(factory);

        DiscoAgentTemplate template = new DiscoAgentTemplate(null);

        Mockito.verify(factory).get();
        Assert.assertNotNull(DiscoAgentTemplate.getAgentConfigFactory());
        Assert.assertSame(args, template.config.getArgs());
    }

    @Test
    public void testSetDecorateFunction() {
        DiscoRunnableDecorator.setDecorateFunction(null);
        Runnable runnable = Mockito.mock(Runnable.class);

        // ensure that the decorate function was indeed set to null since Runnable was not decorated
        Assert.assertSame(runnable, DiscoRunnableDecorator.maybeDecorate(runnable));

        install(createDiscoAgentTemplate(), Collections.emptySet());

        Assert.assertTrue(DiscoRunnableDecorator.maybeDecorate(runnable) instanceof DecoratedRunnable);
    }

    @Test
    public void testInstallationErrorListing_delegateToPlugins() {
        Collection<PluginOutcome> pluginOutcomes = createDiscoAgentTemplate()
                .install(instrumentation, InstallableWithInstallationErrors.SINGLETON_SET);
        Assert.assertEquals(pluginOutcomes.size(), 1);
        PluginOutcome coreOutcome = pluginOutcomes.iterator().next();
        Assert.assertEquals(coreOutcome.name, DiscoAgentTemplate.CORE_PSEUDO_PLUGIN_NAME);
        Assert.assertEquals(coreOutcome.installationErrors, InstallableWithInstallationErrors.ERRORS);
    }

    @Test
    public void testInstallationErrorListing_noVerification() {
        DiscoAgentTemplate.setEffectVerificationStrategy(EffectVerificationStrategy.Standard.NO_VERIFICATION);
        Collection<PluginOutcome> pluginOutcomes = createDiscoAgentTemplate()
                .install(instrumentation, InstallableWithInstallationErrors.SINGLETON_SET);
        Assert.assertEquals(pluginOutcomes.size(), 1);
        PluginOutcome coreOutcome = pluginOutcomes.iterator().next();
        Assert.assertEquals(coreOutcome.name, DiscoAgentTemplate.CORE_PSEUDO_PLUGIN_NAME);
        Assert.assertTrue(coreOutcome.installationErrors.isEmpty());
    }

    private DiscoAgentTemplate createDiscoAgentTemplate(String... args) {
        List<String> argsList = new LinkedList<>(Arrays.asList(args));
        argsList.add("domain=DOMAIN");
        argsList.add("realm=REALM");
        DiscoAgentTemplate discoAgentTemplate = new DiscoAgentTemplate(String.join(":", argsList));
        discoAgentTemplate.setInterceptionInstaller(mockInterceptionInstaller);
        return discoAgentTemplate;
    }

    private DiscoAgentTemplate install(DiscoAgentTemplate discoAgentTemplate, Set<Installable> installables) {
        discoAgentTemplate.install(instrumentation, installables);
        return discoAgentTemplate;
    }

    private DiscoAgentTemplate install(DiscoAgentTemplate discoAgentTemplate) {
        return install(discoAgentTemplate, new HashSet<>());
    }

    static class DummyInstallable implements Installable {
        @Override
        public AgentBuilder install(AgentBuilder agentBuilder) {
            return agentBuilder;
        }
    }

    static class InstallableWithInstallationErrors implements Installable {
        static final List<InstallationError> ERRORS = new LinkedList<>();
        static {
            ERRORS.add(new InstallationError("Description #1."));
            ERRORS.add(new InstallationError("Description #2."));
        }
        static final Set<Installable> SINGLETON_SET = new HashSet<>();
        static {
            SINGLETON_SET.add(new InstallableWithInstallationErrors());
        }

        @Override
        public AgentBuilder install(AgentBuilder agentBuilder) {
            return agentBuilder;
        }

        @Override
        public List<InstallationError> verifyEffect() { return ERRORS; }
    }
}
