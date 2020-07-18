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

import java.util.List;

/**
 * Holds agent configuration parsed during bootstrap.
 */
public class AgentConfig {
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
