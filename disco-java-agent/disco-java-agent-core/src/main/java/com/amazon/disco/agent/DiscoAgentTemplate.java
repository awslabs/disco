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

import com.amazon.disco.agent.config.AgentConfig;
import com.amazon.disco.agent.config.AgentConfigParser;
import com.amazon.disco.agent.interception.Installable;
import com.amazon.disco.agent.interception.InterceptionInstaller;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;
import com.amazon.disco.agent.logging.LoggerFactory;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.Set;

/**
 * All agent products have different needs and use-cases, but initialization is super similar between them.
 * This class acts as a Template Method pattern implementation, to perform the constant initialization, but with each
 * agent able to configure which actual Installable hooks are present in the Agent instance.
 */
public class DiscoAgentTemplate {
    private static Logger log = LogManager.getLogger(DiscoAgentTemplate.class);

    private AgentConfig config;
    private InterceptionInstaller interceptionInstaller = InterceptionInstaller.getInstance();
    private ElementMatcher.Junction<? super TypeDescription> customIgnoreMatcher = ElementMatchers.none();

    /**
     * Constructs a new DiSCoAgentTemplate, which is the responsibility of any Agent build on DiSCo.
     * Parses the arguments given to the 'premain' method. Should be called immediately once inside premain.
     *
     * @param agentArgs any arguments passed as part of the -javaagent argument string
     */
    public DiscoAgentTemplate(String agentArgs) {
        this.config = new AgentConfigParser().parseCommandLine(agentArgs);
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
     */
    public void install(Instrumentation instrumentation, Set<Installable> installables) {
        install(instrumentation, installables, ElementMatchers.none());
    }

    /**
     * This method wraps installation of hooks on behalf of agents, performing default
     * boilerplate initialization common to both.
     *
     * @param instrumentation the Instrumentation object given to every Agent, to transform bytecode
     * @param installables the agent supplies a collection of Installables to be installed.
     * @param customIgnoreMatcher an extra ignore rule to be OR'd with the default
     */
    public void install(Instrumentation instrumentation, Set<Installable> installables, ElementMatcher.Junction<? super TypeDescription> customIgnoreMatcher) {
        if (!config.isInstallDefaultInstallables()) {
            log.info("DiSCo(Core) removing all default installables as requested");
            installables.clear();
        }
        addCustomInstallables(installables);

        //give each installable the chance to handle command line args
        for (Installable installable: installables) {
            log.info("DiSCo(Core) passing arguments to " + installable.getClass().getSimpleName() + " to process");
            installable.handleArguments(config.getArgs());
        }

        interceptionInstaller.install(instrumentation, installables, config, customIgnoreMatcher);
    }

    /**
     * If supplied on the command line, process the list of Installable class names, instantiate them, and
     * add them to the set of Installables for installation.
     * @param installables - the set of installables to populate
     */
    private void addCustomInstallables(Set<Installable> installables) {
        for (String className: config.getCustomInstallableClasses()) {
            Class clazz;
            try {
                clazz = Class.forName(className, true, ClassLoader.getSystemClassLoader());
            } catch (ClassNotFoundException e) {
                log.error("DiSCo(Core) Custom installable " + className + " not found (is it on the classpath?), skipping");
                continue;
            }

            Installable installable;
            try {
                 installable = (Installable) clazz.newInstance();
            } catch (IllegalAccessException e) {
                log.error("DiSCo(Core) Custom installable " + className + " could not be instantiated - non-public or inaccessible default ctor, skipping");
                continue;
            } catch (InstantiationException e) {
                log.error("DiSCo(Core) Custom installable " + className + " could not be instantiated - either no default ctor, or a non-concrete class, skipping");
                continue;
            } catch (ClassCastException e) {
                log.error("DiSCo(Core) Custom installable " + className + " could not be instantiated - does not inherit from Installable, skipping");
                continue;
            }

            log.info("DiSCo(Core) Adding Custom Installable " + className);
            installables.add(installable);
        }
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
}
