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

package software.amazon.disco.agent;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.disco.agent.interception.Installable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AWSSupportTests {
    @Test
    public void testSqlSupport() {
        Collection<Installable> pkg = new AWSSupport().get();
        Set<Installable> installables = new HashSet<>();
        installables.addAll(pkg);
        Assert.assertEquals(2, installables.size());
    }
}
