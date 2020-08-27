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
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.JarEntryReadException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentedClassState;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JarInfo;
import software.amazon.disco.instrumentation.preprocess.MockEntities;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarExportStrategyTest {
    static final String PACKAGE_SUFFIX = "suffix";
    static final String TEMP_FILE_NAME = "temp.jar";
    static final String ORIGINAL_FILE_NAME = "mock.jar";
    static final String OUT_DIR = "out";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    JarInfo mockJarInfo;
    Map<String, InstrumentedClassState> instrumented;
    JarFile mockJarFile;
    JarOutputStream mockJarOS;
    JarExportStrategy mockStrategy;
    JarExportStrategy spyStrategy;
    PreprocessConfig config;

    @Before
    public void before() throws IOException {
        instrumented = Mockito.mock(Map.class);
        mockJarFile = Mockito.mock(JarFile.class);
        mockStrategy = Mockito.mock(JarExportStrategy.class);
        mockJarOS = Mockito.mock(JarOutputStream.class);

        spyStrategy = Mockito.spy(new JarExportStrategy());
        mockJarInfo = MockEntities.makeMockJarInfo();
        config = PreprocessConfig.builder().build();

        Mockito.doCallRealMethod().when(mockStrategy).export(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(mockStrategy.createTempFile(Mockito.any())).thenReturn(tempFolder.newFile(TEMP_FILE_NAME));
    }

    @Test
    public void testExportWorksAndInvokesCreateTempFile() {
        mockStrategy.export(mockJarInfo, instrumented, null);
        Mockito.verify(mockStrategy).createTempFile(mockJarInfo);
    }

    @Test
    public void testExportWorksAndInvokesBuildOutputJar() {
        mockStrategy.export(mockJarInfo, instrumented, null);
        Mockito.verify(mockStrategy).buildOutputJar(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testExportWorksAndInvokesMoveTempFileToDestination() {
        mockStrategy.export(mockJarInfo, instrumented, null);
        Mockito.verify(mockStrategy).moveTempFileToDestination(Mockito.eq(mockJarInfo), Mockito.any(), Mockito.any());
    }

    @Test
    public void testSaveTransformedClassesWorksAndCreatesNewEntries() throws IOException {
        spyStrategy.saveTransformedClasses(mockJarOS, MockEntities.makeInstrumentedClassesMap());

        ArgumentCaptor<JarEntry> jarEntryArgument = ArgumentCaptor.forClass(JarEntry.class);
        Mockito.verify(mockJarOS, Mockito.times(3)).putNextEntry(jarEntryArgument.capture());
        Assert.assertTrue(jarEntryArgument.getAllValues().size() == 3);

        ArgumentCaptor<byte[]> byteArrayArgument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(mockJarOS, Mockito.times(3)).write(byteArrayArgument.capture());
        Assert.assertTrue(byteArrayArgument.getAllValues().size() == 3);

        Mockito.verify(mockJarOS, Mockito.times(3)).closeEntry();
    }

    @Test
    public void testCopyExistingJarEntriesWorksWithFilesAndPath() throws IOException {
        JarFile jarFile = MockEntities.makeMockJarFile();
        Mockito.doCallRealMethod().when(mockStrategy).copyExistingJarEntries(Mockito.eq(mockJarOS), Mockito.any(), Mockito.any());

        mockStrategy.copyExistingJarEntries(mockJarOS, jarFile, MockEntities.makeInstrumentedClassesMap());

        // 3 out of 6 classes have not been instrumented
        Mockito.verify(mockStrategy, Mockito.times(3)).copyJarEntry(Mockito.eq(mockJarOS), Mockito.any(), Mockito.any());

        // 1 path to be copied
        ArgumentCaptor<JarEntry> jarEntryArgument = ArgumentCaptor.forClass(JarEntry.class);
        Mockito.verify(mockJarOS).putNextEntry(jarEntryArgument.capture());
        Assert.assertTrue(jarEntryArgument.getValue().getName().equals("pathA/"));
    }

    @Test(expected = JarEntryReadException.class)
    public void testCopyJarEntryFailsWithNullEntry() throws IOException {
        Mockito.doReturn(null).when(mockJarFile).getInputStream(Mockito.any());

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, new JarEntry("fff"));
    }

    @Test
    public void testCopyJarEntryWorksAndWritesToOS() throws IOException {
        JarEntry entry = Mockito.mock(JarEntry.class);
        InputStream mockStream = Mockito.mock(InputStream.class);

        Mockito.when(mockJarFile.getInputStream(entry)).thenReturn(mockStream);
        Mockito.when(mockStream.read(Mockito.any())).thenReturn(1).thenReturn(-1);

        spyStrategy.copyJarEntry(mockJarOS, mockJarFile, entry);

        Mockito.verify(mockJarOS).putNextEntry(entry);
        Mockito.verify(mockJarOS).write(Mockito.any(), Mockito.eq(0), Mockito.eq(1));
        Mockito.verify(mockJarOS).closeEntry();
    }

    @Test
    public void testMoveTempFileToDestinationToReplaceOriginal() throws IOException {
        // create original file and assume temp/disco is where the original package is
        File originalFile = createOriginalFile();
        Assert.assertEquals(1, originalFile.length());

        long originalLength = originalFile.length();

        // create temp file named temp.jar
        Mockito.when(mockJarInfo.getFile()).thenReturn(originalFile);
        File file = spyStrategy.createTempFile(mockJarInfo);

        // replace original file
        Path path = spyStrategy.moveTempFileToDestination(mockJarInfo, config, file);

        Assert.assertNotEquals(originalLength, path.toFile().length());
        Assert.assertEquals(originalFile.getAbsolutePath(), path.toFile().getAbsolutePath());
        Assert.assertEquals(0, path.toFile().length());
        Assert.assertEquals(0, originalFile.length());
    }

    @Test
    public void testMoveTempFileToDestinationWorks() throws IOException {
        File outDir = tempFolder.newFolder(OUT_DIR);
        config = PreprocessConfig.builder().outputDir(outDir.getAbsolutePath()).build();
        JarExportStrategy spyStrategy = new JarExportStrategy();

        // create original file and assume temp/disco/tests is where the original package is
        File originalFile = createOriginalFile();
        Assert.assertEquals(1, originalFile.length());

        // create temp file named temp.jar
        Mockito.when(mockJarInfo.getFile()).thenReturn(originalFile);
        File file = spyStrategy.createTempFile(mockJarInfo);

        // move to destination
        Path path = spyStrategy.moveTempFileToDestination(mockJarInfo, config, file);

        Assert.assertEquals(outDir.getAbsolutePath(), path.toFile().getParentFile().getAbsolutePath());
        Assert.assertEquals(mockJarInfo.getFile().getName(), path.toFile().getName());
        Assert.assertEquals(ORIGINAL_FILE_NAME, path.toFile().getName());
        Assert.assertNotEquals(originalFile.length(), path.toFile().length());
        Assert.assertTrue(originalFile.exists());
    }

    @Test
    public void testMoveTempFileToDestinationWorksWithSuffix() throws IOException {
        File outDir = tempFolder.newFolder(OUT_DIR);
        config = PreprocessConfig.builder().suffix(PACKAGE_SUFFIX).outputDir(outDir.getAbsolutePath()).build();
        spyStrategy = new JarExportStrategy();

        // create original file and assume temp/disco/tests is where the original package is
        File originalFile = createOriginalFile();

        File tempFile = spyStrategy.createTempFile(mockJarInfo);

        // move to destination
        Mockito.when(mockJarInfo.getFile()).thenReturn(originalFile);
        Path path = spyStrategy.moveTempFileToDestination(mockJarInfo, config, tempFile);

        String nameToCheck = mockJarInfo.getFile()
                .getName()
                .substring(0, mockJarInfo.getFile().getName().lastIndexOf(PreprocessConstants.JAR_EXTENSION))
                + PACKAGE_SUFFIX
                + PreprocessConstants.JAR_EXTENSION;

        Assert.assertEquals(outDir.getAbsolutePath(), path.toFile().getParentFile().getAbsolutePath());
        Assert.assertEquals(nameToCheck, path.toFile().getName());
        Assert.assertTrue(originalFile.exists());
    }

    private File createOriginalFile() throws IOException {
        File tempFile = tempFolder.newFile(ORIGINAL_FILE_NAME);

        tempFile.getParentFile().mkdirs();
        FileOutputStream os = new FileOutputStream(tempFile);
        os.write(12);
        os.close();

        return tempFile;
    }
}
