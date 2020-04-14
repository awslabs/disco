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

package software.amazon.disco.agent.interception.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Methods in a data accessor annotated with DataAccessPath access their data according to that path.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
public @interface DataAccessPath {

    /**
     * A path representing the chain of calls required to extract the data being accessed.
     * For example the value "getFoo()/getBar()" would call getFoo() on the interception target, and then call getBar()
     * on the result of the getFoo() call (without having to know its type). The result produced at the end of this chain
     * of calls, if any, is then provided as the return value of the instrumented method.
     *
     * @return the data path
     */
    String value();
}
