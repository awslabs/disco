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

package software.amazon.disco.application.example;

import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class InjectorTest {
    //comment out this annotation to see the test fail, since the agent will not have been loaded.
    @BeforeClass
    public static void beforeClass() throws Exception {
        final String jarPath = "../disco-java-agent-example/build/libs/disco-java-agent-example-0.1.jar";
        Injector.loadAgent(jarPath, "extraverbose");
    }

    /**
     * Just a small test to create a Thread, assert that some context survives the hand off into the child thread
     */
    @Test
    public void testAgentThreadInterception() throws Exception {
        TransactionContext.clear();
        TransactionContext.set("tx_value");
        AtomicBoolean correctValue = new AtomicBoolean();
        Thread t = new Thread(()->{
            correctValue.set(TransactionContext.get().equals("tx_value"));
        });
        t.start();
        t.join();
        Assert.assertTrue(correctValue.get());
    }
}
