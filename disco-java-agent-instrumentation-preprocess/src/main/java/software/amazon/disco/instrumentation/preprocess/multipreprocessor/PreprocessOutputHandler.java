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

import lombok.Getter;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Output class for the preprocessing, responsible for collecting outputs from preprocessors,
 * printing outputs and parsing outputs to generate a unified summary of the preprocessing.
 */
public class PreprocessOutputHandler {
    private static final Logger log = LogManager.getLogger(PreprocessOutputHandler.class);
    /**
     * sign for the start of the item name in summary
     */
    private static final String START_SEPARATOR = "- ";
    /**
     * sign for the end of the item name in summary
     */
    private static final String END_SEPARATOR = ": ";
    private final List<String> preprocessorOutputs;

    @Getter
    final private Map<String, Integer> summary = new LinkedHashMap<String, Integer>() {{
        put(PreprocessConstants.SUMMARY_ITEM_SOURCES_PROCESSED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SOURCES_INSTRUMENTED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_DISCOVERED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED, 0);
        put(PreprocessConstants.SUMMARY_ITEM_SOURCES_WITH_UNRESOLVABLE_DEPENDENCIES, 0);
    }};

    public PreprocessOutputHandler(List<String> preprocessorOutputs) {
        this.preprocessorOutputs = preprocessorOutputs;
    }

    /**
     * Print output of the preprocessing. It will process all the outputs from preprocessors,
     * print and parse each output, and print a final summary of the whole preprocessing.
     */
    public void printPreprocessOutput() {
        processAllOutputs();
        logPreprocessSummary();
    }

    /**
     * Print and parse output of each preprocessor.
     */
    protected void processAllOutputs() {
        for (String preprocessorOutput : preprocessorOutputs) {
            log.info(preprocessorOutput);
            parsePreprocessorOutputAndUpdateSummary(preprocessorOutput);
        }
    }

    /**
     * Parse the instrumentation summary from a preprocessor and update the summary of the preprocessing
     *
     * @param preprocessorOutput output from a preprocessor
     *                           For example, a preprocessor output with summary like
     *                           Disco(Instrumentation preprocess) - Preprocessing summary
     *                           Disco(Instrumentation preprocess) - Sources processed: 1
     *                           Disco(Instrumentation preprocess) - Sources instrumented: 1
     *                           Disco(Instrumentation preprocess) - Signed jars discovered: 0
     *                           Disco(Instrumentation preprocess) - Signed jars instrumented: 0
     *                           Disco(Instrumentation preprocess) - Sources containing classes with unresolvable dependencies: 0
     *                           The summary map of PreprocessOutputHandler will be updated with item data parsed from preprocessor's summary.
     *                           If the summary was initial with empty values, it will be updated to
     *                           summary = {SUMMARY_ITEM_SOURCES_PROCESSED = 1, SUMMARY_ITEM_SOURCES_INSTRUMENTED = 1,
     *                           SUMMARY_ITEM_SIGNED_JARS_DISCOVERED = 0, SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED = 0,
     *                           SUMMARY_ITEM_SOURCES_WITH_UNRESOLVABLE_DEPENDENCIES = 0}
     */
    protected void parsePreprocessorOutputAndUpdateSummary(String preprocessorOutput) {
        try {
            int indexOfSummaryTitle = preprocessorOutput.indexOf(PreprocessConstants.SUMMARY_TITLE);
            String summaryContent = preprocessorOutput.substring(indexOfSummaryTitle);
            Scanner scanner = new Scanner(summaryContent);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // if it is the line for summary title, or it doesn't contain these separators, skip the line
                if (line.contains(PreprocessConstants.SUMMARY_TITLE) || !line.contains(START_SEPARATOR) || !line.contains(END_SEPARATOR)) {
                    continue;
                }
                // the start index of the item name, e.g. index of "Sources processed"
                int indexOfItemName = line.indexOf(START_SEPARATOR) + START_SEPARATOR.length();
                // the start index of the item value, which is also the end index of the item name
                int indexOfItemValue = line.indexOf(END_SEPARATOR) + END_SEPARATOR.length();
                // the output string that an item used in the actual summary, e.g. "Sources processed: "
                String item = line.substring(indexOfItemName, indexOfItemValue);
                if (summary.containsKey(item)) {
                    int itemValue = Integer.parseInt(line.substring(indexOfItemValue));
                    summary.put(item, summary.get(item) + itemValue);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse the preprocessor output" + e.getMessage());
        }
    }

    /**
     * Log preprocessing summary
     */
    protected void logPreprocessSummary() {
        log.info(PreprocessConstants.MESSAGE_PREFIX + PreprocessConstants.SUMMARY_TITLE);
        for (Map.Entry<String, Integer> item : summary.entrySet()) {
            log.info(PreprocessConstants.MESSAGE_PREFIX + item.getKey() + item.getValue());
        }
    }
}
