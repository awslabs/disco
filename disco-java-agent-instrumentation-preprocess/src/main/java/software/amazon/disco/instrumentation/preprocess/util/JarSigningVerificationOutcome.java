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

package software.amazon.disco.instrumentation.preprocess.util;

import java.io.File;

/**
 * Enum to denote the outcome of the Jar verification process performed by {@link software.amazon.disco.instrumentation.preprocess.util.JarFileUtils#verifyJar(File)}
 * <p>
 * UNSIGNED
 * - The 'jarsigner' tool did not detect any Jar signing artifacts(certificate, signed manifest and so on...) after having scanned the Jar.
 * SIGNED
 * - The 'jarsigner' tool successfully verified the Jar and validated that it was not tampered with after being signed.
 * INVALID
 */
public enum JarSigningVerificationOutcome {
    UNSIGNED, SIGNED, INVALID
}
