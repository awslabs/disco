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

package software.amazon.disco.agent.concurrent;

import org.junit.Assert;
import org.junit.Test;

public class TransactionIdentifierTests {
    private static final String HEX_ALPHABET = "0123456789abcdef";

    @Test
    public void testLength() {
        Assert.assertEquals(24, TransactionIdentifier.generate().length());
    }

    @Test
    public void testBitDistribution() {
        final int samples = 1_000_000;
        final double tolerance = 0.01;
        final int[] bits = new int[4 * TransactionIdentifier.LENGTH];
        for (int sample = 0; sample < samples; sample++) {
            final String id = TransactionIdentifier.generate();
            for (int i = 0; i < id.length(); i++) {
                int c = HEX_ALPHABET.indexOf(id.charAt(i));
                for (int k = 0; k < 4; k++) {
                    if (((c >> k) & 0x1) == 1) {
                        bits[4 * i + k]++;
                    }
                }
            }
        }
        boolean failure = false;
        double expectedDistribution = 0.5 * samples;
        for (int i = 0; i < bits.length; i++) {
            double error = (expectedDistribution - bits[i]) / expectedDistribution;
            if (Math.abs(error) > tolerance) {
                failure = true;
            }
        }
        Assert.assertFalse(failure);
    }
}
