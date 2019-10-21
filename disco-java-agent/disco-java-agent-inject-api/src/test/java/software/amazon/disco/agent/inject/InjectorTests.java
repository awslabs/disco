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

package software.amazon.disco.agent.inject;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;


public class InjectorTests {
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLoadAgent() throws Exception {
        File dummyJarFile = createJar("testLoadAgent");
        //pre-check that the class is not on the bootstrap classloader
        try {
            Class.forName(PremainClass.class.getName(), true, null);
            Assert.fail();
        } catch (ClassNotFoundException c) {
            //we expect this.
        }

        Injector.loadAgent(dummyJarFile.getPath(), null);

        //now check that the class, accessed from the boostrap loader explicitly, has had its method called
        Class<?> premainClass = Class.forName(PremainClass.class.getName(), true, null);
        boolean called = (boolean)premainClass.getDeclaredField("called").get(null);
        Assert.assertTrue(called);
    }

    @Test
    public void testAddToSystemClasspath() throws Exception {
        File dummyJarFile = createJar("testAddToSystemClasspath");
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Injector.addToSystemClasspath(instrumentation, dummyJarFile);
        Mockito.verify(instrumentation).appendToSystemClassLoaderSearch(Mockito.any());
        URL[] urlArray = ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs();
        List<URL> urlList = Arrays.asList(urlArray);
        Set<URL> urls = new HashSet<>(urlList);
        Assert.assertTrue(urls.contains(dummyJarFile.toURI().toURL()));
    }

    @Test
    public void testAddToBootstrapClasspath() throws Exception {
        File dummyJarFile = createJar("testAddToBootstrapClasspath");
        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        Injector.addToBootstrapClasspath(instrumentation, dummyJarFile);

        Class classFileLocator = Class.forName("net.bytebuddy.dynamic.ClassFileLocator$ForClassLoader");
        Field bootLoaderProxyField = classFileLocator.getDeclaredField("BOOT_LOADER_PROXY");
        bootLoaderProxyField.setAccessible(true);

        Method getURLs = URLClassLoader.class.getDeclaredMethod("getURLs");
        URL[] urls = (URL[])getURLs.invoke(bootLoaderProxyField.get(null));

        Mockito.verify(instrumentation).appendToBootstrapClassLoaderSearch(Mockito.any());
        Assert.assertEquals(urls[0], dummyJarFile.toURI().toURL());
    }

    @Test
    public void testAddURL() throws Exception {
        MyURLClassLoader urlClassLoader = new MyURLClassLoader();
        URL url = new URL("file://something");
        Injector.addURL(urlClassLoader, url);
        Assert.assertEquals(url, urlClassLoader.url);
    }

    class MyURLClassLoader extends URLClassLoader {
        public URL url = null;
        public MyURLClassLoader() {
            super(new URL[]{});
        }

        @Override
        public void addURL(URL url) {
            this.url = url;
            super.addURL(url);
        }
    }

    private static File createJar(String name) throws Exception {
        File file = tempFolder.newFile(name + ".jar");
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
                //write a manifest specifying a premain class
                jarOutputStream.putNextEntry(new ZipEntry("META-INF/"));
                jarOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                jarOutputStream.write(("Premain-Class: "+PremainClass.class.getName()+"\n\n").getBytes());
                jarOutputStream.closeEntry();

                //write the PremainClass below, so that we can add it to bootstrap classloader for the loadAgentTest.
                jarOutputStream.putNextEntry(new ZipEntry("software/amazon/disco/agent/inject/"));
                jarOutputStream.putNextEntry(new ZipEntry("software/amazon/disco/agent/inject/InjectorTests$PremainClass.class"));
                jarOutputStream.write(getBytes(PremainClass.class.getName()));
                jarOutputStream.closeEntry();
            }
        }
        return file;
    }

    private static byte[] getBytes(String className) throws Exception {
        return Files.readAllBytes(Paths.get(ClassLoader.getSystemClassLoader().getResource(
                className.replace('.', '/') + ".class"
        ).toURI()));
    }

    public static class PremainClass {
        public static boolean called = false;
        public static void premain(String agentArgs, Instrumentation instrumentation) {
            called = true;
        }
    }
}
