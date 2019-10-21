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

package software.amazon.disco.agent.plugin;

import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Disco can load specially formatted JAR files as plugins, which may contain add-on Installables, and Listeners.
 * This is an alternative to building monolithic agents where it is predicted that end user installations will require
 * multiple disco-enabled agents (e.g. a logging agent and a metrics agent at the same time). It is at least a performance hazard
 * to install disco's core interception treatments repeatedly, and the multi-agent scenario is not even guaranteed to work
 * at all. For such scenarios, a decoupled agent can act as a substrate for plugins, ensuring that disco's core interceptions
 * are only instantiated once, and extra Installables and EventBus Listeners are loaded by discovery, not by hard coding
 * into a built agent.
 *
 * Each plugin is delivered as a JAR file, containing a Manifest with the following properties:
 *
 * Disco-Init-Class: if any one-off-initialization is required, a fully qualified class may be provided. If this class provides a method
 *                   matching the signature "public static void init(void)", that method will be executed.
 * Disco-Installable-Classes: a space-separated list of fully qualified class names which are expected to inherit from Installable
 *                            and have a no-args constructor.
 * Disco-Listener-Classes: a space-separated list of fully qualified class names which are expected to inherit from Listener
 *                         and have a no-args constructor.
 * Disco-Bootstrap-Classloader: if set to the literal case-insensitive string 'true', this JAR file will be added to the runtime's bootstrap
 *                              classloader. Any other value, or the absence of this attribute, means the plugin will be loaded
 *                              via the system classloader like a normal runtime dependency. It is not usually necessary to
 *                              specify this attribute, unless Installables wish to intercept JDK classes.
 *
 * At startup, the decoupled disco agent searches the Config's pluginPath for such JAR files, and loads the plugins according
 * to the Manifest's content.
 */
public class PluginDiscovery {
    private static final Logger log = LogManager.getLogger(PluginDiscovery.class);

    /**
     * Entry point for plugin discovery. Should be called just once in any agent. It is called by DiscoAgentTemplate.install()
     * and should not be called directly under normal circumstances.
     * @param instrumentation the Instrumentation instance to use for interacting with this plugin
     * @param installables the set of installables gathered so far by the surrounding agent, to be added to upon discovery of more.
     * @param config the Agent's config, from command line or otherwise
     * @return a collection of PluginOutcomes describing what took place during plugin discovery
     */
    public static List<PluginOutcome> init(Instrumentation instrumentation, Set<Installable> installables, AgentConfig config) {
        List<PluginOutcome> outcomes = null;

        try {
            if (config.getPluginPath() == null) {
                log.info("DiSCo(Core) no plugin path specified, skipping plugin scan");
                return new ArrayList<>();
            }

            File pluginDir = new File(config.getPluginPath());
            if (!pluginDir.isDirectory()) {
                log.warn("DiSCo(Core) invalid plugin path specified, skipping plugin scan");
                return new ArrayList<>();
            }

            outcomes = new LinkedList<>();

            for (File jarFile: pluginDir.listFiles()) {
                if (jarFile.getName().substring(jarFile.getName().lastIndexOf(".")).equalsIgnoreCase(".jar")) {
                    PluginOutcome outcome = processJarFile(instrumentation, installables, jarFile);
                    if (outcome != null) {
                        outcomes.add(outcome);
                    }
                } else {
                    //ignore non JAR file
                    log.info("DiSCo(Core) non JAR file found on plugin path, skipping this file");
                }
            }
        } catch (Throwable t) {
            //safely do nothing
            log.error("DiSCo(Core) error while processing plugins", t);
        }

        return outcomes;
    }

    /**
     * Process a single JAR file whilst scanning the plugin directory for plugins
     * @param instrumentation an Agent Instrumentation instance, used to manipulate classloader search paths. Typically acquired
     *                        from premain() or agentmain() methods, or by manually installing a ByteBuddyAgent instance.
     * @param installables the set of Installables found so far, for later passing to InterceptionInstaller methods
     * @param jarFile a Java File object represenenting the JAR file on disk
     * @return the PluginOutcome encapsulating all that took place whilst loading this plugin
     * @throws Exception
     */
    static PluginOutcome processJarFile(Instrumentation instrumentation, Set<Installable> installables, File jarFile) throws Exception {
        JarFile jar = new JarFile(jarFile);
        Manifest manifest = jar.getManifest();
        if (manifest == null) {
            log.info("DiSCo(Core) JAR file without manifest found on plugin path, skipping this file");
            return null;
        }
        jar.close();

        Attributes attributes = manifest.getMainAttributes();
        if (attributes == null) {
            log.info("DiSCo(Core) JAR file found with manifest without any main attributes, skipping this file");
            return null;
        }

        //read each pertinent Manifest attribute
        String initClassName = attributes.getValue("Disco-Init-Class");
        String installableClassNames = attributes.getValue("Disco-Installable-Classes");
        String listenerClassNames = attributes.getValue("Disco-Listener-Classes");
        String bootstrapClassloader = attributes.getValue("Disco-Bootstrap-Classloader");

        //load and process the plugin based on the Manifest
        PluginOutcome outcome = new PluginOutcome(jarFile);
        boolean bootstrap = loadJar(instrumentation, jarFile, bootstrapClassloader);
        outcome.bootstrap = bootstrap;
        outcome.initClass = initJar(initClassName, bootstrap);
        outcome.installables = initInstallables(installableClassNames, installables, bootstrap);
        outcome.listeners = initListeners(listenerClassNames, bootstrap);
        return outcome;
    }

