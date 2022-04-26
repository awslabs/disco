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

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.export.DirectoryExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * A {@link ClassFileLoader} that loads all compiled Java classes under specified directory
 */
public class DirectoryLoader implements ClassFileLoader {
    private static final Logger log = LogManager.getLogger(DirectoryLoader.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public SourceInfo load(final Path source, final PreprocessConfig config) {
        log.debug(PreprocessConstants.MESSAGE_PREFIX + "Loading Directory: " + source);

        try {
            final ClassFileScanner locator = new ClassFileScanner(source);

            Files.walkFileTree(source, locator);

            if (!locator.getClassFilesLocated().isEmpty()) {
                return new SourceInfo(
                    source.toFile(),
                    new DirectoryExportStrategy(),
                    locator.getClassFilesLocated()
                );
            } else {
                return null;
            }

        } catch (Throwable t) {
            log.warn(PreprocessConstants.MESSAGE_PREFIX + "Failed to load directory: " + source);
            return null;
        }
    }

    /**
     * An implementation of the {@link SimpleFileVisitor} class. This implementation walks through all sub folders of a specified path
     * and extracts all compiled Java class files to be later processed by the Preprocessor.
     */
    public static class ClassFileScanner extends SimpleFileVisitor<Path> {
        @Getter
        private Map<String, byte[]> classFilesLocated;

        private Path root;

        /**
         * Constructor
         *
         * @param root starting point to start walking
         */
        public ClassFileScanner(final Path root) {
            this.classFilesLocated = new HashMap<>();
            this.root = root;
        }

        /**
         * {@inheritDoc}
         * <p>
         * This overridden method also collects all files with '.class' extension.
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
            if (!attr.isDirectory()) {
                if (file.toString().endsWith(".class")) {
                    final Path fullPath = Paths.get(file.toFile().getAbsolutePath());

                    // compute the fully qualified name of a discovered class. for instance, a class file located under 'tomcat/software/amazon/somepackage/ClassA.class' will be
                    // formatted to 'software.amazon.somepackage.ClassA'
                    final Path relativePath = root.relativize(fullPath);
                    final String fullyQualifiedClassName = relativePath.toString()
                        .substring(0, relativePath.toString().indexOf(".class"))
                        .replace("/", ".");

                    classFilesLocated.put(fullyQualifiedClassName, Files.readAllBytes(file));
                }
            }

            return CONTINUE;
        }
    }
}
