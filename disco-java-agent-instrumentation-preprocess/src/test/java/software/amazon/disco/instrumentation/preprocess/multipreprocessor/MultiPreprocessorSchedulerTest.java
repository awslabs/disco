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

package software.amazon.disco.instrumentation.preprocess.multipreprocessor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ProcessInstrumentationAbortedException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static software.amazon.disco.instrumentation.preprocess.MockEntities.mockPreprocessorInvoker;

public class MultiPreprocessorSchedulerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private PreprocessConfig config;
    private MultiPreprocessorScheduler.PreprocessorInvoker mockPreprocessorInvokerWithNormalTermination;
    private MultiPreprocessorScheduler.PreprocessorInvoker mockPreprocessorInvokerWithAbnormalTermination;
    private List<String[]> preprocessorRawCommandlineArgsList;

    @Before
    public void before() throws IOException, InterruptedException {
        mockPreprocessorInvokerWithNormalTermination = mockPreprocessorInvoker(0, "Process with normal termination");
        mockPreprocessorInvokerWithAbnormalTermination = mockPreprocessorInvoker(1, "Process with abnormal termination");
        config = PreprocessConfig.builder()
                .agentPath("a path")
                .sourcePath("lib", new HashSet<>(Arrays.asList("/d1", "/d2")))
                .failOnUnresolvableDependency(false)
                .build();
        preprocessorRawCommandlineArgsList = Arrays.asList(new String[]{"arg1", "arg2"}, new String[]{"arg1", "arg2"});
    }

    @Test(expected = ProcessInstrumentationAbortedException.class)
    public void testExecutePreprocessorInvokersWithAbnormalTermination() throws Throwable {
        MultiPreprocessorScheduler multiPreprocessorScheduler = configureMultiPreprocessorScheduler();
        List<MultiPreprocessorScheduler.PreprocessorInvoker> preprocessorInvokers = Arrays.asList(mockPreprocessorInvokerWithNormalTermination, mockPreprocessorInvokerWithAbnormalTermination);
        multiPreprocessorScheduler.executePreprocessorInvokers(preprocessorInvokers);
    }

    @Test
    public void testExecutePreprocessorInvokersWithNormalTermination() throws Throwable {
        MultiPreprocessorScheduler multiPreprocessorScheduler = configureMultiPreprocessorScheduler();
        List<MultiPreprocessorScheduler.PreprocessorInvoker> preprocessorInvokers = Collections.singletonList(mockPreprocessorInvokerWithNormalTermination);
        List<String> preprocessorOutputs = multiPreprocessorScheduler.executePreprocessorInvokers(preprocessorInvokers);
        assertEquals(Collections.singletonList("Process with normal termination"), preprocessorOutputs);
    }

    @Test
    public void testGetPreprocessorNumReturnNotSmallerThanOne() {
        MultiPreprocessorScheduler multiPreprocessorScheduler = configureMultiPreprocessorScheduler();
        assertFalse(multiPreprocessorScheduler.getPreprocessorNum() < 1);
    }


    private MultiPreprocessorScheduler configureMultiPreprocessorScheduler() {
        MultiPreprocessorScheduler multiPreprocessorScheduler = Mockito.spy(
                MultiPreprocessorScheduler.builder()
                        .config(config)
                        .build()
        );
        return multiPreprocessorScheduler;
    }
}