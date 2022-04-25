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

package software.amazon.disco.instrumentation.preprocess;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestAdviceToInterceptStrategyTarget;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestAdviceToVisitStrategyTarget;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestInvocable;
import software.amazon.disco.instrumentation.preprocess.source.IntegTestListener;


/**
 * Instrument the target classes {@link IntegTestAdviceToVisitStrategyTarget} and {@link IntegTestAdviceToInterceptStrategyTarget} for testing AdviceTo with
 * both '.intercept(...)' and '.visit(...)' flavors.
 *
 * @see IntegTestAdviceToInterceptor for the interceptor used.
 */
public class AdviceToTest extends IntegtestBase {
    private static final Class<? extends IntegTestInvocable> adviceVisitTargetType = IntegTestAdviceToVisitStrategyTarget.class;
    private static final Class<? extends IntegTestInvocable> adviceInterceptTargetType = IntegTestAdviceToInterceptStrategyTarget.class;

    private static IntegTestListener listener;
    private static IntegTestInvocable adviceVisitTarget;
    private static IntegTestInvocable adviceInterceptTarget;

    @BeforeClass
    public static void beforeAll() throws Exception {
        adviceVisitTarget = adviceVisitTargetType.getDeclaredConstructor().newInstance();
        adviceInterceptTarget = adviceInterceptTargetType.getDeclaredConstructor().newInstance();

        listener = new IntegTestListener();
        listener.register();
    }

    @After
    public void after() {
        listener.events.clear();
    }

    @AfterClass
    public static void afterAll() {
        listener.unRegister();
    }

    @Test
    public void testAdviceToVisitStrategy() {
        Object response = adviceVisitTarget.invokeInstrumentedMethod(ARGUMENT);

        verifyInstrumentedMethodReturnObject(response, adviceVisitTargetType.getName(), listener, true);
    }

    @Test
    public void testAdviceToInterceptStrategy() {
        Object response = adviceInterceptTarget.invokeInstrumentedMethod(ARGUMENT);

        verifyInstrumentedMethodReturnObject(response, adviceInterceptTargetType.getName(), listener, true);
    }
}
