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

import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Partition config for preprocessing into configs supplied to sub-preprocessors
 */
public class ConfigPartitioner {
    /**
     * Partition one {@link PreprocessConfig} for preprocessing into a list of PreprocessConfig objects of length partitionNum for sub-preprocessors,
     * where the preprocessing work are divided roughly equally among PreprocessConfig objects for sub-preprocessors.
     *
     * @param config       a PreprocessConfig containing information to perform preprocessing
     * @param partitionNum number of preprocessors work in parallel
     * @return a list of PreprocessConfig objects
     */
    public static List<PreprocessConfig> partitionConfig(PreprocessConfig config, int partitionNum) {
        List<PreprocessConfig> configs = new ArrayList<>();
        Map<String, Set<String>> sourcePaths = config.getSourcePaths();
        List<Map<String, Set<String>>> sourcePathsPartitions = partitionSourcePaths(sourcePaths, partitionNum);
        for (Map<String, Set<String>> sourcePathsPartition : sourcePathsPartitions) {
            configs.add(generatePreprocessorConfig(config, sourcePathsPartition, sourcePathsPartitions.indexOf(sourcePathsPartition)));
        }
        return configs;
    }

    /**
     * Divides a source paths map into partitions, each partition is a map has the same keys with the original source paths,
     * value to a key is a partition of sources for the same key in original source paths.
     *
     * @param sourcePaths  a set of sources to be processed indexed by their corresponding relative path
     * @param partitionNum number of partitions
     * @return a list of source paths partitions. For each partition, the key is the relative path and value is sources partition.
     * For example, partitioning source paths {lib1=[/d1, /d2, /d3, /d4, /d5], lib2=[/d11, /d22, /d33]} into 2 partitions
     * yields [{lib1=[/d1, /d2, /d3], lib2=[d11, /d22]}, {lib1=[/d4, /d5], lib2=[/d33]}]
     */
    protected static List<Map<String, Set<String>>> partitionSourcePaths(Map<String, Set<String>> sourcePaths, int partitionNum) {
        List<Map<String, Set<String>>> sourcePathsMaps = new ArrayList<>();
        Map<String, List<?>> partitionedSourcePaths = new HashMap<>();
        for (final Map.Entry<String, Set<String>> entry : sourcePaths.entrySet()) {
            List<List<String>> sourcesPartitions = partitionSources(entry.getValue(), partitionNum);
            partitionedSourcePaths.put(entry.getKey(), sourcesPartitions);
        }

        for (int i = 0; i < partitionNum; i++) {
            Map<String, Set<String>> sourcePathsPartition = new HashMap<>();
            for (final Map.Entry<String, List<?>> entry : partitionedSourcePaths.entrySet()) {
                if (i < entry.getValue().size()) {
                    sourcePathsPartition.put(entry.getKey(), new HashSet<>((List<String>) entry.getValue().get(i)));
                }
            }
            if (sourcePathsPartition.isEmpty()) break;
            sourcePathsMaps.add(sourcePathsPartition);
        }
        return sourcePathsMaps;
    }

    /**
     * Divides a set of sources into given number of sublist.
     *
     * @param sources      set of sources to be partitioned
     * @param partitionNum number of partitions
     * @return a list of sources sublist.
     * For example, partitioning sources containing [/d1, /d2, /d3, /d4, /d5] into 2 partitions yields [[/d1, /d2, /d3], [/d4, /d5]]
     */
    protected static List<List<String>> partitionSources(Set<String> sources, int partitionNum) {
        int partitionSize = (sources.size() + partitionNum - 1) / partitionNum;
        final AtomicInteger counter = new AtomicInteger();
        List<List<String>> sourcesPartitions = new ArrayList<>(sources.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / partitionSize))
                .values());
        return sourcesPartitions;
    }

    /**
     * Generate {@link PreprocessConfig} file for a preprocessor. The PreprocessConfig of the preprocessor will have same
     * values with the original config and with its own source paths partition.
     *
     * @param config      a PreprocessConfig containing information to perform whole preprocessing
     * @param sourcePaths a set of sources to be processed by the preprocessor
     * @param index       the index of the preprocessor
     * @return an instance of PreprocessConfig for the preprocessor
     */
    protected static PreprocessConfig generatePreprocessorConfig(PreprocessConfig config, Map<String, Set<String>> sourcePaths, int index) {
        //create a cloned preprocessing config
        PreprocessConfig clonedConfig = config.toBuilder().build();
        //clear the source paths of the cloned config
        clonedConfig.setSourcePaths(null);
        //create a new builder that starts out with all values of the cloned config
        //all these values are same with preprocessing config except sourcePaths that will be null
        PreprocessConfig.PreprocessConfigBuilder configBuilder = clonedConfig.toBuilder();
        configBuilder.sourcePaths(sourcePaths);
        //if jdk path is not null, only allow first preprocessor to have jdk path set to prevent duplicate jdk instrumentation
        if (config.getJdkPath() != null && index > 0) {
            configBuilder.jdkPath(null);
        }
        return configBuilder.build();
    }
}
