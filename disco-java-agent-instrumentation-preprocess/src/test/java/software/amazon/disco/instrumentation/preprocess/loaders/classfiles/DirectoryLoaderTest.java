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
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.export.DirectoryExportStrategy;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DirectoryLoaderTest {
    @Rule()
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    File directoryContainingClasses;
    DirectoryLoader loader;

    @Before
    public void before() throws Exception {
        loader = Mockito.spy(new DirectoryLoader());

        directoryContainingClasses = temporaryFolder.newFolder();

        TestUtils.createFile(directoryContainingClasses, "a.class", "a.class".getBytes());
        TestUtils.createFile(new File(directoryContainingClasses.getAbsolutePath() + "/packageTopLevel"), "b.class", "b.class".getBytes());
        TestUtils.createFile(new File(directoryContainingClasses.getAbsolutePath() + "/packageTopLevel/packageSecondLevel"), "c.class", "c.class".getBytes());
    }

    @Test
    public void testLoadReturnsExpectedSourceInfoAfterScanningDirectory() {
        SourceInfo info = loader.load(directoryContainingClasses.toPath(), null);

        assertEquals(directoryContainingClasses.getAbsolutePath(), info.getSourceFile().getAbsolutePath());
        assertTrue(info.getExportStrategy() instanceof DirectoryExportStrategy);

        verifyResult(info.getClassByteCodeMap());
    }

    @Test
    public void testLoadReturnsNull(){
        assertNull(loader.load(null, null));
    }

    @Test
    public void testClassFileLocatorCollectsAllClassFilesUnderDirectory() throws Exception {
        DirectoryLoader.ClassFileScanner locator = new DirectoryLoader.ClassFileScanner(directoryContainingClasses.toPath());
        Files.walkFileTree(directoryContainingClasses.toPath(), locator);

        verifyResult(locator.getClassFilesLocated());
    }

    private void verifyResult(Map<String, byte[]> extractedClassFiles) {
        assertEquals(3, extractedClassFiles.size());

        assertTrue(extractedClassFiles.containsKey("a"));
        assertArrayEquals("a.class".getBytes(), extractedClassFiles.get("a"));
        assertTrue(extractedClassFiles.containsKey("packageTopLevel.b"));
        assertArrayEquals("b.class".getBytes(), extractedClassFiles.get("packageTopLevel.b"));
        assertTrue(extractedClassFiles.containsKey("packageTopLevel.packageSecondLevel.c"));
        assertArrayEquals("c.class".getBytes(), extractedClassFiles.get("packageTopLevel.packageSecondLevel.c"));
    }
}
