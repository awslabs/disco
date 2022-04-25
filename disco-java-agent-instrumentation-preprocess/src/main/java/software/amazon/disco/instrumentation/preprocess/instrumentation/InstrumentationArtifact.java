/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.instrumentation;

import lombok.Getter;
import software.amazon.disco.agent.interception.Installable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that encapsulates data of an instrumentation artifact
 */
@Getter
public class InstrumentationArtifact {
    private final Set<String> installableIds;
    private byte[] classBytes;

    /**
     * Object is created at the first instance when a class is transformed.
     *
     * @param installableId id of the {@link Installable installables} applied
     * @param classBytes    byte[] of the transformed class
     */
    public InstrumentationArtifact(final String installableId, final byte[] classBytes) {
        this.classBytes = classBytes;
        this.installableIds = new HashSet<>(Arrays.asList(installableId));
    }

    /**
     * Creates a InstrumentationArtifact instance that represents a dependency class injected by the
     * {@link software.amazon.disco.agent.plugin.ResourcesClassInjector}.
     *
     * @param classBytes byte[] of the artifact class
     */
    public InstrumentationArtifact(byte[] classBytes) {
        this(null, classBytes);
    }

    /**
     * Updates the byte[] of an already transformed class as well as the set of installable ids.
     *
     * @param installableId id of the {@link Installable installables} applied
     * @param classBytes    byte[] of the transformed class
     */
    public void update(final String installableId, final byte[] classBytes) {
        this.classBytes = classBytes;
        this.installableIds.add(installableId);
    }
}