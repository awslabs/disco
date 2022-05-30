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
 * Strategy to be used if the user has determined that no signed Jars should be statically instrumented. Beware that this strategy may result in instrumentation gap if a
 * signed Jar contains a target class that performs some critical business logic meant to be instrumented. E.g. a subclass of Thread used for making downstream calls.
 */
public class SkipSignedJarHandlingStrategy implements SignedJarHandlingStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean skipJarLoading(final JarSigningVerificationOutcome outcome) {
        // in the event where a Jar is signed and the 'SignedJarHandlingStrategy' strategy was configured as this implementation, the Jar loading process will be short-circuited
        // effectively preventing the signed Jar from being statically instrumented.
        return outcome.equals(JarSigningVerificationOutcome.SIGNED);
    }
}
