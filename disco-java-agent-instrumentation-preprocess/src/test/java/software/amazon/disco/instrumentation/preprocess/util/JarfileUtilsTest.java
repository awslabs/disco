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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.disco.instrumentation.preprocess.TestUtils;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarfileUtilsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testReadEntryFromJarWorks() throws Exception {
        Map<String, byte[]> srcEntries = new HashMap<>();
        srcEntries.put("ClassAAAAAAAAAAAAA.class","ClassAAAAAAAAAAAAA.class".getBytes());
        srcEntries.put("ClassBBBBBBBBBBBB.class","ClassBBBBBBBBBBBB.class".getBytes());
        srcEntries.put("/CCCC","/CCCC".getBytes());

        File testFile = TestUtils.createJar(temporaryFolder, "Test.jar", srcEntries);

        Map<String, byte[]> entriesRead = new HashMap<>();
        try (JarFile jarFile = new JarFile(testFile)) {
            Enumeration<JarEntry> jarEntries = jarFile.entries();

            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();
                entriesRead.put(entry.getName(), JarFileUtils.readEntryFromJar(jarFile, entry));
            }
        }

        Assert.assertEquals(3, entriesRead.size());
        for(Map.Entry<String, byte[]> entry: srcEntries.entrySet()){
            Assert.assertTrue(entriesRead.containsKey(entry.getKey()));
            Assert.assertArrayEquals(entry.getValue(), entriesRead.get(entry.getKey()));
        }
    }
}
