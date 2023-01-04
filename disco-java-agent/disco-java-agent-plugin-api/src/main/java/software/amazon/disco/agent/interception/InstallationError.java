/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.interception;

/**
 * A descriptor of an installation error detected by an {@link Installable} after installation. Not a Throwable because
 * it's not intended to be thrown.
 */
public class InstallationError {
    /**
     * A short description of the symptoms of the error.
     */
    public final String description;

    public InstallationError(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
