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

package software.amazon.disco.instrumentation.preprocess.loaders.agents;

import lombok.Getter;
import software.amazon.disco.agent.inject.Injector;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * A No-op implementation of the {@link Instrumentation} interface that extracts all {@link ClassFileTransformer transformers} installed
 * onto the instance and appends the agent jar to the bootstrap class path.
 */
public class TransformerExtractor implements Instrumentation {
    @Getter
    private static List<ClassFileTransformer> transformers = new ArrayList<>();

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        transformers.add(transformer);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        return false;
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return true;
    }

    @Override
    public void retransformClasses(Class<?>... classes) { }

    @Override
    public boolean isRedefineClassesSupported() {
        return true;
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) { }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return false;
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return new Class[0];
    }

    @Override
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return new Class[0];
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return 0;
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        Injector.createInstrumentation().appendToBootstrapClassLoaderSearch(jarfile);
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) { }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return false;
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) { }
}
