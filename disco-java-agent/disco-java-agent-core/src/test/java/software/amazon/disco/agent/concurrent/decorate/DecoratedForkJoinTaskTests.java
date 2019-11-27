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

package software.amazon.disco.agent.concurrent.decorate;

import org.junit.Test;

import java.util.concurrent.ForkJoinTask;

/**
 * All tests can do is throw reflection errors, since unit-tests are agent-not-present scenario
 */
public class DecoratedForkJoinTaskTests {

    @Test(expected = NoSuchFieldException.class)
    public void testCreateThrows() throws Exception {
        ForkJoinTask fjt = new TestForkJoinTask();
        DecoratedForkJoinTask.create(fjt);

    }

    @Test(expected = NoSuchFieldException.class)
    public void testGetThrows() throws Exception {
        ForkJoinTask fjt = new TestForkJoinTask();
        DecoratedForkJoinTask.get(fjt);

    }

    @Test(expected = NoSuchFieldException.class)
    public void testLookupThrows() throws Exception {
        DecoratedForkJoinTask.lookup();
    }

    static class TestForkJoinTask extends ForkJoinTask {
        @Override
        public Object getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Object value) {
        }

        @Override
        protected boolean exec() {
            return false;
        }
    }
}
