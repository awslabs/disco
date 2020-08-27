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

import java.util.List;

/**
 * Interface to load byte[] of compiled classes to be instrumented
 */
public interface ClassFileLoader {
    /**
     * Loads bytecode of classes and aggregate them into a single list of {@link JarInfo}.
     *
     * @param config a PreprocessConfig containing information to perform module instrumentation
     * @return list of {@link JarInfo} loaded by this package loader. Empty if no modules found.
     */
    List<JarInfo> load(PreprocessConfig config);
}
