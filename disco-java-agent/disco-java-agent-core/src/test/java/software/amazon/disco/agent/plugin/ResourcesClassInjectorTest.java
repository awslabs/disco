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

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.disco.agent.inject.Injector;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class ResourcesClassInjectorTest {
    private static final String CLASS_NAME_ALPHA = "software.amazon.disco.agent.plugin.source.ClassToBeInjectedAlpha";
    private static final String CLASS_NAME_BETA = "software.amazon.disco.agent.plugin.source.ClassToBeInjectedBeta";

    @BeforeClass
    public static void beforeClass() {
        Assert.assertTrue(new File("build/tmp/test.jar").exists());

        // mock the action of a plugin containing "ClassToBeInjectedAlpha.class" and "ClassToBeInjectedBeta.class" being injected into the system classloader.
        Injector.addToSystemClasspath(Injector.createInstrumentation(), new File("build/tmp/test.jar"));
    }

    @After
    public void after() {
        ResourcesClassInjector.getInjectedDependencies().clear();
    }

    @Test
    public void testInjectClass() throws Exception {
        MyURLClassLoader classLoader = new MyURLClassLoader();

        // "ClassToBeInjected.class" has been packaged into a jar named test.jar and deleted from /classes/java/test/...
        try {
            Class.forName(CLASS_NAME_ALPHA, false, classLoader);
            Assert.fail();
        } catch (ClassNotFoundException c) {
            // expected
        }

        ResourcesClassInjector.injectClass(classLoader, ClassLoader.getSystemClassLoader(), CLASS_NAME_ALPHA);

        Class injected = Class.forName(CLASS_NAME_ALPHA, false, classLoader);
        Assert.assertTrue((Boolean) injected.getDeclaredField("initialized").get(null));

        Assert.assertEquals(1, ResourcesClassInjector.getInjectedDependencies().size());
        String keyToCheck = CLASS_NAME_ALPHA.replace(".","/");
        Assert.assertTrue(ResourcesClassInjector.getInjectedDependencies().containsKey(keyToCheck));
    }

    @Test
    public void testInjectClassNotThrowExceptionWithInvalidClassName() {
        MyURLClassLoader classLoader = new MyURLClassLoader();
        ResourcesClassInjector.injectClass(classLoader, ClassLoader.getSystemClassLoader(), "made up name");
    }

    @Test
    public void testInjectClassNotThrowExceptionWithInvalidParams() {
        ResourcesClassInjector.injectClass(null, null, null);
        ResourcesClassInjector.injectClass(null, null, "");
    }

    @Test
    public void testInjectAllClasses() throws Exception {
        MyURLClassLoader classLoader = new MyURLClassLoader();

        try {
            Class.forName(CLASS_NAME_ALPHA, false, classLoader);
            Assert.fail();

            Class.forName(CLASS_NAME_BETA, false, classLoader);
            Assert.fail();
        } catch (ClassNotFoundException c) {
            // expected
        }

        ResourcesClassInjector.injectAllClasses(classLoader, ClassLoader.getSystemClassLoader(),
                CLASS_NAME_ALPHA,
                CLASS_NAME_BETA
        );

        Class injectedAlpha = Class.forName(CLASS_NAME_ALPHA, false, classLoader);
        Assert.assertTrue((Boolean) injectedAlpha.getDeclaredField("initialized").get(null));

        Class injectedBeta = Class.forName(CLASS_NAME_BETA, false, classLoader);
        Assert.assertTrue((Boolean) injectedBeta.getDeclaredField("initialized").get(null));

        Assert.assertEquals(2, ResourcesClassInjector.getInjectedDependencies().size());
        Assert.assertTrue(ResourcesClassInjector.getInjectedDependencies().containsKey(CLASS_NAME_ALPHA.replace(".", "/")));
        Assert.assertTrue(ResourcesClassInjector.getInjectedDependencies().containsKey(CLASS_NAME_BETA.replace(".", "/")));
    }

    @Test
    public void testInjectAllClassesNotThrowExceptionWithInvalidClassName() {
        MyURLClassLoader classLoader = new MyURLClassLoader();
        ResourcesClassInjector.injectAllClasses(classLoader, ClassLoader.getSystemClassLoader(), "made up name", null, "another invalid name");

        Assert.assertTrue(ResourcesClassInjector.getInjectedDependencies().isEmpty());
    }

    @Test
    public void testInjectAllClassesNotThrowExceptionWithInvalidParams() {
        ResourcesClassInjector.injectAllClasses(null, null, null, null);
        ResourcesClassInjector.injectAllClasses(null, null, null);
        ResourcesClassInjector.injectAllClasses(null, null, new String[]{});

        Assert.assertTrue(ResourcesClassInjector.getInjectedDependencies().isEmpty());
    }

    @Test
    public void testClassExistsInReturnsTrue() {
        Assert.assertTrue(ResourcesClassInjector.classExistsIn("java.lang.Thread", null));
        Assert.assertTrue(ResourcesClassInjector.classExistsIn("java.lang.String", ClassLoader.getSystemClassLoader()));
        Assert.assertTrue(ResourcesClassInjector.classExistsIn(ResourcesClassInjector.class.getName(), ResourcesClassInjector.class.getClassLoader()));
    }

    @Test
    public void testClassExistsInReturnsFalse() {
        Assert.assertFalse(ResourcesClassInjector.classExistsIn("made.up.class.Class", ClassLoader.getSystemClassLoader()));
    }

    class MyURLClassLoader extends URLClassLoader {
        public MyURLClassLoader() {
            super(new URL[]{});
        }
    }
}
