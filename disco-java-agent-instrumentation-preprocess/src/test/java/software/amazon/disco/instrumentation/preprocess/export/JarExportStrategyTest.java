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

package software.amazon.disco.instrumentation.preprocess.export;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.MockEntities;
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationArtifact;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;
import software.amazon.disco.instrumentation.preprocess.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class JarExportStrategyTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    SourceInfo mockSourceInfo;
    Map<String, InstrumentationArtifact> instrumented;
    JarFile mockJarFile;
    JarOutputStream mockJarOS;
    JarExportStrategy spyStrategy;
    PreprocessConfig config;

    @Before
    public void before() throws IOException {
        instrumented = Mockito.mock(Map.class);
        mockJarFile = Mockito.mock(JarFile.class);
        mockJarOS = Mockito.mock(JarOutputStream.class);
        spyStrategy = Mockito.spy(new JarExportStrategy());

        mockSourceInfo = MockEntities.makeMockJarInfo();
        config = PreprocessConfig.builder().outputDir(tempFolder.newFolder("instrumented").getAbsolutePath()).build();

        Mockito.doReturn(new File(tempFolder.getRoot(), "someFile")).when(spyStrategy).createOutputFile(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testExportWorksAndInvokesHelperMethods() throws Exception {
        File file = TestUtils.createJar(tempFolder, "someJar.jar", Collections.emptyMap());
        Mockito.when(mockSourceInfo.getSourceFile()).thenReturn(file);

        spyStrategy.export(mockSourceInfo, instrumented, config, "lib");

        Mockito.verify(spyStrategy).createOutputFile(tempFolder.getRoot().getAbsolutePath() + "/instrumented", "lib", mockSourceInfo.getSourceFile().getName());
        Mockito.verify(spyStrategy).copyExistingJarEntries(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(spyStrategy).saveInstrumentationArtifactsToJar(Mockito.any(), Mockito.any());
    }

    @Test
    public void testExportSkipsJarCreationWhenNoClassesInstrumented() throws Exception {
        File file = TestUtils.createJar(tempFolder, "someJar.jar", Collections.emptyMap());
        Mockito.when(mockSourceInfo.getSourceFile()).thenReturn(file);

        spyStrategy.export(mockSourceInfo, Collections.emptyMap(), config, "lib");

        Mockito.verify(spyStrategy, Mockito.never()).createOutputFile(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(spyStrategy, Mockito.never()).copyExistingJarEntries(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(spyStrategy, Mockito.never()).saveInstrumentationArtifactsToJar(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCopyExistingJarEntriesWorksWithFilesAndPath() throws Exception {
        // this mock Jar contains 7 entries in total, 6 files and 1 directory
        JarFile jarFile = MockEntities.makeMockJarFile();
        Mockito.doNothing().when(spyStrategy).copyJarEntry(Mockito.any(), Mockito.any(), Mockito.any());

        spyStrategy.copyExistingJarEntries(mockJarOS, jarFile, MockEntities.makeInstrumentedClassesMap());

        // 3 out of 6 classes have not been instrumented and therefore copied
        Mockito.verify(spyStrategy, Mockito.times(3)).copyJarEntry(Mockito.eq(mockJarOS), Mockito.any(), Mockito.any());

        // 1 path to be copied
        ArgumentCaptor<JarEntry> jarEntryArgument = ArgumentCaptor.forClass(JarEntry.class);
        Mockito.verify(mockJarOS).putNextEntry(jarEntryArgument.capture());
        assertTrue(jarEntryArgument.getValue().getName().equals("pathA/"));
    }

    @Test
    public void testExportCreatesValidJarFile() throws Exception {
        Map<String, InstrumentationArtifact> instrumented = MockEntities.makeInstrumentedClassesMap();
        Map<String, byte[]> records = instrumented.entrySet().stream().collect(Collectors.toMap(
            e -> e.getKey() + ".class",
            e -> e.getValue().getClassBytes())
        );

        File original = TestUtils.createJar(tempFolder, "JarExportStrategyTest.jar", records);
        Mockito.when(mockSourceInfo.getSourceFile()).thenReturn(original);

        File outDir = tempFolder.newFolder();

        new JarExportStrategy().export(mockSourceInfo, instrumented, PreprocessConfig.builder().outputDir(outDir.getAbsolutePath()).build(), "lib");

        JarFile outputJar = new JarFile(Paths.get(outDir.getAbsolutePath(), "lib", "JarExportStrategyTest.jar").toFile());

        // check whether the output Jar can be opened correctly and each entry can be parsed and read.
        Enumeration entries = outputJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();

            byte[] bytes = FileUtils.readEntryFromJar(outputJar, entry);
            assertArrayEquals(records.get(entry.getName()), bytes);
            records.remove(entry.getName());
        }

        assertTrue(records.isEmpty());
    }
}
