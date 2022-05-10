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

package software.amazon.disco.agent.config;

import net.bytebuddy.agent.builder.AgentBuilder;
import software.amazon.disco.agent.interception.Installable;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Holds agent configuration parsed during bootstrap.
 */
public class AgentConfig {
    private BiFunction<AgentBuilder, Installable, AgentBuilder> agentBuilderTransformer = new NoOpAgentBuilderTransformer();

    private List<String> args;
    private boolean isRuntimeOnly = false;
    private String pluginPath = null;
    private boolean verbose = false;
    private boolean extraverbose = false;
    private String loggerFactoryClass;

    /**
     * Construct a new AgentConfig
     *
     * @param args the list of "value" or "key=value" format args given on the command line
     */
    public AgentConfig(List<String> args) {
        this.args = args;
    }

    /**
     * Construct a new AgentConfig
     */
    public AgentConfig() {}

    /**
     * A default, 'identity', AgentBuilderTransformer, that just returns the inputted AgentBuilder
     */
    private static class NoOpAgentBuilderTransformer implements BiFunction<AgentBuilder, Installable, AgentBuilder> {
        @Override
        public AgentBuilder apply(AgentBuilder agentBuilder, Installable installable) {
            return agentBuilder;
        }
    }

    /**
     * Set a Transformer (a function taking an AgentBuilder, an Installable, and returning an AgentBuilder).
     * This transformer will be invoked in InterceptionInstaller to apply transformations on all Installables of
     * a given agent. Passing a null value will reset the AgentBuilderTransformer to the default value: {@link NoOpAgentBuilderTransformer}
     *
     * If a brand new instance is created by the transformer and returned while ignoring the passed in AgentBuilder, the default ignore rule and debugger listener
     * set by disco core will be lost. See {@link software.amazon.disco.agent.interception.InterceptionInstaller} for more detail on how they are set.
     *
     * @param agentBuilderTransformer the AgentBuilder Transformer to be applied to an AgentBuilder
     */
    public void setAgentBuilderTransformer(BiFunction<AgentBuilder, Installable, AgentBuilder> agentBuilderTransformer) {
        this.agentBuilderTransformer = agentBuilderTransformer == null ? new NoOpAgentBuilderTransformer() : agentBuilderTransformer;
    }

    /**
     * Get the registered Transformer (a function taking an AgentBuilder, an Installable, and returning an AgentBuilder)
     *
     * @return a transformed AgentBuilder instance, which may not be the same instance that was passed.
     */
    public BiFunction<AgentBuilder, Installable, AgentBuilder> getAgentBuilderTransformer() {
        return agentBuilderTransformer;
    }

    /**
     * Get the list of arguments which were given to the command line e.g. ["key1=value1", "key2=value2,value3", "value4"]
     * @return command line arguments
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Return true if configured to perform no instrumentation of classes. The agent in this mode only acts as a way to install
     * the Disco runtime (TransactionContext, EventBus and so on) such that it is on the correct classloader as demanded by
     * the agent manifest (usually the bootstrap such that Concurrency support works correctly). This mode is used when running an
     * application which does not need runtime instrumentation, e.g. if all Event-publishing classes are integrated explicitly, or
     * if classes have been transformed ahead of time by a build tool.
     * @return true if the agent should be a container for the runtime only, thus disabling all Installable interceptions
     */
    public boolean isRuntimeOnly() {
        return isRuntimeOnly;
    }

    /**
     * Get the configured path to search for decoupled disco plugins
     * @return the plugin path
     */
    public String getPluginPath() {
        return pluginPath;
    }

    /**
     * Get whether verbose (debug) logging is enabled.
     * @return true if debug level logging is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Get whether extra verbose (trace) logging is enabled.
     * @return true if trace level logging is enabled
     */
    public boolean isExtraverbose() {
        return extraverbose;
    }

    /**
     * Set whether this Agent should install no installables and be a runtime-only agent.
     * @param isRuntimeOnly true for a runtime-only agent, else false (the default)
     */
    protected void setRuntimeOnly(boolean isRuntimeOnly) {
        this.isRuntimeOnly = isRuntimeOnly;
    }

    /**
     * Set the path to search for decoupled disco plugins
     * @param pluginPath the plugin path
     */
    protected void setPluginPath(String pluginPath) {
        this.pluginPath = pluginPath;
    }

    /**
     * Set the list of arguments which were given to the command line e.g. ["key1=value1", "key2=value2,value3", "value4"]
     * @param args command line arguments
     */
    protected void setArgs(final List<String> args) {
        this.args = args;
    }

    /**
     * Set if verbose (debug) logging is enabled
     * @param verbose true to enable debug level logging
     */
    protected void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Set if extra verbose (trace) logging is enabled
     * @param extraverbose true to enable trace level logging
     */
    protected void setExtraverbose(boolean extraverbose) {
        this.extraverbose = extraverbose;
    }

    /**
     * Get the LoggerFactory class name which was set
     * @return the LoggerFactory class name
     */
    public String getLoggerFactoryClass() {
        return loggerFactoryClass;
    }

    /**
     * Set a LoggerFactory fully-qualified class name. The given class will be created using newInstance() and installed
     * into the LogManager
     * @param loggerFactoryClass the name of the LoggerFactory class to use
     */
    public void setLoggerFactoryClass(String loggerFactoryClass) {
        this.loggerFactoryClass = loggerFactoryClass;
    }
}
