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

package software.amazon.disco.instrumentation.preprocess.instrumentation.cache;

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.PreprocessCacheException;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.JDKModuleLoader;
import software.amazon.disco.instrumentation.preprocess.util.FileUtils;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Implementation of {@CacheStrategy} which relies on the 'md5' algorithm to compute the checksum of processed sources for caching purposes. The data structure elected to store
 * caching related data is an instance of {@link Properties} which can be easily serialized and de-serialized.
 */
public class ChecksumCacheStrategy implements CacheStrategy {
    static final String SIMPLE_NAME = "checksum";
    static final String MANIFEST_FILE_NAME = "preprocessing_cache.properties";
    static final String PLUGINS_CHECKSUM_PROPERTY_KEY = "PLUGINS_CHECKSUM";
    static final String AGENT_CHECKSUM_PROPERTY_KEY = "AGENT_CHECKSUM";
    static final String PREPROCESSOR_CHECKSUM_PROPERTY_KEY = "PREPROCESSOR_CHECKSUM";
    static final String JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY = "JAVA_RUNTIME_BASE_MODULE_CHECKSUM";
    static final String PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY = "PREPROCESSOR_CONFIG_STRING";
    static final String TEMPORARY_MANIFEST_FOLDER_NAME = "tmp/cache_manifest_files";

    private static final Logger logger = LogManager.getLogger(ChecksumCacheStrategy.class);
    private static final String CHECK_SUM_ALGORITHM = "md5";
    private static final Set<String> INSTRUMENTATION_CONTEXT_PROPERTIES = new HashSet<>();

    Properties runtimeCache;
    Map<String, String> currentInstrumentationContext;

    static {
        INSTRUMENTATION_CONTEXT_PROPERTIES.addAll(Arrays.asList(
            PLUGINS_CHECKSUM_PROPERTY_KEY,
            AGENT_CHECKSUM_PROPERTY_KEY,
            PREPROCESSOR_CHECKSUM_PROPERTY_KEY,
            JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY,
            PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY
        ));
    }

