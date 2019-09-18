/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.interception.Installable;
import com.amazon.disco.agent.interception.Package;

import java.util.Arrays;
import java.util.Collection;

/**
 * Definition of the Concurrency Support package, containing all Installables required.
 */
public class ConcurrencySupport implements Package {
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Installable> get() {
        return Arrays.asList(
                new ExecutorInterceptor(),
                new ForkJoinPoolInterceptor(),
                new ForkJoinTaskInterceptor(),
                new ThreadInterceptor(),
                new ThreadSubclassInterceptor()
        );
    }
}
