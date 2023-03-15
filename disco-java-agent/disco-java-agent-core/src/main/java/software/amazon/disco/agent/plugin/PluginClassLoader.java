/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A custom URLClassLoader for loading plugins found inside of PluginDiscovery, exposing addURL for JDK 17 reflective access.
 * This is the classloader used by default when loading plugins, unless JAR manifest attributes specify otherwise.
 */
public class PluginClassLoader extends URLClassLoader {
    public static ClassLoader BOOTSTRAP_CLASSLOADER = null;

    /**
     * The default constructor for creating the classloader. The parent is the bootstrap classloader.
     */
    public PluginClassLoader() {
        super(new URL[]{}, BOOTSTRAP_CLASSLOADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
