package com.amazon.disco.agent;

import com.amazon.disco.agent.concurrent.TransactionContext;
import com.amazon.disco.agent.config.Config;
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

public class AlphaOneAgentTemplateTests {
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
    public void testApplicationName() {
        install(createAlphaOneAgentTemplate());
        Assert.assertEquals("TestApp", Config.getVictimApplicationName());
    }

    @Test
    public void testDefaultInstallables() {
        Installable mockInstallable = Mockito.mock(Installable.class);
        Set<Installable> installables = new HashSet<>();
        installables.add(mockInstallable);
        install(createAlphaOneAgentTemplate(), installables);
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any());
        Assert.assertTrue(installableSetArgumentCaptor.getValue().contains(mockInstallable));
    }

    @Test
    public void testNoDefaultInstallables() {
        Installable mockInstallable = Mockito.mock(Installable.class);
        Set<Installable> installables = new HashSet<>();
        installables.add(mockInstallable);
        install(createAlphaOneAgentTemplate("noDefaultInstallables"), installables);
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any());
        Assert.assertTrue(installableSetArgumentCaptor.getValue().isEmpty());
    }

    @Test
    public void testCorrectCustomInstallables() {
        install(createAlphaOneAgentTemplate("installables="+MockInstallable.class.getName()));
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any());
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

        install(createAlphaOneAgentTemplate("installables="+String.join(",", installableNames)));
        Mockito.verify(mockInterceptionInstaller).install(Mockito.any(), installableSetArgumentCaptor.capture(), Mockito.any());
        Assert.assertTrue(installableSetArgumentCaptor.getValue().isEmpty());
    }

    @Test
    public void testVerboseLogging() {
        install(createAlphaOneAgentTemplate("verbose"));
        assertEquals(Logger.Level.DEBUG, LogManager.getMinimumLevel());
    }

    @Test
    public void testExtraVerboseLogging() {
        install(createAlphaOneAgentTemplate("extraverbose"));
        assertEquals(Logger.Level.TRACE, LogManager.getMinimumLevel());
    }

    @Test
    public void testArgumentHandlerCalled() {
        Installable mock = Mockito.mock(Installable.class);
        install(createAlphaOneAgentTemplate("key=value"), new HashSet<>(Arrays.asList(Installable.class.cast(mock))));
        List<String> args = new LinkedList<>(Arrays.asList("key=value", "applicationName=TestApp", "domain=DOMAIN", "realm=REALM"));
        Mockito.verify(mock).handleArguments(Mockito.eq(args));
    }

    private AlphaOneAgentTemplate createAlphaOneAgentTemplate(String... args) {
        List<String> argsList = new LinkedList<>(Arrays.asList(args));
        argsList.add("applicationName=TestApp");
        argsList.add("domain=DOMAIN");
        argsList.add("realm=REALM");
        AlphaOneAgentTemplate alphaOneAgentTemplate = new AlphaOneAgentTemplate(String.join(":", argsList));
        alphaOneAgentTemplate.setInterceptionInstaller(mockInterceptionInstaller);
        return alphaOneAgentTemplate;
    }

    private AlphaOneAgentTemplate install(AlphaOneAgentTemplate alphaOneAgentTemplate, Set<Installable> installables) {
        alphaOneAgentTemplate.install(Mockito.mock(Instrumentation.class), installables);
        return alphaOneAgentTemplate;
    }

    private AlphaOneAgentTemplate install(AlphaOneAgentTemplate alphaOneAgentTemplate) {
        return install(alphaOneAgentTemplate, new HashSet<>());
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