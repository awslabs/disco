/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.integtest.interception;

import org.junit.Test;

import java.security.Permission;

public class ClassCircularityErrorEdgeCaseTest {
    @Test
    public void testClassLoaderInnerClassCircularityError() throws Exception {
        System.setSecurityManager(new TestSecurityManager());
        Class.forName("java.lang.ClassLoader$1");
        System.setSecurityManager(null);
    }

    static class TestSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(final Permission p) {
            p.getName();
        }
    }
}
