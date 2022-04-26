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

package software.amazon.disco.instrumentation.preprocess.instrumentation;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class InstrumentationArtifactTest {
    private static final String INSTALLABLE_ID_1 = "id_1";
    private static final String INSTALLABLE_ID_2 = "id_2";
    private static final byte[] INITIAL= new byte[]{123};
    private static final byte[] UPDATED= new byte[]{45};

    @Test
    public void testConstructorWorks(){
        InstrumentationArtifact instrumentedType = new InstrumentationArtifact(INSTALLABLE_ID_1, INITIAL);

        assertArrayEquals(INITIAL, instrumentedType.getClassBytes());
        assertTrue(instrumentedType.getInstallableIds().contains(INSTALLABLE_ID_1));

        InstrumentationArtifact dependencyType = new InstrumentationArtifact(INITIAL);
        assertArrayEquals(INITIAL, dependencyType.getClassBytes());
    }

    @Test
    public void testUpdateWorks(){
        InstrumentationArtifact data = new InstrumentationArtifact(INSTALLABLE_ID_1, INITIAL);
        data.update(INSTALLABLE_ID_2, UPDATED);

        assertTrue(data.getClassBytes().equals(UPDATED));
        assertTrue(data.getInstallableIds().contains(INSTALLABLE_ID_1));
        assertTrue(data.getInstallableIds().contains(INSTALLABLE_ID_2));
    }
}
