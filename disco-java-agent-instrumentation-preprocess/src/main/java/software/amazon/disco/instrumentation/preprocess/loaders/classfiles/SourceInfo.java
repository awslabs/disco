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

import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.JarSigningVerificationOutcome;

import java.io.File;
import java.util.Map;

/**
 * Class that holds data of a Jar package that has been loaded by a {@link ClassFileLoader} including the export strategy
 * that will be used to store the transformed classes.
 */
@AllArgsConstructor
@Getter
public class SourceInfo {
    private final File sourceFile;
    private final ExportStrategy exportStrategy;
    private final Map<String, byte[]> classByteCodeMap;
    private final JarSigningVerificationOutcome jarSigningVerificationOutcome;

    /**
     * Constructor
     *
     * @param sourceFile       source file where compiled class files were retrieved from. Can either be a folder or a Jar.
     * @param exportStrategy   strategy to be used to export static instrumentation artifacts.
     * @param classByteCodeMap a map containing the byte code of discovered class files mapped to their relative paths.
     */
    public SourceInfo(File sourceFile, ExportStrategy exportStrategy, Map<String, byte[]> classByteCodeMap) {
        this.sourceFile = sourceFile;
        this.exportStrategy = exportStrategy;
        this.classByteCodeMap = classByteCodeMap;
        this.jarSigningVerificationOutcome = null;
    }
}
