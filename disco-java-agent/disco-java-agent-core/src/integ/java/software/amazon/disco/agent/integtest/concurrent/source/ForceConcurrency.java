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

package software.amazon.disco.agent.integtest.concurrent.source;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ForceConcurrency {
    public static class RetryRule implements TestRule {
        @Override
        public Statement apply(Statement base, Description target) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    //seems like a high number of retries, but still fails occasionally, and requires the test suite to be rerun
                    boolean success = false;
                    for (int i = 0; i < 63; i++) {
                        try {
                            base.evaluate();
                            success = true;
                        } catch (ConcurrencyCanBeRetriedException t) {
                            System.out.println("DiSCo(Core-integtests): Retrying test in case of concurrency flakiness, retry "+i);
                        } catch (Throwable t) {
                            throw t;
                        }

                        if (success) {
                            return;
                        }
                    }

                    //last chance
                    base.evaluate();
                }
            };
        }
    }
}