    /**
     * Constructor
     */
    public ChecksumCacheStrategy() {
        runtimeCache = new Properties();
        currentInstrumentationContext = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSimpleName() {
        return SIMPLE_NAME;
    }

    /**
     * Initializes the checksum caching strategy. A single manifest file containing information on previously processed sources will be parsed and loaded. The loaded manifest
     * will later be used by {@link software.amazon.disco.instrumentation.preprocess.cli.Driver#removeCachedInputSourcesForProcessing(PreprocessConfig)} to determine whether
     * an input source should be skipped from being statically instrumented.
     *
     * @param config the PreprocessConfig instance
     * @throws PreprocessCacheException
     */
    @Override
    public void loadManifestFileToRuntimeCache(final PreprocessConfig config) throws PreprocessCacheException {
        logger.info(PreprocessConstants.MESSAGE_PREFIX + "Initializing ChecksumCacheStrategy...");

        currentInstrumentationContext = computeCurrentInstrumentationContext(config);

        final File cacheManifestFile = new File(config.getOutputDir(), MANIFEST_FILE_NAME);
        logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Scanning for manifest file at: " + cacheManifestFile.getAbsolutePath());
        if (!cacheManifestFile.exists()) {
            logger.info(PreprocessConstants.MESSAGE_PREFIX + "Manifest file doesn't exist. All input sources will be processed.");
        } else {
            runtimeCache = readPropertiesFromFile(cacheManifestFile);
            logger.info(PreprocessConstants.MESSAGE_PREFIX + "Runtime cache constructed from loaded manifest file.");
            validateCachedInstrumentationContext();
        }

        runtimeCache.putAll(currentInstrumentationContext);

        // deletes the temporary manifest folder which may contain temporary caching artifacts from previous builds
        final File temporaryManifestDir = new File(config.getOutputDir(), TEMPORARY_MANIFEST_FOLDER_NAME);
        if (temporaryManifestDir.exists()) {
            for (File manifestFile : temporaryManifestDir.listFiles()) {
                manifestFile.delete();
            }
        }
    }

    /**
     * Check if the supplied source is cached
     *
     * @param path path to the Jar to be verified
     * @return true if cache entry corresponding to the source is present in the runtime cache, false otherwise. Failure to compute the checksum
     * would also result in a return value of 'false'
     * @throws PreprocessCacheException
     */
    @Override
    public boolean isSourceCached(final Path path) throws PreprocessCacheException {
        if (runtimeCache.isEmpty()) {
            return false;
        }
        final String checksum = computeChecksum(path);
        return checksum == null ? false : checksum.equals(runtimeCache.getProperty(path.toString()));
    }

    /**
     * Cache a Jar that has been processed in the current run by storing its checksum in a runtime cache. The runtime cache will then
     * be serialized to a Properties file later.
     *
     * @param path path to the Jar to be cached
     * @throws PreprocessCacheException
     */
    @Override
    public void cacheSource(final Path path) throws PreprocessCacheException {
        logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Caching: " + path);

        final String checksum = computeChecksum(path);
        final String pathStr = path.toString();

        runtimeCache.setProperty(pathStr, checksum);
    }

    /**
     * Validate the instrumentation context data cached in the manifest file to determine whether the data is complete and whether the loaded cache should be discarded if the instrumentation
     * context has changed, e.g. the Disco agent Jar checksum.
     *
     * @throws PreprocessCacheException errors thrown while parsing the loaded cache
     */
    void validateCachedInstrumentationContext() throws PreprocessCacheException {
        for (String property : INSTRUMENTATION_CONTEXT_PROPERTIES) {
            final String propertyValue = runtimeCache.getProperty(property);
            if (propertyValue == null) {
                throw new PreprocessCacheException("Property missing from loaded manifest file: " + property);
            }
        }

        final List<String> instrumentationContextDeltaList = computeInstrumentationContextDelta(runtimeCache, currentInstrumentationContext);
        if (!instrumentationContextDeltaList.isEmpty()) {
            logger.info(PreprocessConstants.MESSAGE_PREFIX + "Instrumentation context has changed compared to previous build, cache will be reset.");

            for (String element : instrumentationContextDeltaList) {
                logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Instrumentation context has changed compared for: " + element);
            }

            runtimeCache.clear();
        }
    }

    /**
     * Compute the checksum of the supplied Jar using the configured digest algorithm.
     *
     * @param path path to the Jar file to have its checksum computed
     * @return checksum computed
     * @throws PreprocessCacheException
     */
    String computeChecksum(final Path path) throws PreprocessCacheException {
        if (path == null) {
            logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Path provided to compute its checksum is null, skipping.");
            return null;
        }

        if (!path.toFile().exists()) {
            logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Path provided to compute its checksum does not exist, skipping.");
            return null;
        }

        if (path.toFile().isDirectory()) {
            logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Path provided to compute its checksum a directory, skipping.");
            return null;
        }

        try {
            final MessageDigest msgDst = MessageDigest.getInstance(CHECK_SUM_ALGORITHM);
            final byte[] fileContent = Files.readAllBytes(path);
            final byte[] msgArr = msgDst.digest(fileContent);

            return new BigInteger(1, msgArr).toString(16);
        } catch (Exception e) {
            throw new PreprocessCacheException(PreprocessConstants.MESSAGE_PREFIX + "Failed to compute checksum for file: " + path.toString(), e);
        }
    }

    /**
     * Compute the aggregate checksum of all discovered Disco plugins. This is done by joining individual plugin checksums in an alphabetical order
     * where each element is composed of "absolute_path_to_the_plugin=checksum_computed".
     *
     * @param config the PreprocessConfig instance
     * @return a string representing the aggregate checksum of all discovered plugins
     * @throws PreprocessCacheException
     */
    String computeChecksumForPlugins(final PreprocessConfig config) throws PreprocessCacheException {
        final StringBuilder pluginChecksumPropertyBuilder = new StringBuilder();
        final File[] plugins = FileUtils.scanPluginsFromAgentConfig(config.getAgentArg()).toArray(new File[0]);

        // sort plugin paths lexicographically to guarantee order
        Arrays.sort(plugins);

        for (File plugin : plugins) {
            pluginChecksumPropertyBuilder
                .append(pluginChecksumPropertyBuilder.length() == 0 ? "" : ",")
                .append(plugin.getAbsolutePath())
                .append("=")
                .append(computeChecksum(plugin.toPath()));
        }

        return pluginChecksumPropertyBuilder.toString();
    }

    /**
     * Serialize the runtime cache to a temporary manifest file uniquely named across multiple instances of the Preprocessor. These temporary
     * manifests will then be merged into a single source of truth. Each process will therefore write to its designated temporary manifest file
     * which eliminates the need to implement any file locking mechanism with unreliable retry tactics.
     *
     * @param config Preprocessor config
     * @throws PreprocessCacheException Errors thrown while attempting to serialize the runtime cache into a '.properties' file.
     */
    @Override
    public void serializeRuntimeCacheToTempManifestFile(final PreprocessConfig config) throws PreprocessCacheException {
        logger.info(PreprocessConstants.MESSAGE_PREFIX + "Serializing runtime cache to manifest file at: " + config.getOutputDir());
        logger.debug(PreprocessConstants.MESSAGE_PREFIX + runtimeCache.size() + " entries will be stored in the manifest file.");

        try {
            if (runtimeCache.isEmpty()) {
                logger.info(PreprocessConstants.MESSAGE_PREFIX + "No input sources were cached, skipping serialization.");
                return;
            }

            final File temporaryManifest = FileUtils.createTemporaryManifestFile(new File(config.getOutputDir(), TEMPORARY_MANIFEST_FOLDER_NAME));
            try (FileOutputStream fileOutputStream = new FileOutputStream(temporaryManifest)) {
                runtimeCache.putAll(computeCurrentInstrumentationContext(config));

                // save the runtime cache to the manifest file.
                runtimeCache.store(fileOutputStream, null);
            }
        } catch (Exception e) {
            throw new PreprocessCacheException("Failed to serialize runtime cache to temporary manifest file.", e);
        }
    }

    /**
     * Merges all temporary manifest files into a single source of truth to be parsed during future invocations of the Preprocessor.
     *
     * @param outputDir output dir where preprocessor artifacts will be stored
     * @throws PreprocessCacheException
     */
    @Override
    public void mergeTempCacheManifests(final File outputDir) throws PreprocessCacheException {
        final File temporaryManifestsDir = new File(outputDir, TEMPORARY_MANIFEST_FOLDER_NAME);

        if (!temporaryManifestsDir.exists() || temporaryManifestsDir.listFiles().length == 0) {
            return;
        }

        logger.info(PreprocessConstants.MESSAGE_PREFIX + "Merging " + temporaryManifestsDir.listFiles().length + " temporary manifest files.");

        for (File manifest : temporaryManifestsDir.listFiles()) {
            if (manifest.isDirectory()) {
                return;
            }
            final Properties propertiesToBeMerged = readPropertiesFromFile(manifest);

            logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Attempting to merge: " + manifest.getAbsolutePath());
            final List<String> instrumentationContextDeltaList = computeInstrumentationContextDelta(runtimeCache, propertiesToBeMerged);
            if (!instrumentationContextDeltaList.isEmpty()) {
                for (String element : instrumentationContextDeltaList) {
                    logger.error(PreprocessConstants.MESSAGE_PREFIX + "Inconsistent instrumentation context element: " + element);
                }
                throw new PreprocessCacheException("Preprocessor checksum retrieved from temporary manifest is inconsistent with the one stored in the runtime cache. Please perform a clean build.");
            }

            // merge temporary manifest into main
            runtimeCache.putAll(propertiesToBeMerged);
            logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Manifest file merged successfully.");
        }

        final File cacheManifestFile = new File(outputDir, MANIFEST_FILE_NAME);
        try (FileOutputStream fileOutputStream = new FileOutputStream(cacheManifestFile)) {
            // save the runtime cache to the permanent manifest file.
            runtimeCache.store(fileOutputStream, null);
            logger.info(PreprocessConstants.MESSAGE_PREFIX + "Serialized runtime cache to manifest file: " + cacheManifestFile.getAbsolutePath());
        } catch (Exception e) {
            throw new PreprocessCacheException("Failed to serialize manifest to file: " + cacheManifestFile.getAbsolutePath(), e);
        }
    }

    /**
     * Read the supplied properties file.
     *
     * @param file a file with '.properties' extension.
     * @return an instance of {@link Properties} representing the data contained in the read file.
     * @throws PreprocessCacheException
     */
    Properties readPropertiesFromFile(final File file) throws PreprocessCacheException {
        logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Loading manifest file: " + file.getAbsolutePath());

        try (InputStream input = new FileInputStream(file)) {
            final Properties properties = new Properties();

            properties.load(input);
            logger.debug(PreprocessConstants.MESSAGE_PREFIX + "Manifest file loaded");

            return properties;
        } catch (IOException e) {
            throw new PreprocessCacheException("Failed to read file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Compute the current instrumentation context.
     *
     * @param config Preprocessor config
     * @return a Map representing the current instrumentation context
     * @throws PreprocessCacheException any errors thrown while attempting to compute various checksums
     */
    Map<String, String> computeCurrentInstrumentationContext(final PreprocessConfig config) throws PreprocessCacheException {
        final Map<String, String> instrumentationContext = new HashMap<>();

        instrumentationContext.put(AGENT_CHECKSUM_PROPERTY_KEY, computeChecksum(Paths.get(config.getAgentPath())));
        instrumentationContext.put(PLUGINS_CHECKSUM_PROPERTY_KEY, computeChecksumForPlugins(config));
        instrumentationContext.put(PREPROCESSOR_CHECKSUM_PROPERTY_KEY, computeChecksum(getPreprocessorJarPath()));
        instrumentationContext.put(PREPROCESSOR_CONFIG_STRING_PROPERTY_KEY, config.toStringForCaching());
        instrumentationContext.put(JAVA_RUNTIME_BASE_MODULE_CHECKSUM_PROPERTY_KEY, computeChecksum(new JDKModuleLoader().getJDKBaseModule(System.getProperty("java.home")).toPath()));

        return instrumentationContext;
    }

    /**
     * Compute the delta of 2 instrumentation contexts to determine if there's any inconsistencies.
     *
     * @param context      an instrumentation context
     * @param contextOther another instrumentation context
     * @return a list of inconsistent instrumentation context elements, e.g. the checksum of the Disco Java agent.
     */
    List<String> computeInstrumentationContextDelta(final Map context, final Map contextOther) {
        final List<String> delta = new ArrayList<>();
        for (String property : INSTRUMENTATION_CONTEXT_PROPERTIES) {
            if (!context.get(property).equals(contextOther.get(property))) {
                logger.debug(PreprocessConstants.MESSAGE_PREFIX + "The value of the instrumentation context property is inconsistent: " + property);
                delta.add(property);
            }
        }

        return delta;
    }

    /**
     * Get the path to the Preprocessor source code. In the unit testing environment, this is the path to a folder composed of compiled class files instead of a path
     * to a Jar file.
     *
     * @return path to the Preprocessor source code
     * @throws PreprocessCacheException errors occurred while retrieving the path to the Preprocessor Jar location.
     */
    Path getPreprocessorJarPath() throws PreprocessCacheException {
        final CodeSource codeSource = CacheStrategy.class.getProtectionDomain().getCodeSource();

        if (codeSource == null) {
            throw new PreprocessCacheException("Failed to determine path to the preprocessor Jar. Code source is null.");
        }

        try {
            return new File(codeSource.getLocation().toURI().getPath()).toPath();
        } catch (Exception e) {
            throw new PreprocessCacheException("Failed to determine path to the Preprocessor Jar.", e);
        }
    }
}
