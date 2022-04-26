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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class PluginClassLoaderTests {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private PluginClassLoader pluginClassLoader;

    @Before
    public void before() {
        pluginClassLoader = new PluginClassLoader();
    }

    @Test
    public void testPluginClassLoaderParentIsBootstrap() {
        Assert.assertNull(pluginClassLoader.getParent());
    }

    @Test
    public void testClassForNameThrowsClassNotFoundException() {
        try {
            Class clazz = Class.forName("software.amazon.disco.agent.plugin.PluginOutcome", true, pluginClassLoader);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ClassNotFoundException);
        }
    }

    @Test
    public void testClassForName() throws Exception {
        File jarFile = createJar("plugin_with_init",
                "Disco-Classloader: invalid\n" +
                        "Disco-Init-Class: software.amazon.disco.agent.plugin.source.PluginInit",
                "software.amazon.disco.agent.plugin.source.PluginInit");
        addURL(pluginClassLoader, jarFile.toURI().toURL());
        try {
            Class clazz = pluginClassLoader.loadClass("software.amazon.disco.agent.plugin.source.PluginInit");
            Assert.assertNotNull(clazz);
        } catch (Exception e) {
            Assert.fail();
        }
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

    private void addURL(PluginClassLoader classLoader, URL url) throws Exception {
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(classLoader, url);
    }
}
