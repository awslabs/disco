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
import software.amazon.disco.agent.interception.Package;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
 * Disco-Installable-Classes: a space-separated list of fully qualified class names which are expected to inherit from either Installable or Package
 *                            and have a no-args constructor. Installables will be processed first, across all scanned plugins
 * Disco-Init-Class: if any further one-off-initialization is required, a fully qualified class may be provided. If this class provides a method
 *                  matching the signature "public static void init(void)", that method will be executed. All plugins will have this init()
 *                  method processed *after* all plugins have had their Installables processed.
 * Disco-Listener-Classes: a space-separated list of fully qualified class names which are expected to inherit from Listener
 *                         and have a no-args constructor. Listener registration for all plugins will occure after one-off initialization for all plugins
 * Disco-Bootstrap-Classloader: referenced in the event the 'Disco-Classloader' attribute is absent. if set to the literal
 *                              case-insensitive string 'true', this JAR file will be added to the runtime's bootstrap
 *                              classloader. Any other value, or the absence of this attribute, means the plugin will be loaded
 *                              via a dedicated classloader. It is not usually necessary to specify this attribute,
 *                              unless Installables wish to intercept JDK classes.
 * Disco-Classloader: allowable values include "bootstrap", "system", and "plugin".
 *                    if set to the literal case-insensitive string 'system', this JAR file will be added to the runtime's system
 *                    classloader. If set to the literal case-insensitive string 'bootstrap', this JAR file will be added to the
 *                    bootstrap classloader. If otherwise set to the literal case-insensitive string 'plugin', this JAR
 *                    file will be added to a dedicated PluginClassLoader. Any other will result in failure to load the plugin.
 *                    The absence of this attribute will defer to the Disco-Bootstrap-Classloader attribute for
 *                    determining the classloader to use.
 * At startup, the decoupled disco agent searches the Config's pluginPath for such JAR files, and loads the plugins according
 * to the Manifest's content.
 */
public class PluginDiscovery {
    private static final Logger log = LogManager.getLogger(PluginDiscovery.class);

    //results of the scan()
    static class ClassInfo {
        final String pluginName;
        final Class<?> clazz;
        final ClassLoaderType classLoaderType;

        ClassInfo(String pluginName, Class<?> clazz, ClassLoaderType classLoaderType) {
            this.pluginName = pluginName;
            this.clazz = clazz;
            this.classLoaderType = classLoaderType;
        }
    }
    private static List<ClassInfo> installableClasses;
    private static List<ClassInfo> initClasses;
    private static List<ClassInfo> listenerClasses;
    private static Map<String, PluginOutcome> pluginOutcomes;

    /**
     * Entry point for plugin discovery. The lifetime of the system begins with a call to scan(), after which point any
     * discovered Installables may be instantiated and collected via processInstallables, and finally the Init methods
     * and Listeners are applied via apply(). DiscoAgentTemplate manages this sequence of calls, and it would be unusual for
     * any other code to call these methods directly.
     * @param instrumentation and instrumentation instance, used to add discovered plugins to classpaths
     * @param config an AgentConfig supplying information about whether plugin discovery should be attempted, and if
     *               so which path to scan.
     */
    public static void scan(Instrumentation instrumentation, AgentConfig config) {
        installableClasses = new LinkedList<>();
        initClasses = new LinkedList<>();
        listenerClasses = new LinkedList<>();
        pluginOutcomes = new HashMap<>();

        try {
            if (config.getPluginPath() == null) {
                log.info("DiSCo(Core) no plugin path specified, skipping plugin scan");
                return;
            }

            File pluginDir = new File(config.getPluginPath());
            if (!pluginDir.isDirectory()) {
                log.warn("DiSCo(Core) invalid plugin path specified, skipping plugin scan");
                return;
            }

            File[] files = pluginDir.listFiles();
            if (files != null) {
                for (File jarFile : files) {
                    if (jarFile.getName().substring(jarFile.getName().lastIndexOf(".")).equalsIgnoreCase(".jar")) {
                        processJarFile(instrumentation, jarFile, config.isRuntimeOnly());
                    } else {
                        //ignore non JAR file
                        log.info("DiSCo(Core) non JAR file found on plugin path, skipping this file");
                    }
                }
            }
        } catch (Throwable t) {
            //safely do nothing
            log.error("DiSCo(Core) error while processing plugins", t);
        }
    }

