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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.MockEntities;
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JarLoaderTest {
    JarLoader loader;
    PreprocessConfig config;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        config = PreprocessConfig.builder().sourcePath("lib", new HashSet<>(MockEntities.makeMockPathsWithDuplicates())).build();
        loader = new JarLoader();
    }

    @Test
    public void testLoadWorks() {
        JarLoader packageLoader = Mockito.spy(loader);
        SourceInfo info = new SourceInfo(new File("a"), null, Collections.emptyMap());

        // return the same JarInfo object for all 3 jar paths defined in the config file.
        Mockito.doReturn(info).when(packageLoader).loadJar(Mockito.any(File.class), Mockito.any(ExportStrategy.class));

        packageLoader.load(Paths.get("somePath"), config);

        Mockito.verify(packageLoader).loadJar(Mockito.eq(Paths.get("somePath").toFile()), Mockito.any(ExportStrategy.class));
    }

    @Test
    public void testLoadJarWorks() throws Exception {
        JarLoader packageLoader = Mockito.spy(new JarLoader());

        Map<String,byte[]> srcEntries = new HashMap<>();
        srcEntries.put("A.class", "A.class".getBytes());
        srcEntries.put("B.class", "B.class".getBytes());

        File file = TestUtils.createJar(temporaryFolder, "jarFile", srcEntries);

        SourceInfo info = packageLoader.loadJar(file, null);

        Mockito.verify(packageLoader).injectFileToSystemClassPath(file);
        assertEquals(2, info.getClassByteCodeMap().size());
        assertEquals(file, info.getSourceFile());
        assertTrue(info.getClassByteCodeMap().containsKey("A"));
        assertTrue(info.getClassByteCodeMap().containsKey("B"));
        assertArrayEquals("A.class".getBytes(), info.getClassByteCodeMap().get("A"));
        assertArrayEquals("B.class".getBytes(), info.getClassByteCodeMap().get("B"));
    }

    @Test
    public void testExtractEntriesWorks() {
        JarFile jarFile = MockEntities.makeMockJarFile();

        List<JarEntry> entries = loader.extractEntries(jarFile);

        assertEquals(6, entries.size());
    }
}
