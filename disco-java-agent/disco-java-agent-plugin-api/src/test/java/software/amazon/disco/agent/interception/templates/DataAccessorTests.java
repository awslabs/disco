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

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatcher;
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

import static net.bytebuddy.matcher.ElementMatchers.*;

public class DataAccessorTests {
    private static final String path = "getFoo()/getBar()/getBaz()";
    private static final String pathWithArgs = "getFoo(2)/getBar(1)/getBaz(0)";

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
    public void testProduceCallChainNoArgs() {
        Deque<String> callChain = DataAccessor.produceCallChain(path);
        Assert.assertEquals(3, callChain.size());
        Assert.assertEquals("getBaz()", callChain.pop());
        Assert.assertEquals("getBar()", callChain.pop());
        Assert.assertEquals("getFoo()", callChain.pop());
    }

    @Test
    public void testProduceCallChainWithArgs() {
        Deque<String> callChain = DataAccessor.produceCallChain(pathWithArgs);
        Assert.assertEquals(3, callChain.size());
        Assert.assertEquals("getBaz(0)", callChain.pop());
        Assert.assertEquals("getBar(1)", callChain.pop());
        Assert.assertEquals("getFoo(2)", callChain.pop());
    }

    @Test
    public void testProduceNextMethodCallNoArgs() {
        MethodCall methodCall = DataAccessor.produceNextMethodCall(null, null, "getFoo()");
        MethodCall shouldBe = MethodCall.invoke(named("getFoo").and(takesArguments(0)));
        Assert.assertEquals(shouldBe, methodCall);
    }

    @Test
    public void testProduceNextMethodCallWithArgs() throws Exception {
        MethodCall methodCall = DataAccessor.produceNextMethodCall(null, this.getClass().getDeclaredMethod("simpleAccessor", int.class), "getFoo(0)");
        MethodCall shouldBe = MethodCall.invoke(named("getFoo").and(takesArguments(1)).and(takesArgument(0, int.class))).withArgument(0);
        Assert.assertEquals(shouldBe, methodCall);
    }

    @Test
    public void testChainMethodCallNoArgs() {
        Deque<String> callChain = DataAccessor.produceCallChain(path);
        MethodCall methodCall = DataAccessor.chainMethodCall(null, callChain);
        MethodCall shouldBe = MethodCall.invoke(
                named("getBaz").and(takesArguments(0)))
                .onMethodCall(MethodCall.invoke(named("getBar").and(takesArguments(0)))
                .onMethodCall(MethodCall.invoke(named("getFoo").and(takesArguments(0)))));
        Assert.assertEquals(shouldBe, methodCall);
    }

    @Test
    public void testChainMethodCallWithArgs() throws Exception {
        Deque<String> callChain = DataAccessor.produceCallChain(pathWithArgs);
        MethodCall methodCall = DataAccessor.chainMethodCall(this.getClass().getDeclaredMethod("chainedAccessor", int.class, int.class, int.class), callChain);

        MethodCall shouldBe = MethodCall.invoke(
                named("getBaz").and(takesArguments(1)).and(takesArgument(0, int.class)))
                .onMethodCall(MethodCall.invoke(named("getBar").and(takesArguments(1)).and(takesArgument(0, int.class)))
                .onMethodCall(MethodCall.invoke(named("getFoo").and(takesArguments(1)).and(takesArgument(0, int.class)))
                        .withArgument(2)).withArgument(1)).withArgument(0);

        Assert.assertEquals(shouldBe, methodCall);
    }

    @Test
    public void testMethodMatcherUniquelyMatches() throws Exception {
        testUniqueness();
        testUniqueness(int.class);
        testUniqueness(Object.class);
        testUniqueness(String.class);
    }

    //helper for matcher uniqueness test
    private void testUniqueness(Class<?>... parameterType) throws Exception {
        Method method = MatcherTester.class.getDeclaredMethod("foo", parameterType);
        int[] params;
        if (parameterType.length == 0) {
            params = new int[0];
        } else {
            params = new int[]{0};
        }
        ElementMatcher<? super MethodDescription> matcher = DataAccessor.createParameterizedAccessMethodMatcher("foo", method , params);
        for (Method m: MatcherTester.class.getDeclaredMethods()) {
            if (m.equals(method)) {
                Assert.assertTrue(matcher.matches(new MethodDescription.ForLoadedMethod(m)));
            } else {
                Assert.assertFalse(matcher.matches(new MethodDescription.ForLoadedMethod(m)));
            }
        }
    }

    //class with many 'nearly matching' methods, to stress the uniqueness of the parameterized method matcher
    interface MatcherTester {
        void foo();
        void foo(int i);
        void foo(Object o);
        void foo(String s);
    }

    //private methods accessed reflectively to pass to produceNextMethodCall()
    private Object simpleAccessor(int i) {
        return null;
    }
    private String chainedAccessor(int i, int j, int k) {
        return null;
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
    public void testAccessorWithParam() {
        ExampleOuterClass example = new ExampleOuterClass(new ExampleDelegatedClass());
        ExampleAccessor accessor = (ExampleAccessor)example;
        ExampleDelegatedClass delegatedClass = (ExampleDelegatedClass)accessor.getDelegateByKey(42);
        Assert.assertEquals("Delegated", delegatedClass.getValue());
        Assert.assertNull(accessor.getDelegateByKey(41));
    }

    @Test
    public void testAccessorWithChainedParams() {
        ExampleOuterClass example = new ExampleOuterClass(new ExampleDelegatedClass());
        ExampleAccessor accessor = (ExampleAccessor)example;
        ExampleDelegatedClass delegatedClass = (ExampleDelegatedClass)accessor.getDelegateByKey(42);
        Assert.assertEquals("Delegated", accessor.getDelegatedValueByKeyAndSecret(42, "secret"));
    }

    @Test
    public void testAccessorNullPointerHandling() {
        ExampleOuterClass example = new ExampleOuterClass(null);
        ExampleAccessor accessor = (ExampleAccessor)example;
        Assert.assertEquals("Outer", accessor.getValue());
        Assert.assertEquals(null, accessor.getDelegatedValue());
        Assert.assertEquals(0, accessor.getDelegatedIntValue());
    }

    @Test
    public void testAccessorNullPointerHandlingWithParams() {
        ExampleOuterClass example = new ExampleOuterClass(null);
        ExampleAccessor accessor = (ExampleAccessor)example;
        Assert.assertEquals(null, accessor.getDelegatedValueByKeyAndSecret(42, "secret"));
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
