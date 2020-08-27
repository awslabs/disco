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

import org.junit.Assert;
import org.junit.Test;

public class InstrumentedClassStateTest {
    private static final String INSTALLABLE_ID_1 = "id_1";
    private static final String INSTALLABLE_ID_2 = "id_2";
    private static final byte[] INITIAL= new byte[]{123};
    private static final byte[] UPDATED= new byte[]{45};

    @Test
    public void testConstructorWorks(){
        InstrumentedClassState data = new InstrumentedClassState(INSTALLABLE_ID_1, INITIAL);

        Assert.assertTrue(data.getClassBytes().equals(INITIAL));
        Assert.assertTrue(data.getInstallableIds().contains(INSTALLABLE_ID_1));
    }

    @Test
    public void testUpdateWorks(){
        InstrumentedClassState data = new InstrumentedClassState(INSTALLABLE_ID_1, INITIAL);
        data.update(INSTALLABLE_ID_2, UPDATED);

        Assert.assertTrue(data.getClassBytes().equals(UPDATED));
        Assert.assertTrue(data.getInstallableIds().contains(INSTALLABLE_ID_1));
        Assert.assertTrue(data.getInstallableIds().contains(INSTALLABLE_ID_2));
    }
}
