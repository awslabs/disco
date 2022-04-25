/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.loaders.classfiles;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JDKModuleLoaderTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    JDKModuleLoader loader = Mockito.spy(new JDKModuleLoader());
    File javaHome;
    PreprocessConfig config;

    @Before
    public void before() throws IOException {
        if (javaHome == null) {
            javaHome = temporaryFolder.newFolder("java_home");
        }

        config = PreprocessConfig.builder().outputDir(temporaryFolder.getRoot().getPath()).build();
    }

    @Test
    public void testLoadCallsHelperMethod() throws IOException {
        Mockito.doReturn(false).when(loader).isJDK9Compatible();
        File jdk8ModuleFile = temporaryFolder.newFile("rt.jar");
        Mockito.doReturn(jdk8ModuleFile).when(loader).getJDKBaseModule(Mockito.eq(config));

        loader.load(Paths.get("somePath"), config);

        Mockito.verify(loader).getJDKBaseModule(config);
        Mockito.verify(loader).isJDK9Compatible();
        Mockito.verify(loader).loadJar(Mockito.eq(jdk8ModuleFile), Mockito.any(ExportStrategy.class));
    }

    @Test(expected = InstrumentationException.class)
    public void testLoadFailsWhenInstrumentingJDK8OnJDK9JVM() throws IOException {
        // jvm is jdk9 compatible, aka jdk9+
        Mockito.doReturn(true).when(loader).isJDK9Compatible();

        File jdkBaseModule = temporaryFolder.newFile("rt.jar");
        Mockito.doReturn(jdkBaseModule).when(loader).getJDKBaseModule(Mockito.eq(config));

        loader.load(Paths.get("somePath"), config);

        Mockito.verify(loader).getJDKBaseModule(config);
        Mockito.verify(loader).isJDK9Compatible();
    }

    @Test(expected = InstrumentationException.class)
    public void testLoadFailsWhenInstrumentingJDK9OnJDK8JVM() throws IOException {
        // jvm is not jdk9 compatible, aka jdk8 or lower
        Mockito.doReturn(false).when(loader).isJDK9Compatible();

        File jdkBaseModule = temporaryFolder.newFile("java.base.jmod");
        Mockito.doReturn(jdkBaseModule).when(loader).getJDKBaseModule(Mockito.eq(config));

        loader.load(Paths.get("somePath"), config);

        Mockito.verify(loader).getJDKBaseModule(config);
        Mockito.verify(loader).isJDK9Compatible();
    }

    @Test
    public void testGetJDKBaseModuleReturnsJDKBaseModuleForJDK8AndLower() throws IOException {
        File jdk8ModuleFile = Paths.get(javaHome.getAbsolutePath(), "lib", "rt.jar").toFile();
        jdk8ModuleFile.getParentFile().mkdirs();
        jdk8ModuleFile.createNewFile();

        config = PreprocessConfig.builder().jdkPath(javaHome.getPath()).build();

        File baseModule = loader.getJDKBaseModule(config);

        assertNotNull(baseModule);
        assertEquals(jdk8ModuleFile, baseModule);
    }

    @Test
    public void testGetJDKBaseModuleReturnsJDKBaseModuleForJDK9AndHigher() throws IOException {
        File jdk9ModuleFile = Paths.get(javaHome.getAbsolutePath(), "jmods", "java.base.jmod").toFile();
        jdk9ModuleFile.getParentFile().mkdirs();
        jdk9ModuleFile.createNewFile();

        config = PreprocessConfig.builder().jdkPath(javaHome.getPath()).build();

        File baseModule = loader.getJDKBaseModule(config);

        assertNotNull(baseModule);
        assertEquals(jdk9ModuleFile, baseModule);
    }

    @Test(expected = InstrumentationException.class)
    public void testGetJDKBaseModuleThrowsExceptionOnInvalidJDKPath() {
        config = PreprocessConfig.builder().jdkPath(temporaryFolder.toString()).build();

        loader.getJDKBaseModule(config);
    }
}
