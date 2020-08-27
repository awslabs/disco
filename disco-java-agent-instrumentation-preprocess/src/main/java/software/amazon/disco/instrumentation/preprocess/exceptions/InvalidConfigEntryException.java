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

package software.amazon.disco.instrumentation.preprocess.exceptions;

import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

/**
 * Exception thrown when encountering an entry from {@link software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig}
 * that is invalid.
 */
public class InvalidConfigEntryException extends RuntimeException {
    /**
     * Constructor that calls its parent with a fixed message
     *
     * @param configEntry config entry that is invalid
     * @pram t cause of the error
     */
    public InvalidConfigEntryException(String configEntry, Throwable t) {
        super(PreprocessConstants.MESSAGE_PREFIX + "Invalid configuration entry: " + configEntry, t);
    }
}
