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

import software.amazon.disco.instrumentation.preprocess.exceptions.UnableToReadJarEntryException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class for performing JarFile related tasks.
 */
public class JarFileUtils {

    /**
     * Reads the byte[] of a JarEntry from a JarFile
     *
     * @param jarfile JarFile where the binary data will be read
     * @param entry   JarEntry to be read
     * @return byte[] of the entry
     * @throws UnableToReadJarEntryException
     */
    public static byte[] readEntryFromJar(JarFile jarfile, JarEntry entry) {
        try (final InputStream entryStream = jarfile.getInputStream(entry)) {
            if (entryStream == null) {
                throw new UnableToReadJarEntryException(entry.getName(), null);
            }

            final byte[] buffer = new byte[2048];

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (int len = entryStream.read(buffer); len != -1; len = entryStream.read(buffer)) {
                os.write(buffer, 0, len);
            }
            return os.toByteArray();

        } catch (IOException e) {
            throw new UnableToReadJarEntryException(entry.getName(), null);
        }
    }
}
