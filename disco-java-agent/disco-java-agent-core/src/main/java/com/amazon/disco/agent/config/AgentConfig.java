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

package com.amazon.disco.agent.config;

import java.util.List;

/**
 * Holds agent configuration parsed during bootstrap.
 */
public class AgentConfig {
    private List<String> args;
    private String applicationName;
    private boolean installDefaultInstallables = true;
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
     * Get the application name set from the command line
     * @return the application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Return if we are configured to install the default installables (i.e. the ones expressly listed out in the Agent startup).
     * @return true if default installables should be installed, else false
     */
    public boolean isInstallDefaultInstallables() {
        return installDefaultInstallables;
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
     * Set the application name
     * @param applicationName the application name
     */
    protected void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Set whether to install the default installables for this agent
     * @param installDefaultInstallables true to install the default installables, else false
     */
    protected void setInstallDefaultInstallables(boolean installDefaultInstallables) {
        this.installDefaultInstallables = installDefaultInstallables;
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
