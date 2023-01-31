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

package software.amazon.disco.instrumentation.preprocess.multipreprocessor;

import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PreprocessOutputHandlerTest {
    private static final String OUTPUT_WITH_VALID_SUMMARY_A = PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_TITLE + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_PROCESSED + "3" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_INSTRUMENTED + "1" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_DISCOVERED + "1" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED + "0" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_WITH_UNRESOLVABLE_DEPENDENCIES + "0";
    private static final String OUTPUT_WITH_VALID_SUMMARY_B = PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_TITLE + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_PROCESSED + "3" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_INSTRUMENTED + "0" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_DISCOVERED + "1" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED + "0" + System.lineSeparator()
            + PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_WITH_UNRESOLVABLE_DEPENDENCIES + "0";
    private static final  Map<String, Integer> initialSummary = new LinkedHashMap<String, Integer>() {{
        put(PreprocessConstants.SUMMARY_ITEM_SOURCES_PROCESSED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SOURCES_INSTRUMENTED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_DISCOVERED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SOURCES_WITH_UNRESOLVABLE_DEPENDENCIES, 0);
    }};
    private static final String OUTPUT_WITH_INVALID_SUMMARY = "Invalid summary";
    private static final String OUTPUT_WITH_EMPTY_SUMMARY = " ";
    private static final String OUTPUT_WITH_SUMMARY_MISSING_ITEM = PreprocessConstants.MESSAGE_PREFIX + "3" + System.lineSeparator();
    private static final String OUTPUT_WITH_SUMMARY_HAS_NON_INT_ITEM = PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_ITEM_SOURCES_PROCESSED + "Not parsable string" + System.lineSeparator();

    @Test
    public void testPrintPreprocessOutputWorksAndInvokesHelperMethods() {
        List<String> preprocessorOutputs = Arrays.asList(OUTPUT_WITH_VALID_SUMMARY_A, OUTPUT_WITH_VALID_SUMMARY_B);
        PreprocessOutputHandler preprocessOutputHandler = configurePreprocessOutputHandler(preprocessorOutputs);
        preprocessOutputHandler.printPreprocessOutput();
        Mockito.verify(preprocessOutputHandler).processAllOutputs();
        Mockito.verify(preprocessOutputHandler).logPreprocessSummary();
    }

    @Test
    public void testProcessAllOutputsWorksAndUpdateSummaryMap() {
        List<String> preprocessorOutputs = Arrays.asList(OUTPUT_WITH_VALID_SUMMARY_A, OUTPUT_WITH_VALID_SUMMARY_B);
        PreprocessOutputHandler preprocessOutputHandler = configurePreprocessOutputHandler(preprocessorOutputs);
        preprocessOutputHandler.processAllOutputs();
        //invoke helper method
        Mockito.verify(preprocessOutputHandler, Mockito.times(preprocessorOutputs.size())).parsePreprocessorOutputAndUpdateSummary(Mockito.anyString());
        //update summary correctly
        assertEquals(6, preprocessOutputHandler.getSummary().get(PreprocessConstants.SUMMARY_ITEM_SOURCES_PROCESSED).intValue());
        assertEquals(1, preprocessOutputHandler.getSummary().get(PreprocessConstants.SUMMARY_ITEM_SOURCES_INSTRUMENTED).intValue());
        assertEquals(2, preprocessOutputHandler.getSummary().get(PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_DISCOVERED).intValue());
        assertEquals(0, preprocessOutputHandler.getSummary().get(PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED).intValue());
    }

    @Test
    public void testParsePreprocessorOutputWithInvalidSummary() {
        List<String> preprocessorOutputs = Arrays.asList(OUTPUT_WITH_INVALID_SUMMARY);
        PreprocessOutputHandler preprocessOutputHandler = configurePreprocessOutputHandler(preprocessorOutputs);
        //no exception will be thrown
        preprocessOutputHandler.parsePreprocessorOutputAndUpdateSummary(OUTPUT_WITH_INVALID_SUMMARY);
        //summary map will not get updated
        assertEquals(initialSummary, preprocessOutputHandler.getSummary());
    }

    @Test
    public void testParsePreprocessorOutputWithEmptySummary() {
        List<String> preprocessorOutputs = Arrays.asList(OUTPUT_WITH_EMPTY_SUMMARY);
        PreprocessOutputHandler preprocessOutputHandler = configurePreprocessOutputHandler(preprocessorOutputs);
        //no exception will be thrown
        preprocessOutputHandler.parsePreprocessorOutputAndUpdateSummary(OUTPUT_WITH_EMPTY_SUMMARY);
        //summary map will not get updated
        assertEquals(initialSummary, preprocessOutputHandler.getSummary());
    }

    @Test
    public void testParsePreprocessorOutputWithSummaryMissingItem() {
        List<String> preprocessorOutputs = Arrays.asList(OUTPUT_WITH_SUMMARY_MISSING_ITEM);
        PreprocessOutputHandler preprocessOutputHandler = configurePreprocessOutputHandler(preprocessorOutputs);
        //no exception will be thrown
        preprocessOutputHandler.parsePreprocessorOutputAndUpdateSummary(OUTPUT_WITH_SUMMARY_MISSING_ITEM);
        //summary map will not get updated
        assertEquals(initialSummary, preprocessOutputHandler.getSummary());
    }

    @Test
    public void testParsePreprocessorOutputWithSummaryItemValueNotInt() {
        List<String> preprocessorOutputs = Arrays.asList(OUTPUT_WITH_SUMMARY_HAS_NON_INT_ITEM);
        PreprocessOutputHandler preprocessOutputHandler = configurePreprocessOutputHandler(preprocessorOutputs);
        //no exception will be thrown
        preprocessOutputHandler.parsePreprocessorOutputAndUpdateSummary(OUTPUT_WITH_SUMMARY_HAS_NON_INT_ITEM);
        //summary map will not get updated
        assertEquals(initialSummary, preprocessOutputHandler.getSummary());
    }

    private PreprocessOutputHandler configurePreprocessOutputHandler(List<String> preprocessorOutputs) {
        PreprocessOutputHandler preprocessOutputHandler = Mockito.spy(new PreprocessOutputHandler(preprocessorOutputs));
        return preprocessOutputHandler;
    }
}
