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

package software.amazon.disco.instrumentation.preprocess.loaders.modules;

import lombok.Getter;
import software.amazon.disco.agent.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.NoModuleToInstrumentException;
import software.amazon.disco.instrumentation.preprocess.export.JarModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.export.ModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A {@link ModuleLoader} that loads all Jar files under specified paths
 */
public class JarModuleLoader implements ModuleLoader {
    private static final Logger log = LogManager.getLogger(JarModuleLoader.class);

    @Getter
    private final ModuleExportStrategy strategy;

    /**
     * Default constructor that sets {@link #strategy} to {@link JarModuleExportStrategy}
     */
    public JarModuleLoader() {
        this.strategy = new JarModuleExportStrategy();
    }

    /**
     * Constructor accepting a custom export strategy
     *
     * @param strategy {@link ModuleExportStrategy strategy} for exporting transformed classes under this path. Default strategy is {@link JarModuleExportStrategy}
     */
    public JarModuleLoader(final ModuleExportStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ModuleInfo> loadPackages(PreprocessConfig config) {
        if (config == null || config.getJarPaths() == null) {
            throw new NoModuleToInstrumentException();
        }

        final List<ModuleInfo> packageEntries = new ArrayList<>();

        for (String path : config.getJarPaths()) {
            for (File file : discoverFilesInPath(path)) {
                final ModuleInfo info = loadPackage(file);

                if (info != null) {
                    packageEntries.add(info);
                }
            }
        }
        if (packageEntries.isEmpty()) {
            throw new NoModuleToInstrumentException();
        }
        return packageEntries;
    }

    /**
     * Discovers all files under a path
     *
     * @return a List of {@link File files}, empty is no files found
     */
    protected List<File> discoverFilesInPath(final String path) {
        final List<File> files = new ArrayList<>();
        final File packageDir = new File(path);

        final File[] packageFiles = packageDir.listFiles();

        if (packageFiles == null) {
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "No packages found under path: " + path);
            return files;
        }

        files.addAll(Arrays.asList(packageFiles));
        return files;
    }

    /**
     * Helper method to load one single Jar file
     *
     * @param file Jar file to be loaded
     * @return {@link ModuleInfo object} containing package data, null if file is not a valid {@link JarFile}
     */
    protected ModuleInfo loadPackage(final File file) {
        final JarFile jarFile = processFile(file);

        if (jarFile == null) return null;

        final List<String> names = new ArrayList<>();

        for (JarEntry entry : extractEntries(jarFile)) {
            names.add(entry.getName().substring(0, entry.getName().lastIndexOf(".class")).replace('/', '.'));
        }

        return names.isEmpty() ? null : new ModuleInfo(file, jarFile, names, strategy);
    }

    /**
     * Helper method that iterates and extracts {@link JarEntry entries} that are class files
     *
     * @param jarFile Jar to explore
     * @return a list of {@link JarEntry entries} that are class files
     */
    protected List<JarEntry> extractEntries(final JarFile jarFile) {
        final List<JarEntry> result = new ArrayList<>();

        if (jarFile != null) {
            final Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();

                if (!e.isDirectory() && e.getName().endsWith(".class")) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    /**
     * Validates the file and adds it to the system class path
     *
     * @param file file to process
     * @return a valid {@link JarFile}, null if Jar cannot be created from {@link File} passed in.
     */
    protected JarFile processFile(final File file) {
        if (file.isDirectory() || !file.getName().toLowerCase().contains(PreprocessConstants.JAR_EXTENSION)) return null;

        final JarFile jar = makeJarFile(file);

        if (jar != null) {
            injectFileToSystemClassPath(file);
        }
        return jar;
    }

    /**
     * Creates a {@link JarFile} from {@link File}.
     *
     * @param file file to construct the Jar file from
     * @return a valid {@link JarFile}, null if invalid
     */
    protected JarFile makeJarFile(final File file) {
        try {
            return new JarFile(file);
        } catch (IOException e) {
            log.error(PreprocessConstants.MESSAGE_PREFIX + "Failed to create JarFile from file: " + file.getName(), e);
            return null;
        }
    }

    /**
     * Add the file to the system class path using the {@link Injector injector} api.
     *
     * @param file Jar containing a set of classes to be added to the class path
     */
    protected void injectFileToSystemClassPath(final File file) {
        log.debug(PreprocessConstants.MESSAGE_PREFIX + "Injecting file to system class path: " + file.getName());
        Injector.addToSystemClasspath(Injector.createInstrumentation(), file);
    }
}
