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

package software.amazon.disco.instrumentation.preprocess.instrumentation;

import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Class which holds the outcome of the preprocessing of a given source.
 */
@Builder
@Getter
public class InstrumentationOutcome {
    /**
     * 'COMPLETED' means at least one class file was transformed and saved to a file and that no failures occurred.
     * 'WARNING_OCCURRED' means at least one class file failed to be transformed and triggered an exception that was caught and ignored.
     * 'NO_OP' means no failure occurred and no classes were transformed.
     * <p>
     * Uncaught exceptions are considered as fatal and will be propagated upwards, effectively short-circuiting the entire Build-Time
     * Instrumentation process.
     */
    public enum Status {COMPLETED, WARNING_OCCURRED, NO_OP;}

    private Status status;
    private SourceInfo sourceInfo;
    private String sourcePath;
    private String artifactPath;
    private List<String> failedClasses;

    public boolean hasFailed() {
        return failedClasses != null && !failedClasses.isEmpty();
    }
}
