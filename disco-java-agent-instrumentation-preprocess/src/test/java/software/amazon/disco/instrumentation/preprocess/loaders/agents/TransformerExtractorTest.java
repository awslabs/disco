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

package software.amazon.disco.instrumentation.preprocess.loaders.agents;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public class TransformerExtractorTest {
    @After
    public void after(){
        TransformerExtractor.getTransformers().clear();
    }

    @Test
    public void testAddTransformerWorks(){
        ClassFileTransformer classFileTransformer = Mockito.mock(ClassFileTransformer.class);
        Instrumentation delegate = Mockito.mock(Instrumentation.class);

        TransformerExtractor extractor =  new TransformerExtractor(delegate);
        extractor.addTransformer(classFileTransformer);
        extractor.addTransformer(classFileTransformer);
        extractor.addTransformer(classFileTransformer);

        Assert.assertEquals(3, TransformerExtractor.getTransformers().size());
    }
}
