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

import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

/**
 * Exception thrown when the {@link ExportStrategy exporter}
 * fails to read an exiting entry from the original Jar file.
 */
public class JarEntryReadException extends RuntimeException {
    /**
     * Constructor
     *
     * @param entryName {@link java.util.jar.JarEntry} that failed to be copied
     * @param cause     {@link Throwable cause} of the failure for tracing the root cause.
     */
    public JarEntryReadException(String entryName, Throwable cause) {
        super(PreprocessConstants.MESSAGE_PREFIX + "Failed to read Jar entry: " + entryName, cause);
    }
}
