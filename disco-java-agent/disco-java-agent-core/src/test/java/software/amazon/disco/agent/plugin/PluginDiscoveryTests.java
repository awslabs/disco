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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.agent.config.AgentConfig;
import software.amazon.disco.agent.config.AgentConfigParser;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.plugin.source.PluginInit;
import software.amazon.disco.agent.plugin.source.PluginInstallable;
import software.amazon.disco.agent.plugin.source.PluginListener;
import software.amazon.disco.agent.plugin.source.PluginPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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
    Collection<PluginOutcome> pluginOutcomes;

    @Before
    public void before() {
        agentConfig = new AgentConfigParser().parseCommandLine("pluginPath="+tempFolder.getRoot().getAbsolutePath());
        instrumentation = Mockito.mock(Instrumentation.class);
        installables = new HashSet<>();
        pluginOutcomes = Collections.emptyList();
    }

    @After
    public void after() {
        if(!pluginOutcomes.isEmpty()){
            Map<String, PluginOutcome> outcomes = PluginDiscovery.getPluginOutcomes();
            Assert.assertEquals(pluginOutcomes, outcomes.values());
        }
    }

    @Test
    public void testPluginInitClassPluginClassLoader() throws Exception {
        createJar("plugin_with_init",
                "Disco-Classloader: plugin\n" +
                        "Disco-Init-Class: software.amazon.disco.agent.plugin.source.PluginInit",
                "software.amazon.disco.agent.plugin.source.PluginInit");

        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        PluginOutcome outcome = outcomes.iterator().next();

        //now the class was loaded, check its classloader and PluginOutcome
        Assert.assertNotNull(outcome.initClass);
        ClassLoader classLoader = outcome.initClass.getClassLoader();
        Assert.assertTrue(classLoader instanceof PluginClassLoader);
        Assert.assertEquals(ClassLoaderType.PLUGIN, outcome.classLoaderType);

        Assert.assertTrue(canFindClass("java.lang.Thread", classLoader));
        Assert.assertTrue(canFindClass("software.amazon.disco.agent.plugin.source.PluginInit", classLoader));
        Assert.assertFalse(canFindClass("software.amazon.disco.agent.plugin.PluginOutcome", classLoader));
    }

    @Test
    public void testPluginListenerPluginClassLoader() throws Exception {
        File jarFile = createJar("plugin_with_listener",
                "Disco-Classloader: plugin\n" +
                        "Disco-Listener-Classes: software.amazon.disco.agent.plugin.source.PluginListener",
                "software.amazon.disco.agent.plugin.source.PluginListener");
        runTestsForPluginClassLoader(jarFile);
    }

    @Test
    public void testPluginInstallablePluginClassLoader() throws Exception {
        File jarFile = createJar("plugin_with_package",
                "Disco-Classloader: plugin\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginInstallable");
        runTestsForPluginClassLoader(jarFile);
    }

    @Test
    public void testPluginPackageInstallablePluginClassLoader() throws Exception {
        File jarFile = createJar("plugin_with_installable",
                "Disco-Classloader: plugin\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginPackage$OtherInstallable");
        runTestsForPluginClassLoader(jarFile);
    }

    void runTestsForPluginClassLoader(File jarFile) throws Exception {
        // The Disco agent isn't attached for unit tests (as expected). As a result, disco-java-agent-api and disco-java-agent-plugin-api
        // classes aren't located in the bootstrap classloader (but they are located in the system classloader). Since the
        // custom PluginClassLoader has bootstrap as a parent, ClassDefNotFound exceptions occur when trying to call findClass
        // for these classes (as Listeners and Installables) aren't defined. Therefore, this test will ensure the right classloader
        // is used, and that the JAR does indeed exist as a URL in this classloader.
        ClassLoader classLoader = PluginDiscovery.loadJar(instrumentation, jarFile, ClassLoaderType.PLUGIN);
        Assert.assertTrue(classLoader instanceof PluginClassLoader);
        Assert.assertEquals(((URLClassLoader)classLoader).getURLs()[0], jarFile.toURI().toURL());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToSystemClassLoaderSearch(Mockito.any());
    }

    @Test
    public void testPluginInitClassBootstrapClassLoader() throws Exception {
        File jarFile = createJar("plugin_with_init",
                "Disco-Bootstrap-Classloader: true\n" +
                        "Disco-Init-Class: software.amazon.disco.agent.plugin.source.PluginInit",
                "software.amazon.disco.agent.plugin.source.PluginInit");
        ClassLoader classLoader = runTestsForBootstrapClassLoader(jarFile);
        Assert.assertTrue(canFindClass("java.lang.Thread", classLoader));
        Assert.assertFalse(canFindClass("software.amazon.disco.agent.plugin.PluginOutcome", classLoader));
    }

    @Test
    public void testPluginListenerBootstrapClassLoader() throws Exception {
        File jarFile = createJar("plugin_with_listener",
                "Disco-Bootstrap-Classloader: true\n" +
                        "Disco-Listener-Classes: software.amazon.disco.agent.plugin.source.PluginListener",
                "software.amazon.disco.agent.plugin.source.PluginListener");
        runTestsForBootstrapClassLoader(jarFile);
    }

    @Test
    public void testPluginInstallableBootstrapClassLoader() throws Exception {
        File jarFile = createJar("plugin_with_package",
                "Disco-Bootstrap-Classloader: true\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginInstallable");
        runTestsForBootstrapClassLoader(jarFile);
    }

    @Test
    public void testPluginPackageInstallableBootstrapClassLoader() throws Exception {
        File jarFile = createJar("plugin_with_installable",
                "Disco-Bootstrap-Classloader: true\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginPackage$OtherInstallable");
        runTestsForBootstrapClassLoader(jarFile);
    }

    ClassLoader runTestsForBootstrapClassLoader(File jarFile) throws Exception {
        // The Disco agent isn't attached for unit tests (as expected). As a result, disco-java-agent-api and disco-java-agent-plugin-api
        // classes aren't located in the bootstrap classloader (but they are located in the system classloader). Therefore,
        // ClassDefNotFound exceptions occur when trying to call findClass for these classes (as Listeners and Installables)
        // aren't defined. Therefore, this test will ensure the right classloader is used, and that the JAR was added to this classloader.
        ClassLoader classLoader = PluginDiscovery.loadJar(instrumentation, jarFile, ClassLoaderType.BOOTSTRAP);
        Assert.assertFalse(classLoader instanceof PluginClassLoader);
        Mockito.verify(instrumentation, Mockito.times(1)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToSystemClassLoaderSearch(Mockito.any());
        return classLoader;
    }

    @Test
    public void testPluginInitClassSystemClassLoader() throws Exception {
        createJar("plugin_with_init",
                "Disco-Classloader: system\n" +
                        "Disco-Init-Class: software.amazon.disco.agent.plugin.source.PluginInit",
                "software.amazon.disco.agent.plugin.source.PluginInit");

        //this plugin is not requesting bootstrap classloader loading. Since it is part of our test code, it is *already*
        //on the system classpath, so the best we can do is a mockito verify that it was in principle added to this classpath,
        //while also asserting that it was not already used
        Assert.assertFalse(PluginInit.initCalled);
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        PluginOutcome outcome = outcomes.iterator().next();
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        //now the class was loaded, and its static init called
        Assert.assertTrue(PluginInit.initCalled);
        ClassLoader classLoader = outcome.initClass.getClassLoader();
        Assert.assertFalse(classLoader instanceof PluginClassLoader);
        Assert.assertEquals(ClassLoaderType.SYSTEM, outcomes.iterator().next().classLoaderType);

        Assert.assertTrue(canFindClass("java.lang.Thread", classLoader));
        Assert.assertTrue(canFindClass("software.amazon.disco.agent.plugin.source.PluginInit", classLoader));
        Assert.assertTrue(canFindClass("software.amazon.disco.agent.plugin.PluginOutcome", classLoader));
    }

    @Test
    public void testPluginListenerSystemClassLoader() throws Exception {
        createJar("plugin_with_listener",
                "Disco-Classloader: system\n" +
                        "Disco-Listener-Classes: software.amazon.disco.agent.plugin.source.PluginListener",
                "software.amazon.disco.agent.plugin.source.PluginListener");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Event event = Mockito.mock(Event.class);
        EventBus.publish(event);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        PluginOutcome outcome = outcomes.iterator().next();
        Assert.assertFalse(outcome.listeners.get(0).getClass().getClassLoader() instanceof PluginClassLoader);
        Assert.assertEquals(event, ((PluginListener)outcome.listeners.get(0)).events.get(0));
        Assert.assertEquals(ClassLoaderType.SYSTEM, outcomes.iterator().next().classLoaderType);
    }

    @Test
    public void testPluginInstallableSystemClassLoader() throws Exception {
        createJar("plugin_with_package",
                "Disco-Classloader: system\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginInstallable");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        PluginOutcome outcome = outcomes.iterator().next();
        Installable installable = outcome.installables.get(0);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Assert.assertFalse(installable.getClass().getClassLoader() instanceof PluginClassLoader);
        Assert.assertTrue(installables.contains(installable));
        Assert.assertEquals(ClassLoaderType.SYSTEM, outcomes.iterator().next().classLoaderType);
    }

    @Test
    public void testPluginPackageInstallableSystemClassLoader() throws Exception {
        createJar("plugin_with_installable",
                "Disco-Classloader: system\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginPackage$OtherInstallable");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        PluginOutcome outcome = outcomes.iterator().next();
        Installable installable1 = outcome.installables.get(0);
        Installable installable2 = outcome.installables.get(1);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertFalse(installable1.getClass().getClassLoader() instanceof PluginClassLoader);
        Assert.assertFalse(installable2.getClass().getClassLoader() instanceof PluginClassLoader);
        Assert.assertTrue(installables.contains(installable1));
        Assert.assertTrue(installables.contains(installable2));
        Assert.assertEquals(ClassLoaderType.SYSTEM, outcomes.iterator().next().classLoaderType);
        Set<Class> classes = new HashSet<>();
        classes.add(installable1.getClass());
        classes.add(installable2.getClass());
        Assert.assertTrue(classes.contains(PluginInstallable.class));
        Assert.assertTrue(classes.contains(PluginPackage.OtherInstallable.class));
    }

    @Test
    public void testPluginInitClassInvalidClassLoader() throws Exception {
        createJar("plugin_with_init",
                "Disco-Classloader: invalid\n" +
                        "Disco-Init-Class: software.amazon.disco.agent.plugin.source.PluginInit",
                "software.amazon.disco.agent.plugin.source.PluginInit");

        Assert.assertFalse(PluginInit.initCalled);
        runTestsForInvalidClassLoaderType();
        //static init should not be called
        Assert.assertFalse(PluginInit.initCalled);
    }

    @Test
    public void testPluginListenerInvalidClassLoader() throws Exception {
        createJar("plugin_with_listener",
                "Disco-Classloader: invalid\n" +
                        "Disco-Listener-Classes: software.amazon.disco.agent.plugin.source.PluginListener",
                "software.amazon.disco.agent.plugin.source.PluginListener");
        runTestsForInvalidClassLoaderType();
    }

    @Test
    public void testPluginInstallableInvalidClassLoader() throws Exception {
        createJar("plugin_with_package",
                "Disco-Classloader: invalid\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginInstallable");
        runTestsForInvalidClassLoaderType();
    }

    @Test
    public void testPluginPackageInstallableInvalidClassLoader() throws Exception {
        createJar("plugin_with_installable",
                "Disco-Classloader: invalid\n" +
                        "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginPackage$OtherInstallable");
        runTestsForInvalidClassLoaderType();
    }

    void runTestsForInvalidClassLoaderType() {
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verify(instrumentation, Mockito.times(0)).appendToSystemClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        PluginOutcome outcome = outcomes.iterator().next();
        Assert.assertEquals(outcome.classLoaderType, ClassLoaderType.INVALID);
    }

    boolean canFindClass(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, true, classLoader);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Test
    public void testPluginInstallableRuntimeOnly() throws Exception {
        agentConfig = new AgentConfigParser().parseCommandLine("runtimeOnly");
        createJar("plugin_runtime_only",
                "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginInstallable");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Assert.assertTrue(outcomes.isEmpty());
        Assert.assertTrue(installables.isEmpty());
        Mockito.verifyNoInteractions(instrumentation);
    }

    @Test
    public void testPluginPackageInstallableRuntimeOnly() throws Exception {
        agentConfig = new AgentConfigParser().parseCommandLine("runtimeOnly");
        createJar("plugin_package_runtime_only",
                "Disco-Installable-Classes: software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginPackage",
                "software.amazon.disco.agent.plugin.source.PluginInstallable",
                "software.amazon.disco.agent.plugin.source.PluginPackage$OtherInstallable");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Assert.assertTrue(outcomes.isEmpty());
        Assert.assertTrue(installables.isEmpty());
        Mockito.verifyNoInteractions(instrumentation);
    }

    @Test
    public void testPluginBootstrapFlagTrue() throws Exception {
        createJar("plugin_with_bootstrap_true",
                "Disco-Bootstrap-Classloader: true");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verify(instrumentation, Mockito.times(1)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertEquals(ClassLoaderType.BOOTSTRAP, outcomes.iterator().next().classLoaderType);
    }

    @Test
    public void testPluginBootstrapFlagFalse() throws Exception {
        createJar("plugin_with_bootstrap_false",
                "Disco-Bootstrap-Classloader: false");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(1)).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertEquals(ClassLoaderType.SYSTEM, outcomes.iterator().next().classLoaderType);
    }

    @Test
    public void testPluginClassloaderFlagBootstrap() throws Exception {
        createJar("plugin_with_classloader_bootstrap",
                "Disco-Classloader: bootstrap");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verify(instrumentation, Mockito.times(1)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertEquals(ClassLoaderType.BOOTSTRAP, outcomes.iterator().next().classLoaderType);
    }

    @Test
    public void testPluginClassloaderFlagPlugin() throws Exception {
        File jarFile = createJar("plugin_with_classloader_plugin",
                "Disco-Classloader: plugin");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        JarFile jar = new JarFile(jarFile);
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertEquals(ClassLoaderType.PLUGIN, outcomes.iterator().next().classLoaderType);
    }

    @Test
    public void testPluginClassloaderFlagInvalid() throws Exception {
        createJar("plugin_with_classloader_invalid",
                "Disco-Classloader: foobar");
        Collection<PluginOutcome> outcomes = scanAndApply(instrumentation, agentConfig);
        Mockito.verify(instrumentation, Mockito.times(0)).appendToBootstrapClassLoaderSearch(Mockito.any());
        Mockito.verify(instrumentation, Mockito.times(0)).appendToSystemClassLoaderSearch(Mockito.any());
        Assert.assertEquals(ClassLoaderType.INVALID, outcomes.iterator().next().classLoaderType);
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
        pluginOutcomes = PluginDiscovery.apply();

        return pluginOutcomes;
    }

    private File createJar(String name, String manifestContent, String... classNames) throws Exception {
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
                        String classFull = className.replace('.', '/') + ".class";
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
        return file;
    }

    private static byte[] getBytes(String className) throws Exception {
        return Files.readAllBytes(Paths.get(ClassLoader.getSystemClassLoader().getResource(
                className
        ).toURI()));
    }
}
