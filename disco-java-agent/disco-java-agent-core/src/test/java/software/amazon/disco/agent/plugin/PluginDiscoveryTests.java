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
import software.amazon.disco.agent.config.AgentConfigParser;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.plugin.source.PluginInit;
import software.amazon.disco.agent.plugin.source.PluginInstallable;
import software.amazon.disco.agent.plugin.source.PluginListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.agent.plugin.source.PluginPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class PluginDiscoveryTests {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private AgentConfig agentConfig;
    private Instrumentation instrumentation;
    Set<Installable> installables;

    @Before
    public void before() {
        agentConfig = new AgentConfigParser().parseCommandLine("pluginPath="+tempFolder.getRoot().getAbsolutePath());
        instrumentation = Mockito.mock(Instrumentation.class);
        installables = new HashSet<>();
    }

    @Test
    public void testPluginInitClassNonBootstrap() throws Exception {
        createJar("plugin_with_init",
                "Disco-Init-Class: software.amazon.disco.agent.plugin.source.PluginInit",
                "software.amazon.disco.agent.plugin.source.PluginInit");

        //this plugin is not requesting bootstrap classloader loading. Since it is part of our test code, it is *already*
        //on the system classpath, so the best we can do is a mockito verify that it was in principle added to this classpath,
        //while also asserting that it was not already used
        Assert.assertFalse(PluginInit.initCalled);
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());

        //now the class was loaded, and its static init called
        Assert.assertTrue(PluginInit.initCalled);
        Assert.assertFalse(outcomes.iterator().next().bootstrap);
    }

    @Test
    public void testPluginListenerNonBootstrap() throws Exception {
        createJar("plugin_with_listener",
                "Disco-Listener-Classes: software.amazon.disco.agent.plugin.source.PluginListener",
                "software.amazon.disco.agent.plugin.source.PluginListener");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Event event = Mockito.mock(Event.class);
        EventBus.publish(event);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        PluginOutcome outcome = outcomes.iterator().next();
        Assert.assertEquals(event, ((PluginListener)outcome.listeners.get(0)).events.get(0));
        Assert.assertFalse(outcome.bootstrap);
    }

    @Test
    public void testPluginInstallableNonBootstrap() throws Exception {
        createJar("plugin_with_package",
                "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginInstallable");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        PluginOutcome outcome = outcomes.iterator().next();
        Installable installable = outcome.installables.get(0);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertTrue(installables.contains(installable));
        Assert.assertFalse(outcome.bootstrap);
    }

    @Test
    public void testPluginPackageInstallableNonBootstrap() throws Exception {
        createJar("plugin_with_installable",
                "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginPackage$OtherInstallable");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        PluginOutcome outcome = outcomes.iterator().next();
        Installable installable1 = outcome.installables.get(0);
        Installable installable2 = outcome.installables.get(1);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertTrue(installables.contains(installable1));
        Assert.assertTrue(installables.contains(installable2));
        Assert.assertFalse(outcome.bootstrap);
        Set<Class> classes = new HashSet<>();
        classes.add(installable1.getClass());
        classes.add(installable2.getClass());
        Assert.assertTrue(classes.contains(PluginInstallable.class));
        Assert.assertTrue(classes.contains(PluginPackage.OtherInstallable.class));
    }

    @Test
    public void testPluginBootstrapFlag() throws Exception {
        createJar("plugin_with_bootstrap_true",
                "Disco-Bootstrap-Classloader: true");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verify(instrumentation).appendToBootstrapClassLoaderSearch(Mockito.any());
        Assert.assertTrue(outcomes.iterator().next().bootstrap);
    }

    @Test
    public void testJarWithoutManifestSafelySkipped() throws Exception {
        createJar("jar_without_manifest", null);
        JarFile jar = new JarFile(agentConfig.getPluginPath() + "/jar_without_manifest.jar");
        Assert.assertNull(jar.getManifest());
        jar.close();
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verifyNoInteractions(instrumentation);
        Assert.assertTrue(outcomes.isEmpty());

    }

    @Test
    public void testManifestWithoutMainAttributesSafelySkipped() throws Exception {
        createJar("jar_without_main_attributes", "");
        JarFile jar = new JarFile(agentConfig.getPluginPath() + "/jar_without_main_attributes.jar");
        Assert.assertTrue(jar.getManifest().getMainAttributes().isEmpty());
        jar.close();
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verifyNoInteractions(instrumentation);
        Assert.assertTrue(outcomes.isEmpty());
    }

    @Test
    public void testManifestWithoutDiscoAttributesSafelySkipped() throws Exception {
        createJar("jar_without_disco_attributes", "Foobar: boofar");
        JarFile jar = new JarFile(agentConfig.getPluginPath() + "/jar_without_disco_attributes.jar");
        Assert.assertEquals("boofar", jar.getManifest().getMainAttributes().getValue("Foobar"));
        jar.close();
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verifyNoInteractions(instrumentation);
        Assert.assertTrue(outcomes.isEmpty());
    }

    @Test
    public void testSplitString() {
        String[] result = PluginDiscovery.splitString("    com.foo.Foo        com.foo.Bar     ");
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("com.foo.Foo", result[0]);
        Assert.assertEquals("com.foo.Bar", result[1]);
    }

    private Collection<PluginOutcome> scanAndApply(Instrumentation instrumentation, AgentConfig agentConfig) {
        PluginDiscovery.scan(instrumentation, agentConfig);
        installables.addAll(PluginDiscovery.processInstallables());
        return PluginDiscovery.apply();
    }

    private void createJar(String name, String manifestContent, String... classNames) throws Exception {
        File file = tempFolder.newFile(name + ".jar");
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {

                //write manifest
                if (manifestContent != null) {
                    jarOutputStream.putNextEntry(new ZipEntry("META-INF/"));
                    jarOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                    jarOutputStream.write((manifestContent + "\n\n").getBytes());
                    jarOutputStream.closeEntry();
                }

                //write class file
                if (classNames != null) {
                    for (String className: classNames) {
                        String classFull = className.replace('.', '/');
                        String classPackage = classFull.substring(0, classFull.indexOf('/')) + "/";
                        try {
                            jarOutputStream.putNextEntry(new ZipEntry(classPackage));
                        } catch (IOException e) {
                            //swallow, if this occurred due to creating an already-present folder
                        }
                        jarOutputStream.putNextEntry(new ZipEntry(classFull));
                        jarOutputStream.write(getBytes(classFull));
                        jarOutputStream.closeEntry();
                    }
                }
            }
        }
    }

    private static byte[] getBytes(String className) throws Exception {
        return Files.readAllBytes(Paths.get(ClassLoader.getSystemClassLoader().getResource(
                className.replace('.', '/') + ".class"
        ).toURI()));
    }
}
