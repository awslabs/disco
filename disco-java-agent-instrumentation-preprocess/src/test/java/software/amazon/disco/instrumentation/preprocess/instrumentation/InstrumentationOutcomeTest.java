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

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstrumentationOutcomeTest {
    @Test
    public void testHasFailedReturnsFalse_whenFailedClassesIsNull(){
        InstrumentationOutcome outcome = InstrumentationOutcome.builder()
            .failedClasses(null)
            .build();

        assertFalse(outcome.hasFailed());
    }

    @Test
    public void testHasFailedReturnsFalse_whenFailedClassesIsEmpty(){
        InstrumentationOutcome outcome = InstrumentationOutcome.builder()
            .failedClasses(Collections.emptyList())
            .build();

        assertFalse(outcome.hasFailed());
    }

    @Test
    public void testHasFailedReturnsTrue_whenFailedClassesIsNotEmpty(){
        InstrumentationOutcome outcome = InstrumentationOutcome.builder()
            .failedClasses(Collections.singletonList("SomeClass"))
            .build();

        assertTrue(outcome.hasFailed());
    }
}
