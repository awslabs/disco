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

package software.amazon.disco.agent.interception.templates;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.interception.annotations.DataAccessPath;
import software.amazon.disco.agent.interception.templates.integtest.ExampleAccessor;
import software.amazon.disco.agent.interception.templates.integtest.ExampleAccessorInstaller;
import software.amazon.disco.agent.interception.templates.integtest.source.ExampleDelegatedClass;
import software.amazon.disco.agent.interception.templates.integtest.source.ExampleOuterClass;

import java.lang.reflect.Method;
import java.util.Deque;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class DataAccessorTests {
    private static final String path = "getFoo()/getBar()/getBaz()";

    @BeforeClass
    public static void beforeClass() {
        ExampleAccessorInstaller.init();
    }

    @Test
    public void testForClassNamed() {
        DataAccessor da = DataAccessor.forClassNamed("software.amazon.disco.agent.interception.templates.DataAccessorTests$TestClass", DummyAccessor.class);
        Assert.assertTrue(da.typeImplementingAccessor.matches(new TypeDescription.ForLoadedType(TestClass.class)));
        Assert.assertTrue(da.typesImplementingAccessMethods.matches(new TypeDescription.ForLoadedType(TestClass.class)));
    }

    @Test
    public void testForConcreteSubclassesOfInterface() {
        DataAccessor da = DataAccessor.forConcreteSubclassesOfInterface("software.amazon.disco.agent.interception.templates.DataAccessorTests$TestInterface", DummyAccessor.class);
        Assert.assertTrue(da.typeImplementingAccessor.matches(new TypeDescription.ForLoadedType(TestInterface.class)));
        Assert.assertFalse(da.typesImplementingAccessMethods.matches(new TypeDescription.ForLoadedType(TestBaseClass.class)));
        Assert.assertTrue(da.typesImplementingAccessMethods.matches(new TypeDescription.ForLoadedType(TestDerivedClass.class)));
    }

    @Test
    public void testProcessAccessorMethodWithoutDataPath() throws Exception {
        DynamicType.Builder<?> builder = Mockito.mock(DynamicType.Builder.class);
        DataAccessor.processAccessorMethod(builder, DummyAccessor.class.getDeclaredMethod("simpleMethod"));
        Mockito.verifyNoInteractions(builder);
    }

    @Test
    public void testProcessAccessorMethodWithDataPath() throws Exception {
        DynamicType.Builder<?> builder = Mockito.mock(DynamicType.Builder.class);
        DynamicType.Builder.MethodDefinition.ImplementationDefinition implementationDefinition = Mockito.mock(DynamicType.Builder.MethodDefinition.ImplementationDefinition.class);
        Mockito.when(builder.define(Mockito.any(Method.class))).thenReturn(implementationDefinition);
        DataAccessor.processAccessorMethod(builder, DummyAccessor.class.getDeclaredMethod("pathMethod"));
        Mockito.verify(builder).define(Mockito.any(Method.class));
    }

    @Test
    public void testProduceCallChain() {
        Deque<String> callChain = DataAccessor.produceCallChain(path);
        Assert.assertEquals(3, callChain.size());
        Assert.assertEquals("getBaz()", callChain.pop());
        Assert.assertEquals("getBar()", callChain.pop());
        Assert.assertEquals("getFoo()", callChain.pop());
    }

    @Test
    public void testProduceNextMethodCall() {
        Deque<String> callChain = DataAccessor.produceCallChain(path);
        MethodCall methodCall = DataAccessor.produceNextMethodCall(callChain);
        MethodCall shouldBe = MethodCall.invoke(named("getFoo"));
        Assert.assertEquals(shouldBe, methodCall);
    }

    @Test
    public void testChainMethodCall() {
        Deque<String> callChain = DataAccessor.produceCallChain(path);
        MethodCall methodCall = DataAccessor.chainMethodCall(callChain);
        MethodCall shouldBe = MethodCall.invoke(named("getBaz")).onMethodCall(MethodCall.invoke(named("getBar")).onMethodCall(MethodCall.invoke(named("getFoo"))));
        Assert.assertEquals(shouldBe, methodCall);
    }

    //
    // Integ tests
    //
    @Test
    public void testAccessor() {
        ExampleOuterClass example = new ExampleOuterClass(new ExampleDelegatedClass());
        ExampleAccessor accessor = (ExampleAccessor)example;
        Assert.assertEquals("Outer", accessor.getValue());
        Assert.assertEquals("Delegated", accessor.getDelegatedValue());
        Assert.assertEquals(42, accessor.getDelegatedIntValue());
    }

    @Test
    public void testAccessorNullPointerHandling() {
        ExampleOuterClass example = new ExampleOuterClass(null);
        ExampleAccessor accessor = (ExampleAccessor)example;
        Assert.assertEquals("Outer", accessor.getValue());
        Assert.assertEquals(null, accessor.getDelegatedValue());
        Assert.assertEquals(0, accessor.getDelegatedIntValue());
    }


    interface DummyAccessor {
        void simpleMethod();

        @DataAccessPath("foo()/bar()")
        void pathMethod();
    }

    static class TestClass {}

    interface TestInterface {}
    static abstract class TestBaseClass implements TestInterface {}
    static class TestDerivedClass extends TestBaseClass {}
}
