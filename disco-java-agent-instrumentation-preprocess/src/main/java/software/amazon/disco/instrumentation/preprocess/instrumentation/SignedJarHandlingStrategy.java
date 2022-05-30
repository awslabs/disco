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

import software.amazon.disco.instrumentation.preprocess.util.JarSigningVerificationOutcome;

/**
 * Strategy to be supplied by the user in regard to how to handle a signed Jar.
 */
public interface SignedJarHandlingStrategy {

    /**
     * Method to be called to determine whether the configured strategy should short circuit the Jar loading process once the Jar verification outcome is obtained.
     *
     * @param outcome outcome of the Jar verification process.
     * @return true if the Jar loading process should be short-circuited, false otherwise.
     */
    boolean skipJarLoading(final JarSigningVerificationOutcome outcome);
}
