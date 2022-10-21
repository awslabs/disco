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

package software.amazon.disco.instrumentation.preprocess.loaders.classfiles;

import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;

import java.nio.file.Path;

/**
 * Interface to load byte[] of compiled classes to be instrumented
 */
public interface ClassFileLoader {
    /**
     * Loads bytecode of classes and aggregate them into a single instance of {@link SourceInfo}. If the original source path was
     * a Symbolic link, the passed in 'path' param is the resolved file that the link was pointing to.
     *
     * @param path               path to the source to be statically instrumented
     * @param config             a PreprocessConfig containing information to perform preprocessing
     * @return an instance of {@link SourceInfo} loaded by this loader. null if no classes found.
     */
    SourceInfo load(Path path, PreprocessConfig config);
}
