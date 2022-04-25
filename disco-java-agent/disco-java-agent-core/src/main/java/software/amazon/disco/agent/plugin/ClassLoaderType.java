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

/**
 * An enum representing the classloader a plugin was loaded into. For example, a PluginOutcome with the 'classLoaderType'
 * property set to ClassLoaderType.PLUGIN denotes that specific plugin was loaded by a PluginClassLoader.
 */
enum ClassLoaderType {
    // Representative of the bootstrap classloader
    BOOTSTRAP,

    // Representative of the system classloader
    SYSTEM,

    // Representative of the PluginClassLoader
    PLUGIN,

    // Representative of an invalid input to 'Disco-Classloader' (and therefore that the plugin should not be loaded)
    INVALID
}
