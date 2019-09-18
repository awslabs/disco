/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazon.disco.agent;

/**
 * Thrown when there is an issue with HTTP communication between Alpha One components
 */
public class AlphaOneHttpException extends Exception {
    private static final long serialVersionUID = -128389147239848L;

    /**
     * Constructs new instance of this exception, with specified message.
     *
     * @param message the message describing exception
     */
    public AlphaOneHttpException(String message) {
        super(message);
    }

    /**
     * Constructs new instance of this exception, with specified message and cause
     *
     * @param message the message describing exception
     * @param cause the cause of this exception
     */
    public AlphaOneHttpException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs new instance of this exception, with specified cause
     *
     * @param cause the cause of this exception
     */
    public AlphaOneHttpException(Throwable cause) {
        super(cause);
    }
}