    /**
     * Load the given Jar file by adding it appropriately to either the bootstrap or system classloader, as dictated by the
     * relevant Manifest entry
     * @param instrumentation an Agent Instrumentation instance, used to manipulate classloader search paths. Typically acquired
     *                        from premain() or agentmain() methods, or by manually installing a ByteBuddyAgent instance.
     * @param jarFile a Java File object representing the JAR file on disk
     * @param bootstrapClassLoader the Manifest entry declaring whether the JAR should be loaded by the bootstrap classloader.
     * @return true if the JAR was loaded by the bootstrap classloader, else false.
     */
    static boolean loadJar(Instrumentation instrumentation, File jarFile, String bootstrapClassLoader) throws Exception {
        boolean bootstrap = false;
        if (bootstrapClassLoader != null) {
            if (bootstrapClassLoader.trim().equalsIgnoreCase("true")) {
                bootstrap = true;
            }
        }

        JarFile jar = null;
        if (bootstrap) {
            if (LogManager.isDebugEnabled()) {
                log.debug("DiSCo(Core) attempting to load JAR file into bootstrap classloader: " + jarFile.getName());
            }
            jar = Injector.addToBootstrapClasspath(instrumentation, jarFile);
        } else {
            if (LogManager.isDebugEnabled()) {
                log.debug("DiSCo(Core) attempting to load JAR file into system classloader: " + jarFile.getName());
            }
            jar = Injector.addToSystemClasspath(instrumentation, jarFile);
        }
        if (jar != null) {
            jar.close();
        }

        return bootstrap;
    }

    /**
     * Helper to execute the one-off initialization method if specified in the plugin manifest.
     * @param initClassName the fully qualified classname of the class
     * @param bootstrap true if loading into the bootstrap classloader
     * @return the init class discovered, or null if none.
     */
    static Class<?> initJar(String initClassName, boolean bootstrap) throws Exception {
        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(Core) attempting to init plugin using class: " + initClassName);
        }
        if (initClassName != null) {
            Class<?> initClass = classForName(initClassName.trim(), bootstrap);
            initClass.getDeclaredMethod("init").invoke(null);
            return initClass;
        }
        return null;
    }

    /**
     * Helper to load all installables from the given whitespace-separated list
     * @param installableClassNames a string containing the list of Installable class names
     * @param installables the set of Installables found so far, for later passing to InterceptionInstaller methods
     * @param bootstrap true if loading into the bootstrap classloader
     * @return a set of all discovered and succesfully added installables.
     */
    static List<Installable> initInstallables(String installableClassNames, Set<Installable> installables, boolean bootstrap) throws Exception {
        List<Installable> newInstallables = new LinkedList<>();
        if (installableClassNames != null) {
            String[] classNames = installableClassNames.trim().split("\\s");
            for (String className: classNames) {
                if (LogManager.isDebugEnabled()) {
                    log.debug("DiSCo(Core) attempting to add Installable from plugin using class: " + className);
                }
                try {
                    Class<?> clazz = classForName(className.trim(), bootstrap);
                    if (Installable.class.isAssignableFrom(clazz)) {
                        Installable installable = (Installable) clazz.newInstance();
                        newInstallables.add(installable);
                        installables.add(installable);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("DiSCo(Core) failed to instantiate Installable: " + className);
                }
            }
        }
        return newInstallables;
    }

    /**
     * Helper to load all listeners from the given whitespace-separated list
     * @param listenerClassNames a string containing the list of Listener class names
     * @param bootstrap true if loading into the bootstrap classloader
     * @return set of now-installed Listeners
     */
    static List<Listener> initListeners(String listenerClassNames, boolean bootstrap) throws Exception {
        List<Listener> addedListeners = new LinkedList<>();
        if (listenerClassNames != null) {
            String[] classNames = listenerClassNames.trim().split("\\s");
            for (String className : classNames) {
                if (LogManager.isDebugEnabled()) {
                    log.debug("DiSCo(Core) attempting to add Listener from plugin using class: " + className);
                }
                try {
                    Class<?> clazz = classForName(className.trim(), bootstrap);
                    if (Listener.class.isAssignableFrom(clazz)) {
                        Listener listener = (Listener) clazz.newInstance();
                        EventBus.addListener(listener);
                        addedListeners.add(listener);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("DiSCo(Core) failed to instantiate Listener: " + className);
                }
            }
        }

        return addedListeners;
    }

    /**
     * Helper method to load a class with the foreknowledge of whether this plugin is being loaded into the bootstrap classloader or not.
     * @param name the name of the class to try and load
     * @param bootstrap true if loading into the bootstrap classloader
     * @return the Class after loading.
     */
    static Class<?> classForName(String name, boolean bootstrap) throws Exception {
        return bootstrap
            ? Class.forName(name, true, null)
            : Class.forName(name, true, ClassLoader.getSystemClassLoader());
    }

}
