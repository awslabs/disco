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
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;

import java.util.HashMap;
import java.util.Map;

/**
 * This listener collects all the {@link AgentBuilder.Listener#onTransformation(TypeDescription, ClassLoader, JavaModule, boolean, DynamicType) events}
 * and stores the byte[] of the transformed classes inside a map that is to be retrieved by the preprocess tool to perform static instrumentation.
 */
public class TransformationListener implements AgentBuilder.Listener {
    @Getter
    private final static Map<String, InstrumentedClassState> instrumentedTypes = new HashMap<>();
    private final static Logger log = LogManager.getLogger(TransformationListener.class);
    private final String uid;

    /**
     * Constructor that takes in the type of the {@link software.amazon.disco.agent.interception.Installable installable}
     * that this listener is attached to.
     *
     * @param uid unique identifier of the installable
     */
    public TransformationListener(final String uid) {
        this.uid = uid;
    }

    /**
     * {@inheritDoc}
     */
    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This event is intercepted by this listener which extracts the byte[] of the transformed classes and any
     * auxiliary classes created as a result of the instrumentation.
     */
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        collectDataFromEvent(typeDescription, dynamicType);
    }

    /**
     * {@inheritDoc}
     */
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
    }

    /**
     * {@inheritDoc}
     */
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        throw new InstrumentationException("Failed to instrument : " + typeName, throwable);
    }

    /**
     * {@inheritDoc}
     */
    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
    }

    /**
     * Extracts the byte[] of the transformed classes from the onTransformation events. Also updates byte[]
     * of a already transformed class that underwent another transformation.
     *
     * @param typeDescription The type that is being transformed.
     * @param dynamicType     The {@link DynamicType dynamic type} that was created by ByteBuddy.
     */
    protected void collectDataFromEvent(TypeDescription typeDescription, DynamicType dynamicType) {
        if (instrumentedTypes.containsKey(typeDescription.getInternalName())) {
            instrumentedTypes.get(typeDescription.getInternalName()).update(uid, dynamicType.getBytes());
        } else {
            instrumentedTypes.put(typeDescription.getInternalName(), new InstrumentedClassState(uid, dynamicType.getBytes()));
        }

        if (!dynamicType.getAuxiliaryTypes().isEmpty()) {
            for (Map.Entry<TypeDescription, byte[]> auxiliaryEntry : dynamicType.getAuxiliaryTypes().entrySet()) {
                instrumentedTypes.put(auxiliaryEntry.getKey().getInternalName(), new InstrumentedClassState(null, auxiliaryEntry.getValue()));
            }
        }
    }
}
