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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.MockEntities;
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SkipSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.util.JarSigningVerificationOutcome;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JarLoaderTest {
    static File dummyUnsignedJar;
    static File dummySignedJar;
    static SignedJarHandlingStrategy instrumentStrategy = new InstrumentSignedJarHandlingStrategy();
    static SignedJarHandlingStrategy skipStrategy = new SkipSignedJarHandlingStrategy();

    JarLoader loader;
    PreprocessConfig config;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeAll() throws Exception {
        Map<String, byte[]> srcEntries = new HashMap<>();
        srcEntries.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
        srcEntries.put("A.class", "A.class".getBytes());
        srcEntries.put("B.class", "B.class".getBytes());

        dummyUnsignedJar = TestUtils.createJar(temporaryFolder, null, srcEntries);
        dummySignedJar = new File(JarLoaderTest.class.getClassLoader().getResource("self-signed.jar").getFile());
    }

    @Before
    public void before() {
        config = PreprocessConfig.builder().sourcePath("lib", new HashSet<>(MockEntities.makeMockPathsWithDuplicates())).build();
        loader = Mockito.spy(new JarLoader());
    }

    @Test
    public void testLoadReturnsSourceInfoObject() {
        SourceInfo info = new SourceInfo(new File("a"), null, Collections.emptyMap(), JarSigningVerificationOutcome.UNSIGNED);

        // return the same JarInfo object for all 3 jar paths defined in the config file.
        Mockito.doReturn(info).when(loader).loadJar(Mockito.any(File.class), Mockito.any(ExportStrategy.class), Mockito.any(SignedJarHandlingStrategy.class));

        loader.load(Paths.get("somePath"), config);

        Mockito.verify(loader).loadJar(Mockito.eq(Paths.get("somePath").toFile()), Mockito.any(ExportStrategy.class), Mockito.any(SignedJarHandlingStrategy.class));
    }

    @Test
    public void testLoadJarReturnsSourceInfoObject() {
        SourceInfo info = loader.loadJar(dummyUnsignedJar, null, instrumentStrategy);

        Mockito.verify(loader).injectFileToSystemClassPath(dummyUnsignedJar);
        assertEquals(2, info.getClassByteCodeMap().size());
        assertEquals(dummyUnsignedJar, info.getSourceFile());
        assertTrue(info.getClassByteCodeMap().containsKey("A"));
        assertTrue(info.getClassByteCodeMap().containsKey("B"));
        assertArrayEquals("A.class".getBytes(), info.getClassByteCodeMap().get("A"));
        assertArrayEquals("B.class".getBytes(), info.getClassByteCodeMap().get("B"));
    }

    @Test
    public void testLoadJarReturnsSourceInfoObject_whenJarIsSignedAndDefaultStrategyIsUsed() {
        SourceInfo info = loader.loadJar(dummySignedJar, null, instrumentStrategy);

        assertNotNull(info);
        assertEquals(JarSigningVerificationOutcome.SIGNED, info.getJarSigningVerificationOutcome());

        Mockito.verify(loader).injectFileToSystemClassPath(dummySignedJar);
        assertEquals(2, info.getClassByteCodeMap().size());
        assertEquals(dummySignedJar, info.getSourceFile());
        assertTrue(info.getClassByteCodeMap().containsKey("A"));
        assertTrue(info.getClassByteCodeMap().containsKey("B"));
        assertArrayEquals("A.class".getBytes(), info.getClassByteCodeMap().get("A"));
        assertArrayEquals("B.class".getBytes(), info.getClassByteCodeMap().get("B"));
    }

    @Test
    public void testLoadJarReturnsNull_whenJarIsSignedAndIgnoreStrategyIsUsed() {
        SourceInfo info = loader.loadJar(dummySignedJar, null, skipStrategy);

        assertNull(info);
    }

    @Test
    public void testExtractEntriesReturnsList() {
        JarFile jarFile = MockEntities.makeMockJarFile();

        List<JarEntry> entries = loader.extractEntries(jarFile);

        assertEquals(6, entries.size());
    }
}
