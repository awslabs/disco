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

package software.amazon.disco.instrumentation.preprocess.exceptions;

import software.amazon.disco.instrumentation.preprocess.instrumentation.StaticInstrumentationTransformer;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.ClassFileLoader;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

/**
 * Exception thrown when a valid {@link ClassFileLoader} is not provided to
 * {@link StaticInstrumentationTransformer}
 */
public class ClassFileLoaderNotProvidedException extends RuntimeException {
    /**
     * Constructor invoking the parent constructor with a fixed error message
     */
    public ClassFileLoaderNotProvidedException() {
        super(PreprocessConstants.MESSAGE_PREFIX + "package loader not provided");
    }
}
