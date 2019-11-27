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

package software.amazon.disco.agent.interception;

import java.util.Collection;

/**
 * Abstraction around a collection of Installables, so that products can install a whole package, without
 * needing to know the types of each Installable, which is effectively an implementation detail.
 */
public interface Package {
    /**
     * Return a list/set of this package's Installables
     * @return a list/set of this package's Installables
     */
    Collection<Installable> get();
}
