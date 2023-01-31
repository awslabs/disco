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

import lombok.Builder;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.ProcessInstrumentationAbortedException;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Class responsible to partition sources to preprocess and distribute partitions to preprocessor(s) to let them work in parallel.
 * In order to reduce preprocessing time overhead, preprocessor(s) will be invoked to execute instrumentation in parallel.
 * By starting multiple processes with command and arguments to invoke {@link PreprocessorDriver} could start multiple worker programs(preprocessors),
 * and these preprocessors will work independently.
 */
@Builder
public class MultiPreprocessorScheduler {
    private static final Logger log = LogManager.getLogger(MultiPreprocessorScheduler.class);
    private final PreprocessConfig config;
    static final int UNUSED_PROCESSORS = 2;

    /**
     * Callable class that is responsible for invoking a preprocessor by starting a new process,
     * waiting for process to be terminated and return {@link PreprocessorOutcome} from the process.
     */
    public static class PreprocessorInvoker implements Callable<PreprocessorOutcome> {
        private final String preprocessorCommandlineArgs;

        private PreprocessorInvoker(String preprocessorCommandlineArgs) {
            this.preprocessorCommandlineArgs = preprocessorCommandlineArgs;
        }

        @Override
        public PreprocessorOutcome call() throws IOException, InterruptedException {
            ProcessBuilder processBuilder = new ProcessBuilder();
            final File javaHomeDir = new File(System.getProperty("java.home"));
            final File javaDir = javaHomeDir.getAbsolutePath().endsWith("jre") ? new File(javaHomeDir.getParentFile(), "bin") : new File(javaHomeDir, "bin");
            List<String> commandList = new ArrayList<>(Arrays.asList(javaDir + "/java", "-cp", System.getProperty("java.class.path"), "software.amazon.disco.instrumentation.preprocess.multipreprocessor.PreprocessorDriver"));
            commandList.add(preprocessorCommandlineArgs);
            processBuilder.command(commandList);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            final String processOutput = readInputStream(process.getInputStream());
            final int exitCode = process.waitFor();
            return PreprocessorOutcome.builder()
                    .exitCode(exitCode)
                    .preprocessorOutput(processOutput)
                    .commandlineArgs(commandList.toArray(new String[0]))
                    .build();
        }
    }

    /**
     * Partition sources paths, assign partitions to preprocessors, invoke preprocessors to work on its portion of preprocessing work in parallel
     * print the outputs from preprocessors and summary of the whole preprocessing
     */
    public void execute() throws ExecutionException, InterruptedException {
        // number of sub-preprocessors
        int subPreprocessors = configureSubPreprocessors();
        // list of raw command line arguments for sub-preprocessors
        List<String[]> preprocessorRawCommandlineArgsList = ConfigPartitioner.partitionConfig(config, subPreprocessors).stream().map(PreprocessConfig::toCommandlineArguments).collect(Collectors.toList());
        // store sub-preprocessors raw command-line arguments as txt file and get the file path as sub-preprocessor's command-line arguments
        List<String> preprocessorCommandlineArgsList = new PreprocessorArgumentsExporter().exportArguments(preprocessorRawCommandlineArgsList, config, PreprocessConstants.PREPROCESSOR_ARGS_TEMP_FOLDER);
        // create preprocessor invokers
        List<PreprocessorInvoker> preprocessorInvokers = preprocessorCommandlineArgsList.stream().map(PreprocessorInvoker::new).collect(Collectors.toList());
        log.info("Arranged " + preprocessorInvokers.size() + " workers to preprocess sources in parallel, this may take a few minutes to complete...");
        // execute preprocessor invokers
        List<String> preprocessorOutputs = executePreprocessorInvokers(preprocessorInvokers);
        // print output from preprocessors and summary of the whole preprocessing
        new PreprocessOutputHandler(preprocessorOutputs).printPreprocessOutput();
    }

    /**
     * Invoke preprocessors, wait for all preprocessors to finish work and collect the outputs from preprocessors.
     * If any preprocessor fails, still wait for others to complete.
     *
     * @param preprocessorInvokers a list of {@link PreprocessorInvoker}
     * @return a list of output from preprocessors which is standard output combined with error output
     * @throws InterruptedException if interrupted while waiting for tasks to complete
     * @throws ExecutionException   if task result computation threw an exception
     */
    protected List<String> executePreprocessorInvokers(List<PreprocessorInvoker> preprocessorInvokers) throws InterruptedException, ExecutionException {
        ExecutorService taskExecutor = Executors.newCachedThreadPool();
        List<String> preprocessorOutputs = new ArrayList<>();

        List<Future<PreprocessorOutcome>> results = taskExecutor.invokeAll(preprocessorInvokers);

        for (Future<PreprocessorOutcome> res : results) {
            PreprocessorOutcome preprocessorOutCome = res.get();
            int exitCode = preprocessorOutCome.getExitCode();
            String processorOutput = preprocessorOutCome.getPreprocessorOutput();
            if (exitCode != 0) {
                throw new ProcessInstrumentationAbortedException(exitCode, preprocessorOutCome.getCommandlineArgs(), processorOutput);
            }
            preprocessorOutputs.add(processorOutput);
        }

        // shutdown thread pool
        taskExecutor.shutdown();
        return preprocessorOutputs;
    }

    /**
     * Configure number of sub-preprocessors based on sub-preprocessors configuration supplied by {@link PreprocessConfig} file.
     *
     * @return number sub-preprocessors to be used in parallel preprocessing.
     */
    protected int configureSubPreprocessors() {
        String subPreprocessors = config.getSubPreprocessors();

        if(subPreprocessors == null) {
            return calculateDefaultSubPreprocessors(Runtime.getRuntime());
        }

        return Integer.parseInt(subPreprocessors);
    }

    /**
     * Calculate number of sub-preprocessors to be used by default strategy.
     * Currently, use maximum number of processors of VM minus number of processor that intended to be idle to prevent from using full power.
     *
     * @param runtime the current runtime.
     * @return the number of sub-preprocessors calculated by default strategy.
     */
    protected int calculateDefaultSubPreprocessors(Runtime runtime) {
        int availableProcessors = runtime.availableProcessors();
        return availableProcessors > UNUSED_PROCESSORS ? availableProcessors - UNUSED_PROCESSORS : availableProcessors;
    }

    /**
     * Read input stream connected to the standard output of the process.
     *
     * @param inputStream input stream to be read
     * @return output of the process
     */
    protected static String readInputStream(final InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining(System.lineSeparator()));
    }
}
