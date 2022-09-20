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

package software.amazon.disco.instrumentation.preprocess;

import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class TestUtils {
    public static File createJar(TemporaryFolder temporaryFolder, String fileName, Map<String, byte[]> entries) throws Exception {
        File file = fileName == null ? temporaryFolder.newFile() : temporaryFolder.newFile(fileName);
        return createJar(file, entries);
    }

    public static File createJar(File file, Map<String, byte[]> entries) throws Exception {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    jarOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                    jarOutputStream.write(entry.getValue());
                    jarOutputStream.closeEntry();
                }
            }
        }
        return file;
    }

    public static File createDummyPlugin(File file, String... additionalEntries) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();

        if (additionalEntries != null) {
            for (String entry : additionalEntries) {
                entries.put(entry, entry.getBytes(StandardCharsets.UTF_8));
            }
        }
        entries.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nDisco-Bootstrap-Classloader: true\nDisco-Installable-Classes: ClassA\n".getBytes(StandardCharsets.UTF_8));

        return createJar(file, entries);
    }

    public static File createFile(File parent, String fileName, byte[] content) throws Exception {
        if (!parent.exists()) {
            parent.mkdirs();
        }
        File file = new File(parent, fileName);
        file.createNewFile();

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(content);
        }

        return file;
    }
}
