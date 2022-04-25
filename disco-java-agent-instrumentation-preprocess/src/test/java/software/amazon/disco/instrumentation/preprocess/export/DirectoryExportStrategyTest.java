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

package software.amazon.disco.instrumentation.preprocess.export;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.MockEntities;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ExportException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationArtifact;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DirectoryExportStrategyTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    PreprocessConfig config;
    Map<String, InstrumentationArtifact> instrumented;
    DirectoryExportStrategy strategy;
    SourceInfo mockSourceInfo;

    @Before
    public void before() {
        config = PreprocessConfig.builder().outputDir(tempFolder.getRoot().getAbsolutePath() + "/out").build();
        instrumented = new HashMap<>();
        strategy = Mockito.spy(new DirectoryExportStrategy());
        mockSourceInfo = MockEntities.makeMockJarInfo();
    }

    @Test
    public void testExportInvokesHelperMethods() {
        InstrumentationArtifact artifactA = new InstrumentationArtifact(new byte[]{1});
        InstrumentationArtifact artifactB = new InstrumentationArtifact(new byte[]{2});
        instrumented.put("software/ClassA", artifactA);
        instrumented.put("software/ClassB", artifactB);

        strategy.export(mockSourceInfo, instrumented, config, "tomcat");

        ArgumentCaptor<Map.Entry<String, InstrumentationArtifact>> captor = ArgumentCaptor.forClass(Map.Entry.class);
        Mockito.verify(strategy, Mockito.times(2)).saveArtifactToDisk(captor.capture(), Mockito.eq(config), Mockito.eq("tomcat"));

        Map valuesCaptured = new HashMap<String, InstrumentationArtifact>();
        captor.getAllValues().forEach(arg -> valuesCaptured.put(arg.getKey(), arg.getValue()));

        assertEquals(2, valuesCaptured.size());

        assertTrue(valuesCaptured.containsKey("software/ClassA"));
        assertEquals(artifactA, valuesCaptured.get("software/ClassA"));

        assertTrue(valuesCaptured.containsKey("software/ClassB"));
        assertEquals(artifactB, valuesCaptured.get("software/ClassB"));
    }

    @Test
    public void testExportReturns_withNullArtifacts() {
        strategy.export(mockSourceInfo, null, config, "");

        Mockito.verify(strategy, Mockito.never()).saveArtifactToDisk(Mockito.any(), Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testExportReturns_withEmptyArtifacts() {
        strategy.export(mockSourceInfo, Collections.emptyMap(), config, "");

        Mockito.verify(strategy, Mockito.never()).saveArtifactToDisk(Mockito.any(), Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testSaveArtifactToDiskWorks() throws IOException {
        InstrumentationArtifact artifactA = new InstrumentationArtifact(new byte[]{2});

        strategy.saveArtifactToDisk(new AbstractMap.SimpleEntry("software/amazon/ClassA", artifactA), config, "tomcat");

        byte[] content = Files.readAllBytes(new File(config.getOutputDir() + "/tomcat/software/amazon", "ClassA.class").toPath());
        assertArrayEquals(artifactA.getClassBytes(), content);
    }

    @Test(expected = ExportException.class)
    public void testSaveArtifactToDiskFailsAndThrowsException() {
        InstrumentationArtifact artifactA = Mockito.mock(InstrumentationArtifact.class);
        Mockito.doThrow(new RuntimeException()).when(artifactA).getClassBytes();

        strategy.saveArtifactToDisk(new AbstractMap.SimpleEntry("someDir/ClassA", artifactA), config, "tomcat");
    }
}
