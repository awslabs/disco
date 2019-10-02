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

package com.amazon.disco.agent.inject;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class Agent {
    /**
     * Alternative entry point for when Agent is loaded after application startup. Manifest must declare the Agent-Class
     * attribute accordingly.
     *
     * @param agentArgs any arguments passed as part of the -javaagent argument string
     * @param instrumentation the Instrumentation object given to every Agent, to transform bytecode
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        try {
            //parse the target Agent JAR from the args
            int indexOfSplitter = agentArgs.indexOf('=');
            String jarPath = agentArgs.substring(0, indexOfSplitter == -1 ? agentArgs.length() : indexOfSplitter);
            String remainingArgs = indexOfSplitter == -1 ? null : agentArgs.substring(indexOfSplitter + 1);

            //add it to bootstrap
            File jarFile = new File(jarPath);
            JarFile jar = new JarFile(jarFile);
            instrumentation.appendToBootstrapClassLoaderSearch(jar);

            //This is a hack to work around what is apparently a deficiency in Java. Adding a new Jar to the
            //bootstrap classloader does not allow the classloader to see the content for RESOURCE loading, only for
            //CLASS loading. The ByteBuddy ClassFileLocator by default find .class file bytecode using getResourceAsStream
            //and so when loading an agent via Injection, this fails. To workaround this, we can force the creation of/
            //the (private) BOOT_LOADER_PROXY field inside of ClassFileLocator.ForClassLoader. We can then add our JAR
            //as a URL to it.
            //In this code we also make a very bold assumption that the bytebuddy classes have been relocated to the
            //com.amazon.disco.agent.jar namespace.
            //See: https://stackoverflow.com/questions/51347432/why-cant-i-load-resources-which-are-appended-to-the-bootstrap-class-loader-sear
            Class classFileLocator = Class.forName("com.amazon.disco.agent.jar.bytebuddy.dynamic.ClassFileLocator$ForClassLoader");
            Field bootLoaderProxyField = classFileLocator.getDeclaredField("BOOT_LOADER_PROXY");
            bootLoaderProxyField.setAccessible(true);
            Object bootLoaderProxy = bootLoaderProxyField.get(null);
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(bootLoaderProxy, jarFile.toURI().toURL());

            //read MANIFEST to ascertain the premain class
            String premainClass = jar.getManifest().getMainAttributes().getValue("Premain-Class");

            //reflectively call its premain, with the remaining args
            Class.forName(premainClass, true, null).getDeclaredMethod("premain", String.class, Instrumentation.class)
                    .invoke(null, remainingArgs, instrumentation);
        } catch (Throwable t) {}
    }
}
