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

import net.bytebuddy.ByteBuddy;
import org.junit.rules.TemporaryFolder;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class JarUtils {
    public static File createJar(TemporaryFolder temporaryFolder, String fileName, Map<String, byte[]> entries) throws Exception {
        File file = temporaryFolder.newFile(fileName + ".jar");

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

    public static File makeTargetJarWithRenamedClasses(List<Class> types, String suffix, TemporaryFolder temporaryFolder) throws Exception {
        /**
         * must rename all classes to be statically instrumented in order to invoke the transformed version of these classes.
         * otherwise the JVM will always resolve to the original .class files when instantiating them using the original fully qualified name.
         */
        Map<String, byte[]> entries = new HashMap<>();
        for (Class clazz : types) {
            byte[] bytes = new ByteBuddy()
                    .redefine(clazz)
                    .name(clazz.getName() + suffix)
                    .make()
                    .getBytes();
            entries.put(clazz.getName().replace('.', '/') + suffix + ".class", bytes);
        }

        return createJar(temporaryFolder, "IntegJarTarget", entries);
    }
}
