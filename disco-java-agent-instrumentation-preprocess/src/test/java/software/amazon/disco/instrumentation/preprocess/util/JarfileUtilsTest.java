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

package software.amazon.disco.instrumentation.preprocess.util;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.disco.instrumentation.preprocess.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;

public class JarfileUtilsTest {
    static File dummyUnsignedJar;
    static File dummySignedJar;
    static Map<String, byte[]> jarEntries;

    @BeforeClass
    public static void beforeAll() throws Exception {
        jarEntries = new HashMap<>();
        jarEntries.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
        jarEntries.put("A.class", "A.class".getBytes());
        jarEntries.put("B.class", "B.class".getBytes());

        dummyUnsignedJar = TestUtils.createJar(temporaryFolder, null, jarEntries);

        // this signed jar has the identical content as the 'fakeUnsignedJar' and was signed used a self-signed certificate with an
        // expiration duration of 36,000 days starting from June 6th 2022
        dummySignedJar = new File(JarfileUtilsTest.class.getClassLoader().getResource("self-signed.jar").getFile());
    }

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testReadEntryFromJarWorks() throws Exception {
        Map<String, byte[]> entriesRead = new HashMap<>();
        try (JarFile jarFile = new JarFile(dummyUnsignedJar)) {
            Enumeration<JarEntry> jarEntries = jarFile.entries();

            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();
                entriesRead.put(entry.getName(), JarFileUtils.readEntryFromJar(jarFile, entry));
            }
        }

        assertEquals(3, entriesRead.size());
        for (Map.Entry<String, byte[]> entry : jarEntries.entrySet()) {
            Assert.assertTrue(entriesRead.containsKey(entry.getKey()));
            Assert.assertArrayEquals(entry.getValue(), entriesRead.get(entry.getKey()));
        }
    }

    @Test
    public void testCheckIsJarSignedReturnsUNSIGNED() {
        JarSigningVerificationOutcome outcome = JarFileUtils.verifyJar(dummyUnsignedJar);

        assertEquals(JarSigningVerificationOutcome.UNSIGNED, outcome);
    }

    @Test
    public void testCheckIsJarSignedReturnsSIGNED() {
        JarSigningVerificationOutcome outcome = JarFileUtils.verifyJar(dummySignedJar);

        assertEquals(JarSigningVerificationOutcome.SIGNED, outcome);
    }

    @Test
    public void testCheckIsJarSignedReturnsINVALID() throws IOException {
        File invalidFile = temporaryFolder.newFile();

        JarSigningVerificationOutcome outcome = JarFileUtils.verifyJar(invalidFile);

        assertEquals(JarSigningVerificationOutcome.INVALID, outcome);
    }
}
