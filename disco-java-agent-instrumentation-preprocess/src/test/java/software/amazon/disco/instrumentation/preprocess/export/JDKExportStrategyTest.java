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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.amazon.disco.instrumentation.preprocess.export.JDKExportStrategy.INSTRUMENTED_JDK_OUTPUT_NAME;
import static software.amazon.disco.instrumentation.preprocess.export.JDKExportStrategy.PACKAGE_TO_INSERT;

public class JDKExportStrategyTest {
    static final String TEMP_FILE_NAME = "temp.jar";
    static final String AGENT_FILE_TO_INSERT = PACKAGE_TO_INSERT + "/DiscoRunnableDecorator.class";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    SourceInfo mockSourceInfo;
    Map<String, InstrumentationArtifact> instrumented;
    JarFile mockJarFile;
    JarOutputStream mockJarOS;
    JDKExportStrategy spyStrategy;

    @Before
    public void before() {
        instrumented = Mockito.mock(Map.class);
        mockJarFile = Mockito.mock(JarFile.class);
        spyStrategy = Mockito.spy(new JDKExportStrategy());
        mockJarOS = Mockito.mock(JarOutputStream.class);

        mockSourceInfo = MockEntities.makeMockJarInfo();

        Mockito.doReturn(new File(tempFolder.getRoot(), INSTRUMENTED_JDK_OUTPUT_NAME)).when(spyStrategy).createOutputFile(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testExportWorksAndInvokesHelperMethods() throws Exception {
        File jdkBaseModule = TestUtils.createJar(tempFolder, "rj.jar", Collections.singletonMap("someClass.class", "someClass.class".getBytes()));
        Mockito.when(mockSourceInfo.getSourceFile()).thenReturn(jdkBaseModule);

        File agentFile = TestUtils.createJar(tempFolder, "agent.jar", Collections.singletonMap(AGENT_FILE_TO_INSERT, AGENT_FILE_TO_INSERT.getBytes()));
        PreprocessConfig config = PreprocessConfig.builder()
            .agentPath(agentFile.getAbsolutePath())
            .jdkPath(jdkBaseModule.getParent())
            .outputDir(tempFolder.getRoot().getAbsolutePath())
            .build();

        spyStrategy.export(mockSourceInfo, instrumented, config, "jdk");

        Mockito.verify(spyStrategy).extractBootstrapDependenciesFromAgent(Mockito.any());
        Mockito.verify(spyStrategy).saveInstrumentationArtifactsToJar(Mockito.any(), Mockito.anyMap());

        ArgumentCaptor<JarEntry> jarEntryArgumentCaptor = ArgumentCaptor.forClass(JarEntry.class);
        Mockito.verify(spyStrategy).copyJarEntry(Mockito.any(), Mockito.any(), jarEntryArgumentCaptor.capture());
        assertEquals(AGENT_FILE_TO_INSERT, jarEntryArgumentCaptor.getValue().getName());
    }

    @Test
    public void testExtractBootstrapDependenciesFromAgentWorks() throws Exception {
        Map<String, byte[]> entries = new HashMap<>();

        List<String> dependencies = Arrays.asList(
            PACKAGE_TO_INSERT,
            PACKAGE_TO_INSERT + "/AnotherClass.class",
            "randomUnrelatedFile"
        );

        for (String dep : dependencies) {
            entries.put(dep, dep.getBytes());
        }

        JarFile jarFile = new JarFile(TestUtils.createJar(tempFolder, TEMP_FILE_NAME, entries));

        Map<String, JarEntry> result = spyStrategy.extractBootstrapDependenciesFromAgent(jarFile);

        assertEquals(2, result.size());

        assertEquals(dependencies.get(0), result.get(dependencies.get(0)).getName());
        assertArrayEquals(dependencies.get(0).getBytes(), FileUtils.readEntryFromJar(jarFile, result.get(dependencies.get(0))));

        assertEquals(dependencies.get(1), result.get(dependencies.get(1)).getName());
        assertArrayEquals(dependencies.get(1).getBytes(), FileUtils.readEntryFromJar(jarFile, result.get(dependencies.get(1))));
    }

    @Test
    public void testExportCreatesValidJarFile() throws Exception {
        // Some fake instrumented classes
        Map<String, InstrumentationArtifact> instrumented = MockEntities.makeInstrumentedClassesMap();
        Map<String, byte[]> records = instrumented.entrySet().stream().collect(Collectors.toMap(
            e -> e.getKey() + ".class",
            e -> e.getValue().getClassBytes())
        );

        // Fake Disco agent, used to test that certain classes get to copied to the output JDK jar.
        Map<String, byte[]> agentJarRecords = new HashMap<>();
        agentJarRecords.put(AGENT_FILE_TO_INSERT, new byte[]{10});

        File agent = TestUtils.createJar(tempFolder, "agent.jar", agentJarRecords);
        File outDir = tempFolder.newFolder();
        File jdkBaseModule = TestUtils.createJar(tempFolder, "rj.jar", Collections.singletonMap("someClass.class", "someClass.class".getBytes()));

        new JDKExportStrategy().export(
            mockSourceInfo,
            instrumented,
            PreprocessConfig.builder().outputDir(outDir.getAbsolutePath()).agentPath(agent.getAbsolutePath()).jdkPath(jdkBaseModule.getParent()).build(),
            "jdk"
        );

        JarFile outputJar = new JarFile(Paths.get(outDir.getAbsolutePath(), "jdk", INSTRUMENTED_JDK_OUTPUT_NAME).toFile());

        // check whether the output Jar can be opened correctly and each entry can be parsed and read.
        Enumeration entries = outputJar.entries();

        // merge both maps
        Map<String, byte[]> combined = new HashMap<>();
        combined.putAll(records);
        combined.putAll(agentJarRecords);

        assertEquals(combined.size(), outputJar.stream().count());

        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();

            byte[] bytes = FileUtils.readEntryFromJar(outputJar, entry);
            assertArrayEquals(combined.get(entry.getName()), bytes);
            combined.remove(entry.getName());
        }

        assertTrue(combined.isEmpty());
    }
}
