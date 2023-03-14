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

package software.amazon.disco.instrumentation.preprocess.exceptions;

import java.util.Arrays;

/**
 * Exception thrown when the process has abnormal termination.
 */
public class ProcessInstrumentationAbortedException extends RuntimeException {
    /**
     * Constructor
     *
     * @param exitCode the exit code if the process
     * @param commandlineArgs command line arguments of the process
     * @param outputWithErrorMsg process's output with error message
     */
    public ProcessInstrumentationAbortedException(int exitCode, String[] commandlineArgs, String outputWithErrorMsg) {
        super("Process has terminated with exit code " + exitCode + System.lineSeparator()
                + " Commandline arguments: " + Arrays.toString(commandlineArgs) + System.lineSeparator()
                + " Preprocessor output: " + outputWithErrorMsg
        );
    }
}
