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

package software.amazon.disco.instrumentation.preprocess;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.export.JarExportStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationArtifact;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;
import software.amazon.disco.instrumentation.preprocess.multipreprocessor.MultiPreprocessorScheduler;
import software.amazon.disco.instrumentation.preprocess.pojo.PreprocessorOutcome;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class that holds mock entities for testing purposes
 */
public class MockEntities {
    public static List<String> makeClassPaths(){
        return Arrays.asList("com/test/folder/ClassA", "com/test/folder/ClassB", "com/test/folder/ClassC");
    }

    public static List<JarEntry> makeMockJarEntriesWithPath() {
        final List<JarEntry> list = makeMockJarEntries();

        list.add(new JarEntry("ClassD.class"));
        list.add(new JarEntry("ClassE.class"));
        list.add(new JarEntry("ClassF.class"));
        list.add(new JarEntry("pathA/"));

        return list;
    }

    public static List<JarEntry> makeMockJarEntries() {
        final List<JarEntry> list = new ArrayList<>();

        list.add(new JarEntry("ClassA.class"));
        list.add(new JarEntry("ClassB.class"));
        list.add(new JarEntry("ClassC.class"));

        return list;
    }

    public static JarFile makeMockJarFile(){
        JarFile file = Mockito.mock(JarFile.class);

        Enumeration<JarEntry> e = Collections.enumeration(makeMockJarEntriesWithPath());
        Mockito.when(file.entries()).thenReturn(e);

        return file;
    }

    public static Map<String, InstrumentationArtifact> makeInstrumentedClassesMap() {
        final Map<String, InstrumentationArtifact> classes = new HashMap<>();
        final InstrumentationArtifact stateOne = new InstrumentationArtifact("installable_a", new byte[]{12});

        stateOne.update("installable_b", new byte[]{15});
        classes.put("ClassD", stateOne);
        classes.put("ClassE", new InstrumentationArtifact("installable_b", new byte[]{13}));
        classes.put("ClassF", new InstrumentationArtifact("installable_c", new byte[]{14}));

        return classes;
    }

    public static List<String> makeMockPathsWithDuplicates() {
        return Arrays.asList("path_a", "path_a", "path_b", "path_c");
    }

    public static DynamicType makeMockDynamicTypeWithAuxiliaryClasses(){
        final DynamicType type = Mockito.mock(DynamicType.class);

        final TypeDescription auxiliary_1 = Mockito.mock(TypeDescription.class);
        final TypeDescription auxiliary_2 = Mockito.mock(TypeDescription.class);

        Map<TypeDescription, byte[]> auxiliaryTypesMap = new HashMap<>();
        Mockito.doReturn(auxiliaryTypesMap).when(type).getAuxiliaryTypes();
        Mockito.doReturn(new byte[]{02}).when(type).getBytes();
        Mockito.doReturn("internal_1$auxiliary123").when(auxiliary_1).getInternalName();
        Mockito.doReturn("internal_2$auxiliary123").when(auxiliary_2).getInternalName();

        auxiliaryTypesMap.put(auxiliary_1, new byte[]{00});
        auxiliaryTypesMap.put(auxiliary_2, new byte[]{01});

        return type;
    }

    public static DynamicType makeMockDynamicType(){
        final DynamicType type = Mockito.mock(DynamicType.class);

        Map<TypeDescription, byte[]> auxiliaryTypesMap = new HashMap<>();
        Mockito.doReturn(auxiliaryTypesMap).when(type).getAuxiliaryTypes();
        Mockito.doReturn(new byte[]{9}).when(type).getBytes();

        return type;
    }

    public static SourceInfo makeMockJarInfo(){
        final SourceInfo info = Mockito.mock(SourceInfo.class);

        final File mockFile = Mockito.mock(File.class);
        final ExportStrategy mockStrategy = Mockito.mock(JarExportStrategy.class);

        Mockito.lenient().when(info.getSourceFile()).thenReturn(mockFile);
        Mockito.lenient().when(info.getExportStrategy()).thenReturn(mockStrategy);
        Mockito.lenient().when(mockFile.getName()).thenReturn("mock.jar");
        Mockito.lenient().when(info.getClassByteCodeMap()).thenReturn(Collections.singletonMap("SomeClass", "SomeClass".getBytes()));

        return info;
    }

    public static MultiPreprocessorScheduler.PreprocessorInvoker mockPreprocessorInvoker(int exitCode, String processOutput) throws InterruptedException, IOException {
        MultiPreprocessorScheduler.PreprocessorInvoker preprocessorInvoker = Mockito.mock(MultiPreprocessorScheduler.PreprocessorInvoker.class);
        PreprocessorOutcome preprocessorOutcome = PreprocessorOutcome.builder().exitCode(exitCode).preprocessorOutput(processOutput).commandlineArgs(new String[]{"command-line", "arguments"}).build();

        Mockito.doReturn(preprocessorOutcome).when(preprocessorInvoker).call();
        return preprocessorInvoker;
    }
}