    /**
     * Must be called after scan() and before apply(). Discovered Installables are instantiated, and returned such that
     * they may be passed to an InterceptionInstaller.
     * @return a collection of discovered, but not-yet-installed, Installables
     */
    public static Set<Installable> processInstallables() {
        Set<Installable> installables = new HashSet<>();
        if (installableClasses != null && !installableClasses.isEmpty()) {
            for (ClassInfo info : installableClasses) {
                if (Installable.class.isAssignableFrom(info.clazz)) {
                    try {
                        Installable installable = (Installable)info.clazz.getDeclaredConstructor().newInstance();
                        installables.add(installable);
                        pluginOutcomes.get(info.pluginName).installables.add(installable);
                    } catch (Exception e) {
                        log.warn("DiSCo(Core) could not instantiate Installable " + info.clazz.getName(), e);
                    }
                } else if (Package.class.isAssignableFrom(info.clazz)) {
                    try {
                        Package pkg = (Package)info.clazz.getDeclaredConstructor().newInstance();
                        Collection<Installable> pkgInstallables = pkg.get();
                        installables.addAll(pkgInstallables);
                        pluginOutcomes.get(info.pluginName).installables.addAll(pkgInstallables);
                    } catch (Exception e) {
                        log.warn("DiSCo(Core) could not instantiate Package " + info.clazz.getName(), e);
                    }
                }
            }
        }
        return installables;
    }

    /**
     * The final stage in PluginDiscovery lifetime, the apply() method will call all plugins' init methods, followed by
     * registering all their Listeners with the EventBus
     * @return a collection of PluginOutcomes to describe all actions that were taken by the plugin subsystem, for debugging
     * and information.
     */
    public static Collection<PluginOutcome> apply() {
        if (initClasses != null && !initClasses.isEmpty()) {
            for (ClassInfo info : initClasses) {
                try {
                    info.clazz.getDeclaredMethod("init").invoke(null);
                    pluginOutcomes.get(info.pluginName).initClass = info.clazz;
                } catch (Exception e) {
                    log.warn("DiSCo(Core) could not process the init() method of " + info.clazz.getName(), e);
                }
            }
        }

        if (listenerClasses != null && !listenerClasses.isEmpty()) {
            for (ClassInfo info : listenerClasses) {
                try {
                    Listener listener = (Listener) info.clazz.getDeclaredConstructor().newInstance();
                    EventBus.addListener(listener);
                    pluginOutcomes.get(info.pluginName).listeners.add(listener);
                } catch (Exception e) {
                    log.warn("DiSCo(Core) could not add the Listener " + info.clazz.getName(), e);
                }
            }
        }

        return pluginOutcomes.values();
    }

    /**
     * Getter for the pluginOutcomes map.
     * @return a map with key as the plugin name and value as its corresponding PluginOutcome object.
     */
    public static Map<String, PluginOutcome> getPluginOutcomes(){
        return pluginOutcomes;
    }

    /**
     * Process a single JAR file which is assumed to be a plugin
     * @param instrumentation and instrumentation instance, used to add discovered plugins to classpaths
     * @param jarFile the jar file to be processed
     * @param runtimeOnly if the Agent is configured as runtime only, Installables will not be considered from plugins. Init plugins and Listener plugins are unaffected.
     * @throws Exception class reflection or file i/o errors may occur
     */
    static void processJarFile(Instrumentation instrumentation, File jarFile, boolean runtimeOnly) throws Exception {
        final Attributes attributes = validateDiscoPlugin(jarFile);
        if (attributes == null) {
            log.info("DiSCo(Core) JAR file is not a valid Disco plugin, skipping this file");
            return;
        }

        //read each pertinent Manifest attribute
        String initClassName = attributes.getValue("Disco-Init-Class");
        String installableClassNames = attributes.getValue("Disco-Installable-Classes");
        String listenerClassNames = attributes.getValue("Disco-Listener-Classes");
        String bootstrapClassloaderAttribute = attributes.getValue("Disco-Bootstrap-Classloader");
        String classLoaderAttribute = attributes.getValue("Disco-Classloader");

        //process the plugin based on the Manifest
        String pluginName = jarFile.getName();
        pluginOutcomes.put(pluginName, new PluginOutcome(pluginName));
        ClassLoaderType classLoaderType = getClassLoaderFromAttributes(bootstrapClassloaderAttribute, classLoaderAttribute);
        pluginOutcomes.get(pluginName).classLoaderType = classLoaderType;
        if (classLoaderType.equals(ClassLoaderType.INVALID)) {
            //this JAR provided an invalid input for 'Disco-Classloader' and thus should not be loaded
            return;
        }
        ClassLoader classLoader = loadJar(instrumentation, jarFile, classLoaderType);
        processInitClass(pluginName, initClassName, classLoader, classLoaderType);
        processInstallableClasses(pluginName, installableClassNames, classLoader, runtimeOnly, classLoaderType);
        processListenerClasses(pluginName, listenerClassNames, classLoader, classLoaderType);
    }

