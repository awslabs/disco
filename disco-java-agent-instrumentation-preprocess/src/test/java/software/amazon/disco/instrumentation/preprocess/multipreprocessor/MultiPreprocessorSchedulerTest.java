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
import static org.junit.Assert.assertNull;
import static software.amazon.disco.instrumentation.preprocess.MockEntities.mockPreprocessorInvoker;
import static software.amazon.disco.instrumentation.preprocess.multipreprocessor.MultiPreprocessorScheduler.UNUSED_PROCESSORS;

public class MultiPreprocessorSchedulerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private PreprocessConfig config;
    private MultiPreprocessorScheduler.PreprocessorInvoker mockPreprocessorInvokerWithNormalTermination;
    private MultiPreprocessorScheduler.PreprocessorInvoker mockPreprocessorInvokerWithAbnormalTermination;
    private List<String[]> preprocessorRawCommandlineArgsList;
    private Runtime mockRuntime;

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
        mockRuntime = Mockito.mock(Runtime.class);
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
    public void testConfigureSubPreprocessors_whenSubPreprocessorsIsNull() {
        MultiPreprocessorScheduler multiPreprocessorScheduler = configureMultiPreprocessorScheduler();
        Mockito.doReturn(4).when(multiPreprocessorScheduler).calculateDefaultSubPreprocessors(Mockito.any(Runtime.class));
        assertNull(config.getSubPreprocessors());

        int subPreprocessors = multiPreprocessorScheduler.configureSubPreprocessors();

        Mockito.verify(multiPreprocessorScheduler).calculateDefaultSubPreprocessors(Mockito.any(Runtime.class));
        assertEquals(4, subPreprocessors);
    }

    @Test
    public void testConfigureSubPreprocessors_whenSubPreprocessorsIsNotNull() {
        config = PreprocessConfig.builder().subPreprocessors("3").build();
        MultiPreprocessorScheduler multiPreprocessorScheduler = configureMultiPreprocessorScheduler();

        int subPreprocessors = multiPreprocessorScheduler.configureSubPreprocessors();

        Mockito.verify(multiPreprocessorScheduler, Mockito.never()).calculateDefaultSubPreprocessors(Mockito.any(Runtime.class));
        assertEquals(3, subPreprocessors);
    }

    @Test
    public void testCalculateDefaultSubPreprocessors_whenAvailableProcessorsIsMoreThanUnusedProcessors() {
        MultiPreprocessorScheduler multiPreprocessorScheduler = configureMultiPreprocessorScheduler();
        int availableProcessors = UNUSED_PROCESSORS + 4;
        Mockito.doReturn(availableProcessors).when(mockRuntime).availableProcessors();

        int defaultSubPreprocessors = multiPreprocessorScheduler.calculateDefaultSubPreprocessors(mockRuntime);

        assertEquals(4, defaultSubPreprocessors);
    }

    @Test
    public void testCalculateDefaultSubPreprocessors_whenAvailableProcessorsIsNoMoreThanUnusedProcessors() {
        MultiPreprocessorScheduler multiPreprocessorScheduler = configureMultiPreprocessorScheduler();
        Mockito.doReturn(1).when(mockRuntime).availableProcessors();

        int defaultSubPreprocessors = multiPreprocessorScheduler.calculateDefaultSubPreprocessors(mockRuntime);

        assertEquals(1, defaultSubPreprocessors);
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