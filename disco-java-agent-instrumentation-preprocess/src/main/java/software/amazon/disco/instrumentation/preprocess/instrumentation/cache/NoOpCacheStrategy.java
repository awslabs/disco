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
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.nio.file.Path;

/**
 * NO-OP Implementation of {@link CacheStrategy}.
 */
public class NoOpCacheStrategy implements CacheStrategy {
    private static final Logger logger = LogManager.getLogger(NoOpCacheStrategy.class);
    private static final String SIMPLE_NAME = "none";

    @Override
    public boolean isSourceCached(final Path path) {
        return false;
    }

    @Override
    public String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    public void loadManifestFileToRuntimeCache(final PreprocessConfig config) {
        logger.info(PreprocessConstants.MESSAGE_PREFIX + "No cache strategy specified. No-Op strategy is configured by default.");
    }

    @Override
    public void cacheSource(final Path path) {
        // no-op
    }

    @Override
    public void serializeRuntimeCacheToTempManifestFile(final PreprocessConfig config) {
        // no-op
    }

    @Override
    public void mergeTempCacheManifests(final File outputDir) {
        // no-op
    }
}