    /**
     * Verify whether the provided file is a Disco plugin Jar by inspecting its manifest if applicable.
     *
     * @param file Jar file to be verified
     * @return Attributes extracted from the Disco plugin Jar if Disco plugin, null otherwise
     * @throws IOException exception thrown while parsing the Jar file.
     */
    public static Attributes validateDiscoPlugin(final File file) throws Exception {
        final JarFile jar = new JarFile(file);
        final Manifest manifest = jar.getManifest();
        jar.close();
        if (manifest == null) {
            log.info("DiSCo(Core) JAR file without manifest found on plugin path, skipping this file");
            return null;
        }

        Attributes attributes = manifest.getMainAttributes();
        if (attributes == null || attributes.isEmpty()) {
            log.info("DiSCo(Core) JAR file found with manifest without any main attributes, skipping this file");
            return null;
        }

        //check that at least one of the attributes is present
        final boolean isPlugin = attributes.getValue("Disco-Init-Class") != null
            || attributes.getValue("Disco-Installable-Classes") != null
            || attributes.getValue("Disco-Listener-Classes") != null
            || attributes.getValue("Disco-Bootstrap-Classloader") != null
            || attributes.getValue("Disco-Classloader") != null;

        return isPlugin ? attributes : null;
    }

    /**
     * Get the ClassLoaderType from the provided Manifest attributes
     * @param bootstrapClassLoader the content of the Disco-Bootstrap-Classloader manifest attribute
     * @param classLoaderAttribute the content of the Disco-Classloader manifest attribute
     * @return The ClassLoaderType representing the ClassLoader the plugin will be loaded from
     */
    static ClassLoaderType getClassLoaderFromAttributes(String bootstrapClassLoader, String classLoaderAttribute) {
        if (classLoaderAttribute != null) {
            classLoaderAttribute = classLoaderAttribute.trim().toUpperCase();
            try {
                return ClassLoaderType.valueOf(classLoaderAttribute);
            } catch (IllegalArgumentException e) {
                // unrecognized value for the attribute
                return ClassLoaderType.INVALID;
            }
        } else if (bootstrapClassLoader != null) {
            if (bootstrapClassLoader.trim().equalsIgnoreCase("true")) {
                return ClassLoaderType.BOOTSTRAP;
            }
        }
        // TODO: The below should be replaced with ClassLoadType.PLUGIN after plugins have stopped using classes only found in the system classloader
        return ClassLoaderType.SYSTEM;
    }

    /**
     * Having discovered a plugin JAR, add it to the classloader as specified in its Manifest
     * @param instrumentation and instrumentation instance, used to add discovered plugins to classpaths
     * @param jarFile the jar file to be processed
     * @param classLoaderType the classloader to load the plugin into
     * @return The ClassLoader in which the plugin JAR was added to
     * @throws Exception JAR file processing may produce I/O exceptions
     */
    static ClassLoader loadJar(Instrumentation instrumentation, File jarFile, ClassLoaderType classLoaderType) throws Exception {
        JarFile jar = null;
        ClassLoader classLoader = null;
        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(Core) attempting to load JAR file into " + classLoaderType.name() + " classloader: " + jarFile.getName());
        }
        switch (classLoaderType) {
            case SYSTEM:
                jar = Injector.addToSystemClasspath(instrumentation, jarFile);
                classLoader = ClassLoader.getSystemClassLoader();
                break;
            case BOOTSTRAP:
                jar = Injector.addToBootstrapClasspath(instrumentation, jarFile);
                classLoader = null;
                break;
            case PLUGIN:
                PluginClassLoader pluginClassLoader = new PluginClassLoader();
                jar = Injector.addToURLClassLoaderClasspath(pluginClassLoader, jarFile);
                classLoader = pluginClassLoader;
                break;
        }
        if (jar != null) {
            jar.close();
        }

