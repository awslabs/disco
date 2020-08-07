/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.loaders.modules;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoModuleToInstrumentException;
import software.amazon.disco.instrumentation.preprocess.export.ModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.JarFileTestUtils;
import software.amazon.disco.instrumentation.preprocess.util.MockEntities;

import java.io.File;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarModuleLoaderTest {
    JarModuleLoader loader;
    PreprocessConfig config;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        config = PreprocessConfig.builder().jarPaths(MockEntities.makeMockPathsWithDuplicates()).build();
        loader = new JarModuleLoader();
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadPackagesFailWithEmptyPathList() {
        loader.loadPackages(config);
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadPackagesFailWithNullConfig() {
        loader.loadPackages(null);
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadPackagesFailWithNullPathList() {
        loader.loadPackages(PreprocessConfig.builder().build());
    }

    @Test
    public void testLoadPackagesWorksWithMultiplePaths() {
        JarModuleLoader packageLoader = Mockito.mock(JarModuleLoader.class);
        ModuleInfo info = Mockito.mock(ModuleInfo.class);

        Mockito.doCallRealMethod().when(packageLoader).loadPackages(config);
        Mockito.doReturn(info).when(packageLoader).loadPackage(Mockito.any(File.class), Mockito.any(ModuleExportStrategy.class));

        List<ModuleInfo> infos = packageLoader.loadPackages(config);

        Mockito.verify(packageLoader, Mockito.times(3)).loadPackage(Mockito.any(File.class), Mockito.any(ModuleExportStrategy.class));
        Assert.assertEquals(3, infos.size());
    }

    @Test
    public void testLoadPackageWorks() throws Exception {
        JarModuleLoader packageLoader = Mockito.spy(new JarModuleLoader());

        File file = JarFileTestUtils.createJar(temporaryFolder, "jarFile", "A.class", "B.class");

        ModuleInfo info = packageLoader.loadPackage(file, null);

        Mockito.verify(packageLoader).injectFileToSystemClassPath(file);
        Assert.assertEquals(2, info.getClassByteCodeMap().size());
        Assert.assertEquals(file, info.getFile());
        Assert.assertTrue(info.getClassByteCodeMap().containsKey("A"));
        Assert.assertTrue(info.getClassByteCodeMap().containsKey("B"));
        Assert.assertArrayEquals("A.class".getBytes(), info.getClassByteCodeMap().get("A"));
        Assert.assertArrayEquals("B.class".getBytes(), info.getClassByteCodeMap().get("B"));
    }

    @Test
    public void testExtractEntriesWorks() {
        JarFile jarFile = MockEntities.makeMockJarFile();

        List<JarEntry> entries = loader.extractEntries(jarFile);

        Assert.assertEquals(6, entries.size());
    }
}
