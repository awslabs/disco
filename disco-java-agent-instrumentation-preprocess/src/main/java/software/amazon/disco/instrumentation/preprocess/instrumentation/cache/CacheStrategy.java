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

import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.PreprocessCacheException;

import java.io.File;
import java.nio.file.Path;

/**
 * Caching strategy to be applied to prevent already processed Jars from being re-processed to optimize build time if instrumentation contexts such as the Disco Agent,
 * Disco plugins, Preprocessor and the input to be processed haven't changed.
 * <p>
 * The high level flow of how an implementation of this interface interacts with the preprocessing flow can be expressed in the following steps:
 * 1- An instance of {@link PreprocessConfig} is parsed from the commandline args.
 * 2- A particular implementation of {@link CacheStrategy} is instantiated.
 * 3- The instantiated cache strategy scans for a manifest file containing the instrumentation context and input sources processed during previous runs.
 * 4- Relying on the runtime cache, the configured cache strategy would filter out input sources to be processed that have already been processed, including the JDK base module.
 * 5- The workload partitioner will take the config file and partition/distribute workload to one or multiple instances of Preprocessor.
 * 6- Each instance of the Preprocessor would instantiate its own instance of {@link CacheStrategy} with an empty runtime cache (since the {@link #isSourceCached(Path)} check no longer needs to be performed).
 * 7- Records are inserted into the runtime cache as input sources are being processed.
 * 8- When an instance of Preprocessor has finished its work, its runtime cache will be serialized into a temporary manifest file uniquely named to its running process.
 * 9- After all instances of Preprocessor have completed their workload, the temporary manifest files generated at step 9 will be merged into the original runtime cache constructed at step 3.
 * 10- The updated runtime cache will be serialized which effectivity updates the original manifest file.
 */
public interface CacheStrategy {
    /**
     * Get the simple name of the cache strategy. This name is also used to specify which strategy to instantiate when provided as the value for
     * the '--cachestrategy' command line argument.
     *
     * @return string representing the simple name of this strategy.
     */
    String getSimpleName();

    /**
     * Loads the cache manifest file into the runtime cache to be used to determine whether an input source should be skipped from being processed again.
     *
     * @param config the PreprocessConfig instance
     */
    void loadManifestFileToRuntimeCache(final PreprocessConfig config) throws PreprocessCacheException;

    /**
     * Cache a particular dependency that has been processed.
     *
     * @param path path to the input source to be cached
     */
    void cacheSource(final Path path) throws PreprocessCacheException;

    /**
     * Verify whether the provided dependency has been cached.
     *
     * @param path dependency to verify
     * @return true if cached, false otherwise
     */
    boolean isSourceCached(final Path path) throws PreprocessCacheException;

    /**
     * Save the runtime cache composed of the outcomes of processing all the input sources provided by a particular instance of preprocessor
     * to a temporary manifest file, under a dedicated folder under the {@link PreprocessConfig#outputDir} provided, meant to be merged into
     * a single manifest file.
     *
     * @param config Preprocessor config
     */
    void serializeRuntimeCacheToTempManifestFile(final PreprocessConfig config) throws PreprocessCacheException;

    /**
     * Merge all discovered temporary manifest files into a single manifest file place at the root of the provided the {@link PreprocessConfig#outputDir}.
     *
     * @param outputDir output directory where instrumentation artifacts will the stored.
     */
    void mergeTempCacheManifests(final File outputDir) throws PreprocessCacheException;
}
