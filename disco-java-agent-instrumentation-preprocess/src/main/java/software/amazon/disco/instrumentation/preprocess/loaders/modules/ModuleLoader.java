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

import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;

import java.util.List;

/**
 * Interface for ModuleLoaders that load modules to be instrumented
 */
public interface ModuleLoader {
    /**
     * Loads all the modules found under the paths specified and aggregate them into a single list of {@link ModuleInfo}.
     * Names of all the classes within each package are discovered and stored inside {@link ModuleInfo}.
     *
     * @param config a PreprocessConfig containing information to perform module instrumentation
     *
     * @return list of {@link ModuleInfo} loaded by this package loader. Empty if no modules found.
     */
    List<ModuleInfo> loadPackages(PreprocessConfig config);
}
