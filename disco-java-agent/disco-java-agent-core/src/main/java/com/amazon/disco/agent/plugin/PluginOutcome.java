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

package com.amazon.disco.agent.plugin;

import com.amazon.disco.agent.event.Listener;
import com.amazon.disco.agent.interception.Installable;

import java.io.File;
import java.util.List;

/**
 * After PluginDiscovery, report to the caller what actions were taken by the plugin mechanism. We can inform the caller
 * which Listeners, Installables and Init classes were added, and where they came from.
 */
public class PluginOutcome {
    public final String name;
    public boolean bootstrap;
    public Class<?> initClass;
    public List<Listener> listeners;
    public List<Installable> installables;

    /**
     * Construct a PluginOutcome given the JarFile which was processed
     * @param jarFile
     */
    public PluginOutcome(File jarFile) {
        this.name = jarFile.getName();
    }
}
