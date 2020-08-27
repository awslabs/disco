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
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class JarUtils {
    public static File createJar(TemporaryFolder temporaryFolder, String fileName, Map<String, byte[]> entries) throws Exception {
        File file = temporaryFolder.newFile(fileName + ".jar");

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
                for (Map.Entry<String, byte[]> entry: entries.entrySet()) {
                    jarOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                    jarOutputStream.write(entry.getValue());
                    jarOutputStream.closeEntry();
                }
            }
        }
        return file;
    }
}
