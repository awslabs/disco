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
 * Exception thrown when an error has occurred during the export process
 */
public class ExportException extends RuntimeException {
    /**
     * Constructor that accepts a message explaining why the module export process failed and
     * a {@link Throwable} instance for tracing.
     *
     * @param message cause of the failure
     * @param cause   {@link Throwable cause} of the failure for tracing the root cause.
     */
    public ExportException(String message, Throwable cause) {
        super(PreprocessConstants.MESSAGE_PREFIX + message, cause);
    }
}