        return classLoader;
    }

    /**
     * Helper method to discover the Class specified for initialization via the init() static method
     * @param pluginName the name of the plugin JAR file where the init class is defined
     * @param initClassName the name of the init class determined from the Manifest
     * @param classLoader the classloader the plugin was loaded by
     * @param classLoaderType the ClassLoaderType representing the classloader that loaded this plugin
     * @throws Exception reflection errors may occur if the class cannot be found
     */
    static void processInitClass(String pluginName, String initClassName, ClassLoader classLoader, ClassLoaderType classLoaderType) throws Exception {
        if (initClassName != null) {
            Class<?> clazz = classForName(initClassName.trim(), classLoader);
            ClassInfo initInfo = new ClassInfo(pluginName, clazz, classLoaderType);
            initClasses.add(initInfo);
        }
    }

    /**
     * Helper method to discover the Classes specified for Installables in the plugin
     * @param pluginName the name of the plugin JAR file where the classes are defined
     * @param installableClassNames the names of the Installable or Package classes determined from the Manifest
     * @param classLoader the classloader the plugin was loaded by
     * @param runtimeOnly true if the agent is configured to be runtime only, thus no Installables will be used for instrumentation.
     * @param classLoaderType the ClassLoaderType representing the classloader that loaded this plugin
     * @throws Exception reflection errors may occur if the class cannot be found
     */
    static void processInstallableClasses(String pluginName, String installableClassNames, ClassLoader classLoader, boolean runtimeOnly, ClassLoaderType classLoaderType) throws Exception {
        if (installableClassNames != null) {
            String[] classNames = splitString(installableClassNames);
            for (String className: classNames) {
                if (runtimeOnly) {
                    log.info("DiSCo(Core) Installable/Package declared in plugin will be ignored because agent is configured as runtime only: " + className);
                    continue;
                }

                try {
                    Class<?> clazz = classForName(className.trim(), classLoader);
                    if (Installable.class.isAssignableFrom(clazz) || Package.class.isAssignableFrom(clazz)) {
                        ClassInfo installableInfo = new ClassInfo(pluginName, clazz, classLoaderType);
                        installableClasses.add(installableInfo);
                    } else {
                        log.warn("DiSCo(Core) specified Installable is not an instance of Installable or Package: " + className);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("DiSCo(Core) cannot locate Installable: " + className, e);
                }
            }
        }
    }

    /**
     * Helper method to discover the Classes specified to be Listeners in the Plugin.
     * @param pluginName the name of the plugin JAR file where the classes are defined
     * @param listenerClassNames the names of the Listener classes determined from the Manifest
     * @param classLoader the classloader the plugin was loaded by
     * @param classLoaderType the ClassLoaderType representing the classloader that loaded this plugin
     * @throws Exception reflection errors may occur if the class cannot be found
     */
    static void processListenerClasses(String pluginName, String listenerClassNames, ClassLoader classLoader, ClassLoaderType classLoaderType) throws Exception {
        if (listenerClassNames != null) {
            String[] classNames = splitString(listenerClassNames);
            for (String className : classNames) {
                if (LogManager.isDebugEnabled()) {
                    log.debug("DiSCo(Core) attempting to add Listener from plugin using class: " + className);
                }
                try {
                    Class<?> clazz = classForName(className.trim(), classLoader);
                    if (Listener.class.isAssignableFrom(clazz)) {
                        ClassInfo installableInfo = new ClassInfo(pluginName, clazz, classLoaderType);
                        listenerClasses.add(installableInfo);
                    } else {
                        log.warn("DiSCo(Core) specified Listener is not an instance of Listener: " + className);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("DiSCo(Core) failed to instantiate Listener: " + className, e);
                }
            }
        }
    }

    /**
     * Helper method to load a class with the foreknowledge of the classloader that loaded this plugin.
     * @param name the name of the class to try and load
     * @param classLoader the classloader the plugin was loaded by
     * @return the Class after loading.
     */
    static Class<?> classForName(String name, ClassLoader classLoader) throws Exception {
        return
            //the initialize param is false so that static initializers are not yet called until we really want
            //to instantiate the class
            Class.forName(name, false, classLoader);
    }

    /**
     * Helper method to take a list of items e.g. Disco-Installable-Classes and produce a Collection of the individual entries
     * @param input the String from the manifest e.g. "    com.foo.Foo        com.foo.Bar     "
     * @return the split results e.g. ["com.foo.Foo", "com.foo.Bar"]
     */
    static String[] splitString(String input) {
        return input.trim().split("\\s+");
    }

}
