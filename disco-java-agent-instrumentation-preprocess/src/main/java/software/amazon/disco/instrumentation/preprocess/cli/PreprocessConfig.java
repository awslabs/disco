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

package software.amazon.disco.instrumentation.preprocess.cli;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.logging.log4j.Level;

import java.util.List;

/**
 * Container for the config created from the command line args
 */
@Builder
@Getter
public class PreprocessConfig {
    private final String outputDir;
    @Singular
    private final List<String> jarPaths;
    private final String agentPath;
    private final String suffix;
    private final Level logLevel;
    private final String serializationJarPath;
}
