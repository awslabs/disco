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

package software.amazon.disco.instrumentation.preprocess.export;

import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentedClassState;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.ModuleInfo;

import java.util.Map;

/**
 * Interface for the strategy to use when exporting transformed classes
 */
public interface ModuleExportStrategy {
    /**
     * Strategy called to export all transformed classes.
     *
     * @param info                 Information of the original Jar
     * @param instrumented         a map of instrumented classes with their bytecode
     * @param suffix               suffix to be appended to the transformed package
     */
    void export(final ModuleInfo info, final Map<String, InstrumentedClassState> instrumented, final String suffix);
}
