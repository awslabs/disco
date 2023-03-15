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

package software.amazon.disco.instrumentation.preprocess.loaders.classfiles;

import org.junit.Test;
import software.amazon.disco.instrumentation.preprocess.util.JarSigningVerificationOutcome;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class SourceInfoTest {

    @Test
    public void testIsJarSignedReturnsTrue_WhenSigned() {
        SourceInfo sourceInfo = new SourceInfo(null, null, null, JarSigningVerificationOutcome.SIGNED);

        assertTrue(sourceInfo.isJarSigned());
    }

    @Test
    public void testIsJarSignedReturnsFalse_WhenUnsigned() {
        SourceInfo sourceInfo = new SourceInfo(null, null, null, JarSigningVerificationOutcome.UNSIGNED);

        assertFalse(sourceInfo.isJarSigned());
    }

    @Test
    public void testIsJarSignedReturnsFalse_WhenInvalid() {
        SourceInfo sourceInfo = new SourceInfo(null, null, null, JarSigningVerificationOutcome.INVALID);

        assertFalse(sourceInfo.isJarSigned());
    }

    @Test
    public void testIsJarSignedReturnsFalse_WhenNull() {
        SourceInfo sourceInfo = new SourceInfo(null, null, null, null);

        assertFalse(sourceInfo.isJarSigned());
    }
}