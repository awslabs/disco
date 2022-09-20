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

package software.amazon.disco.instrumentation.preprocess.util;

/**
 * Holds constant variables used throughout the library
 */
public class PreprocessConstants {
    public static final String MESSAGE_PREFIX = "Disco(Instrumentation preprocess) - ";
    public static final String JAR_EXTENSION = ".jar";
    public static final String SUMMARY_TITLE = "Preprocessing summary";
    public static final String SUMMARY_ITEM_SOURCES_PROCESSED = "Sources processed: ";
    public static final String SUMMARY_ITEM_SOURCES_INSTRUMENTED = "Sources instrumented: ";
    public static final String SUMMARY_ITEM_SIGNED_JARS_DISCOVERED = "Signed jars discovered: ";
    public static final String SUMMARY_ITEM_SIGNED_JARS_INSTRUMENTED = "Signed jars instrumented: ";
    public static final String SUMMARY_ITEM_SOURCES_WITH_UNRESOLVABLE_DEPENDENCIES = "Sources containing classes with unresolvable dependencies: ";
    public static final String PREPROCESSOR_ARGS_TEMP_FOLDER = "tmp/worker_args";
}
