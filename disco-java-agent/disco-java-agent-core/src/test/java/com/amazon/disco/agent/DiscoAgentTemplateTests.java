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

package com.amazon.disco.agent;

import com.amazon.disco.agent.concurrent.TransactionContext;
import com.amazon.disco.agent.interception.Installable;
import com.amazon.disco.agent.interception.InterceptionInstaller;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DiscoAgentTemplateTests {
    @Mock
    private InterceptionInstaller mockInterceptionInstaller;
    @Captor
    private ArgumentCaptor<Set<Installable>> installableSetArgumentCaptor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        TransactionContext.create();
    }

    @After
    public void after() {
        TransactionContext.clear();
    }

    @Test
    public void testDefaultInstallables() {
        Installable mockInstallable = Mockito.mock(Installable.class);
        Set<Installable> installables = new HashSet<>();
        installables.add(mockInstallable);
        install(createDiscoAgentTemplate(), installables);
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any(), Mockito.any());
        Assert.assertTrue(installableSetArgumentCaptor.getValue().contains(mockInstallable));
    }

    @Test
    public void testNoDefaultInstallables() {
        Installable mockInstallable = Mockito.mock(Installable.class);
        Set<Installable> installables = new HashSet<>();
        installables.add(mockInstallable);
        install(createDiscoAgentTemplate("noDefaultInstallables"), installables);
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any(), Mockito.any());
        Assert.assertTrue(installableSetArgumentCaptor.getValue().isEmpty());
    }

    @Test
    public void testCorrectCustomInstallables() {
        install(createDiscoAgentTemplate("installables="+MockInstallable.class.getName()));
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any(), Mockito.any());
        assertEquals(MockInstallable.class.getName(), installableSetArgumentCaptor.getValue().iterator().next().getClass().getName());
    }

    @Test
    public void testIncorrectCustomInstallablesSafelyIgnored() {
        String[] installableNames = {
                //not an Installable
                "java.lang.Object",

                //no such class
                "com.example.Nothing",

                //abstract class
                Installable.class.getName()
        };

        install(createDiscoAgentTemplate("installables="+String.join(",", installableNames)));
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any(), Mockito.any());
        Assert.assertTrue(installableSetArgumentCaptor.getValue().isEmpty());
    }

    @Test
    public void testVerboseLogging() {
        install(createDiscoAgentTemplate("verbose"));
        assertEquals(Logger.Level.DEBUG, LogManager.getMinimumLevel());
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
        List<String> args = new LinkedList<>(Arrays.asList("key=value", "applicationName=TestApp", "domain=DOMAIN", "realm=REALM"));
        Mockito.verify(mock).handleArguments(Mockito.eq(args));
    }

    private DiscoAgentTemplate createDiscoAgentTemplate(String... args) {
        List<String> argsList = new LinkedList<>(Arrays.asList(args));
        argsList.add("applicationName=TestApp");
        argsList.add("domain=DOMAIN");
        argsList.add("realm=REALM");
        DiscoAgentTemplate discoAgentTemplate = new DiscoAgentTemplate(String.join(":", argsList));
        discoAgentTemplate.setInterceptionInstaller(mockInterceptionInstaller);
        return discoAgentTemplate;
    }

    private DiscoAgentTemplate install(DiscoAgentTemplate discoAgentTemplate, Set<Installable> installables) {
        discoAgentTemplate.install(Mockito.mock(Instrumentation.class), installables);
        return discoAgentTemplate;
    }

    private DiscoAgentTemplate install(DiscoAgentTemplate discoAgentTemplate) {
        return install(discoAgentTemplate, new HashSet<>());
    }

    //not genuinely using this object, just using it to produce a classname which implements Installable
    //Cannot use a Mockito 1.x mock due to the reliance on the default method in the ArgumentHandler base interface.
    static class MockInstallable implements Installable {
        @Override
        public AgentBuilder install(AgentBuilder agentBuilder) {
            return null;
        }
    }
}
