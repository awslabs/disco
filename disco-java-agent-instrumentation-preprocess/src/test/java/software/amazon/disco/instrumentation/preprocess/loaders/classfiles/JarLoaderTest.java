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

package software.amazon.disco.instrumentation.preprocess.loaders.classfiles;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoModuleToInstrumentException;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.JarUtils;
import software.amazon.disco.instrumentation.preprocess.MockEntities;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarLoaderTest {
    JarLoader loader;
    PreprocessConfig config;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        config = PreprocessConfig.builder().jarPaths(MockEntities.makeMockPathsWithDuplicates()).build();
        loader = new JarLoader();
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadFailWithEmptyPathList() {
        loader.load(config);
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadFailWithNullConfig() {
        loader.load(null);
    }

    @Test(expected = NoModuleToInstrumentException.class)
    public void testLoadFailWithNullPathList() {
        loader.load(PreprocessConfig.builder().build());
    }

    @Test
    public void testLoadWorksWithMultiplePaths() {
        JarLoader packageLoader = Mockito.mock(JarLoader.class);
        JarInfo info = Mockito.mock(JarInfo.class);

        Mockito.doCallRealMethod().when(packageLoader).load(config);
        Mockito.doReturn(info).when(packageLoader).loadJar(Mockito.any(File.class), Mockito.any(ExportStrategy.class));

        List<JarInfo> infos = packageLoader.load(config);

        Mockito.verify(packageLoader, Mockito.times(3)).loadJar(Mockito.any(File.class), Mockito.any(ExportStrategy.class));
        Assert.assertEquals(3, infos.size());
    }

    @Test
    public void testLoadJarWorks() throws Exception {
        JarLoader packageLoader = Mockito.spy(new JarLoader());

        Map<String,byte[]> srcEntries = new HashMap<>();
        srcEntries.put("A.class", "A.class".getBytes());
        srcEntries.put("B.class", "B.class".getBytes());

        File file = JarUtils.createJar(temporaryFolder, "jarFile", srcEntries);

        JarInfo info = packageLoader.loadJar(file, null);

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
