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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.MockEntities;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.JarEntryCopyException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationArtifact;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

public class ExportStrategyTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    SourceInfo mockSourceInfo;
    Map<String, InstrumentationArtifact> instrumented;
    JarFile mockJarFile;
    JarOutputStream mockJarOS;
    ExportStrategy mockStrategy;
    JarExportStrategy spyStrategy;
    PreprocessConfig config;

    @Before
    public void before() {
        instrumented = Mockito.mock(Map.class);
        mockJarFile = Mockito.mock(JarFile.class);
        mockStrategy = Mockito.mock(JarExportStrategy.class);
        mockJarOS = Mockito.mock(JarOutputStream.class);

        spyStrategy = Mockito.spy(new JarExportStrategy());
        mockSourceInfo = MockEntities.makeMockJarInfo();
        config = PreprocessConfig.builder().build();

        Mockito.doCallRealMethod().when(mockStrategy).export(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testCopyJarEntryWorksAndWritesToOS() throws IOException {
        JarEntry mockEntry = Mockito.mock(JarEntry.class);
        InputStream mockStream = Mockito.mock(InputStream.class);

        Mockito.when(mockJarFile.getInputStream(mockEntry)).thenReturn(mockStream);
        Mockito.when(mockStream.read(Mockito.any())).thenReturn(1).thenReturn(-1);
        Mockito.when(mockEntry.getName()).thenReturn("entry_a");

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, mockEntry);

        ArgumentCaptor<ZipEntry> captor = ArgumentCaptor.forClass(ZipEntry.class);
        Mockito.verify(mockJarOS).putNextEntry(captor.capture());
        Assert.assertEquals(mockEntry.getName(), captor.getValue().getName());

        Mockito.verify(mockJarOS).write(Mockito.any(byte[].class));
        Mockito.verify(mockJarOS).closeEntry();
    }

    @Test(expected = JarEntryCopyException.class)
    public void testCopyJarEntryFailsWithNullEntry() throws IOException {
        Mockito.doReturn(null).when(mockJarFile).getInputStream(Mockito.any());

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, new JarEntry("fff"));
    }

    @Test
    public void testCopyJarEntryWorksAndIgnoresDuplicateEntryIfEntryIsInMETAINF() throws Exception {
        Mockito.doThrow(new ZipException()).when(mockJarOS).write(Mockito.any());

        // the configured 'ZipException' will be ignored since the duplicated file is under '/META-INF'
        JarEntry entry = Mockito.spy(new JarEntry("META-INF/duplicate.txt"));
        InputStream mockStream = Mockito.mock(InputStream.class);

        Mockito.when(mockJarFile.getInputStream(entry)).thenReturn(mockStream);
        Mockito.when(mockStream.read(Mockito.any())).thenReturn(1).thenReturn(-1);

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, entry);

        ArgumentCaptor<ZipEntry> captor = ArgumentCaptor.forClass(ZipEntry.class);
        Mockito.verify(mockJarOS).putNextEntry(captor.capture());
        Assert.assertEquals(entry.getName(), captor.getValue().getName());

        Mockito.verify(mockJarOS).write(Mockito.any(byte[].class));
    }

    @Test(expected = JarEntryCopyException.class)
    public void testCopyJarEntryFailsOnDuplicateEntryIfEntryIsNotInMETAINF() throws IOException {
        Mockito.doThrow(new ZipException()).when(mockJarOS).write(Mockito.any());

        // the configured 'ZipException' will NOT be ignored and will be wrapped inside a 'JarEntryCopyException' to be propagated
        JarEntry entry = Mockito.spy(new JarEntry("duplicate.txt"));
        InputStream mockStream = Mockito.mock(InputStream.class);

        Mockito.when(mockJarFile.getInputStream(entry)).thenReturn(mockStream);
        Mockito.when(mockStream.read(Mockito.any())).thenReturn(1).thenReturn(-1);

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, entry);

        Mockito.verify(mockJarOS).putNextEntry(entry);
        Mockito.verify(mockJarOS).write(Mockito.any(byte[].class));
    }

    @Test
    public void testSaveTransformedClassesWorksAndCreatesNewEntries() throws IOException {
        spyStrategy.saveInstrumentationArtifactsToJar(mockJarOS, MockEntities.makeInstrumentedClassesMap());

        ArgumentCaptor<JarEntry> jarEntryArgument = ArgumentCaptor.forClass(JarEntry.class);
        Mockito.verify(mockJarOS, Mockito.times(3)).putNextEntry(jarEntryArgument.capture());
        Assert.assertTrue(jarEntryArgument.getAllValues().size() == 3);

        ArgumentCaptor<byte[]> byteArrayArgument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(mockJarOS, Mockito.times(3)).write(byteArrayArgument.capture());
        Assert.assertTrue(byteArrayArgument.getAllValues().size() == 3);

        Mockito.verify(mockJarOS, Mockito.times(3)).closeEntry();
    }
}
