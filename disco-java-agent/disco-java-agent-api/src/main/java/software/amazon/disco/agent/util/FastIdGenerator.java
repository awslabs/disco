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

package software.amazon.disco.agent.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates random alphanumeric strings, uniformly randomly selected from a set of at least 2^96 possible strings.
 * Not suitable for generating secret values in cryptographic applications.
 */
public class FastIdGenerator {
    public static final int LENGTH = 24;
    private static final String HEX_ALPHABET = "0123456789abcdef";
    private static final char[] HEX_ENCODING = buildEncodingArray();
    private static char[] buildEncodingArray() {
        char[] encoding = new char[512];
        for (int i = 0; i < 256; ++i) {
            encoding[i] = HEX_ALPHABET.charAt(i >>> 4);
            encoding[i | 0x100] = HEX_ALPHABET.charAt(i & 0xF);
        }
        return encoding;
    }

    /**
     * Randomly generate an ID.
     * Uses thread local random to avoid performance bottlenecks with secure random.
     * Implementation adapted from AWS X-Ray SDK FastIdGenerator.java
     * @return a new random ID
     */
    public static String generate() {
        final byte[] bytes = new byte[LENGTH / 2];
        ThreadLocalRandom.current().nextBytes(bytes);
        char[] chars = new char[LENGTH];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX_ENCODING[v];
            chars[i * 2 + 1] = HEX_ENCODING[v | 0x100];
        }
        return new String(chars);
    }
}
