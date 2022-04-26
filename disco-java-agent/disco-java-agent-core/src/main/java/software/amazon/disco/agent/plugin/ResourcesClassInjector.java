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

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that injects the raw bytecode of a given class, which has been relocated under /resources of a plugin jar during build time, to a target classloader.
 * <p>
 * The purpose of this injector is to solve the class visibility issue when a class inside a plugin which has been injected to the bootstrap classloader, depends on classes from a SDK or
 * framework that's loaded by a different classloader, most likely a direct child of the bootstrap classloader (e.g. Tomcat and Sprint Boot's classloader's implementations). This is problematic
 * since classes loaded by the bootstrap classloader have no visibility to classes loaded by other classloaders such as the system classloader or any application classloaders causing "ClassDefNotFound"
 * errors at runtime.
 * <p>
 * The implemented solution is to inject these classes into the target classloader, whilst ensuring it is not also visible up the chain of parent classloaders, since classloaders typically have
 * parent-first semantics. i.e. even if we were to extract the class bytes from this class residing in the bootstrap classloader, and use a ByteBuddy ClassInjector to inject this class into the
 * system classloader, the bootstrap instance will still be the one loaded by the runtime - due to parent-first classloader semantics.
 * <p>
 * Consequently, it was elected to move these classes under the directory "resources" of the plugin jar at build time. This effectively shades the namespace of the relocated classes under "resources.",
 * e.g. com.amazon.domain.MyClass becomes resources.com.amazon.domain.MyClass, making them "invisible" to the bootstrap classloader. That being said, assuming that the plugin has been injected to a
 * classloader of choice, {@link ResourcesClassInjector} will query this said classloader, extract the class bytes and use a ByteBuddy ClassInjector to inject this class to the target classloader specified.
 */
public class ResourcesClassInjector {
    private static final Logger log = LogManager.getLogger(ResourcesClassInjector.class);
    private static Map<String, byte[]> injectedDependencies = new HashMap<>();

    /**
     * Inject a class into a target classloader. This assumes that the corresponding file relocation has been performed at build time,
     * which effectively renaming its namespace from package.name.SomeClass into resources.package.name.SomeClass within the plugin Jar file.
     * Thus preventing the class from being resolved by the unintended classloader.
     *
     * @param targetClassLoader the classloader in which to inject this class.
     * @param sourceClassloader the classloader which loaded the byte code of the class to be injected.
     * @param className         fully qualified real class name (eg. package.name.SomeClass).
     */
    public static void injectClass(final ClassLoader targetClassLoader, final ClassLoader sourceClassloader, final String className) {
        try {
            final byte[] bytes = ClassFileLocator.ForClassLoader.of(sourceClassloader)
                    .locate("resources." + className)
                    .resolve();

            // add this class to the list of dependencies in case it was already injected as a side-effect of statically
            // instrumenting another package. At runtime, these 2 packages may be loaded by different ClassLoaders that
            // are isolated from each other, therefore the dependency class must be discoverable by both.
            injectedDependencies.put(className.replace('.', '/'), bytes);

            if (!classExistsIn(className, targetClassLoader)) {
                new ClassInjector.UsingUnsafe(targetClassLoader).injectRaw(Collections.singletonMap(className, bytes));
            }
        } catch (Throwable t) {
            final String errorMessage = String.format("Disco(Core) could not inject class: %s in classloader: %s",
                    className == null ? "null class" : className,
                    targetClassLoader == null ? "null classloader" : targetClassLoader.toString()
            );

            log.error(errorMessage, t);
        }
    }

    /**
     * Convenience method to inject an array of classes to a target classloader.
     *
     * @param targetClassLoader the classloader in which to inject this class.
     * @param sourceClassloader the classloader which loaded the byte code of the class to be injected.
     * @param classNames        array of fully qualified class name (eg. package.name.SomeClass).
     */
    public static void injectAllClasses(final ClassLoader targetClassLoader, final ClassLoader sourceClassloader, final String... classNames) {
        if (classNames != null) {
            for (String className : classNames) {
                injectClass(targetClassLoader, sourceClassloader, className);
            }
        }
    }

    /**
     * Query if a classloader already has a definition for the given classname
     *
     * @param className   the FQDN of the class
     * @param classLoader the classloader to query
     * @return true if this classloader already has a visible class of that name
     */
    public static boolean classExistsIn(final String className, final ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Retrieve the collection of dependency classes that must be discoverable by a designated ClassLoader. These classes
     * are normally injected directly during runtime instrumentation, but must be physically saved along side within the
     * package currently being statically instrumented.
     *
     * @return Map of class path to its corresponding bytecode
     */
    public static Map<String, byte[]> getInjectedDependencies() {
        return injectedDependencies;
    }
}
