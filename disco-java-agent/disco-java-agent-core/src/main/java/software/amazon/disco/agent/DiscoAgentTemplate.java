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

import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.config.AgentConfigParser;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.InterceptionInstaller;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.logging.LoggerFactory;
import software.amazon.disco.agent.plugin.PluginDiscovery;
import software.amazon.disco.agent.plugin.PluginOutcome;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/**
 * All agent products have different needs and use-cases, but initialization is super similar between them.
 * This class acts as a Template Method pattern implementation, to perform the constant initialization, but with each
 * agent able to configure which actual Installable hooks are present in the Agent instance.
 */
public class DiscoAgentTemplate {
    private static final Logger log = LogManager.getLogger(DiscoAgentTemplate.class);
    private static Supplier<AgentConfig> agentConfigFactory = null;

    protected final AgentConfig config;
    private InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
    private ElementMatcher.Junction<? super TypeDescription> customIgnoreMatcher = ElementMatchers.none();
    private boolean allowPlugins = true;

    /**
     * Constructs a new DiSCoAgentTemplate, which is the responsibility of any Agent build on DiSCo.
     * Parses the arguments given to the 'premain' method. Should be called immediately once inside premain.
     *
     * @param agentArgs any arguments passed as part of the -javaagent argument string
     */
    public DiscoAgentTemplate(String agentArgs) {
        if (agentConfigFactory == null) {
            this.config = new AgentConfigParser().parseCommandLine(agentArgs);
        } else {
            this.config = agentConfigFactory.get();
        }

        if (config.getLoggerFactoryClass() != null) {
            try {
                LoggerFactory loggerFactory = LoggerFactory.class.cast(Class.forName(config.getLoggerFactoryClass(), true, ClassLoader.getSystemClassLoader()).newInstance());
                LogManager.installLoggerFactory(loggerFactory);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                //nothing much can be done here. Can't even log it
            }
        }

        log.info("DiSCo(Core) finished parsing argument list: " + agentArgs);
        if (config.isExtraverbose()) {
            LogManager.setMinimumLevel(Logger.Level.TRACE);
        } else if (config.isVerbose()) {
            LogManager.setMinimumLevel(Logger.Level.DEBUG);
        }
    }

    /**
     * Set whether or not to allow a plugin search
     * @param allowPlugins true to allow plugins (which is the default), or false to prevent
     */
    public void setAllowPlugins(boolean allowPlugins) {
        this.allowPlugins = allowPlugins;
    }

    /**
     * Set an extra ignore rule for ByteBuddy, if you know of extra rules beyond the defaults that make sense for your
     * agent.
     * @param customIgnoreMatcher - an ignore matcher which will be logically OR'd with the default
     */
    public void setCustomIgnoreMatcher(ElementMatcher.Junction<? super TypeDescription> customIgnoreMatcher) {
        this.customIgnoreMatcher = customIgnoreMatcher;
    }

    /**
     * This method wraps installation of hooks on behalf of agents, performing default
     * boilerplate initialization common to both.
     *
     * @param instrumentation the Instrumentation object given to every Agent, to transform bytecode
     * @param installables the agent supplies a collection of Installables to be installed.
     * @return information about any loaded plugins
     */
    public Collection<PluginOutcome> install(Instrumentation instrumentation, Set<Installable> installables) {
        return install(instrumentation, installables, ElementMatchers.none());
    }

    /**
     * This method wraps installation of hooks on behalf of agents, performing default
     * boilerplate initialization common to both.
     *
     * @param instrumentation the Instrumentation object given to every Agent, to transform bytecode
     * @param installables the agent supplies a collection of Installables to be installed.
     * @param customIgnoreMatcher an extra ignore rule to be OR'd with the default
     * @return information about any loaded plugins
     */
    public Collection<PluginOutcome> install(Instrumentation instrumentation, Set<Installable> installables, ElementMatcher.Junction<? super TypeDescription> customIgnoreMatcher) {
        if (config.isRuntimeOnly()) {
            log.info("DiSCo(Core) setting agent as runtime-only. Ignoring all Installables, including those in Plugins.");
            installables.clear();
        }

        //give the Plugin Discovery subsystem the chance to scan any configured plugin folder.
        if (allowPlugins) {
            PluginDiscovery.scan(instrumentation, config);
            installables.addAll(PluginDiscovery.processInstallables());
        } else {
            if (config.getPluginPath() != null) {
                log.warn("DiSCo(Core) plugin path set but agent is disallowing plugins. No plugins will be loaded");
            }
        }

        //give each installable the chance to handle command line args
        for (Installable installable: installables) {
            log.info("DiSCo(Core) passing arguments to " + installable.getClass().getSimpleName() + " to process");
            installable.handleArguments(config.getArgs());
        }

        interceptionInstaller.install(instrumentation, installables, config, customIgnoreMatcher);

        //after Installables have been installed, process the remaining plugin behaviour
        if (allowPlugins) {
            return PluginDiscovery.apply();
        } else {
            return new ArrayList<>(0);
        }
    }

    /**
     * Get the AgentConfig for inspection if needed
     * @return the AgentConfig which was created via DiscoAgentTemplate construction
     */
    public AgentConfig getConfig() {
        return config;
    }

    /**
     * For tests, override the interception installer
     * @param interceptionInstaller - a new (presumably mock) InterceptionInstaller to use
     * @return the previous InterceptionInstaller
     */
    InterceptionInstaller setInterceptionInstaller(InterceptionInstaller interceptionInstaller) {
        InterceptionInstaller old = this.interceptionInstaller;
        this.interceptionInstaller = interceptionInstaller;
        return old;
    }

    /**
     * Override the default AgentConfig behavior, which is to read from the command line, if there is an alternative source of configuration.
     * Must be called before constructing DiscoAgentTemplate.
     * @param factory the new supplier of an AgentConfig to use.
     */
    public static void setAgentConfigFactory(Supplier<AgentConfig> factory) {
        agentConfigFactory = factory;
    }

    /**
     * Retrieve the current AgentConfigFactory
     *
     * @return the currently configured AgentConfigFactory, null if unset.
     */
    public static Supplier<AgentConfig> getAgentConfigFactory() {
        return agentConfigFactory;
    }
}
