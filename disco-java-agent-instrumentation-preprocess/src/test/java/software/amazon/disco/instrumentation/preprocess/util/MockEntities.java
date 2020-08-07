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

package software.amazon.disco.instrumentation.preprocess.util;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import org.mockito.Mockito;
import software.amazon.disco.instrumentation.preprocess.export.ModuleExportStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentedClassState;
import software.amazon.disco.instrumentation.preprocess.loaders.modules.ModuleInfo;

import java.io.File;
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

    public static Map<String, InstrumentedClassState> makeInstrumentedClassesMap() {
        final Map<String, InstrumentedClassState> classes = new HashMap<>();
        final InstrumentedClassState stateOne = new InstrumentedClassState("installable_a", new byte[]{12});

        stateOne.update("installable_b", new byte[]{15});
        classes.put("ClassD", stateOne);
        classes.put("ClassE", new InstrumentedClassState("installable_b", new byte[]{13}));
        classes.put("ClassF", new InstrumentedClassState("installable_c", new byte[]{14}));

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

    public static ModuleInfo makeMockModuleInfo(){
        final ModuleInfo info = Mockito.mock(ModuleInfo.class);

        final File mockFile = Mockito.mock(File.class);
        final ModuleExportStrategy mockStrategy = Mockito.mock(ModuleExportStrategy.class);

        Mockito.lenient().when(info.getFile()).thenReturn(mockFile);
        Mockito.lenient().when(info.getExportStrategy()).thenReturn(mockStrategy);
        Mockito.lenient().when(mockFile.getName()).thenReturn("mock.jar");

        return info;
    }
}
