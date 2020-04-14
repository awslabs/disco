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

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.annotations.DataAccessPath;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Deque;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * A generic template for creating efficient reflection-free accessors onto Objects for interceptors, where no compile-time dependency on the Object's class can
 * safely be taken (which is most or all of the time).
 *
 * This mechanism may make mocking the target objects harder in tests, because the inserted Accessor interface will also have its methods mocked. Test authors
 * may prefer to construct real objects instead of mocks.
 */
public class DataAccessor implements Installable {
    final ElementMatcher<TypeDescription> typeImplementingAccessor;
    final ElementMatcher<TypeDescription> typesImplementingAccessMethods;
    final Class<?> accessor;

    /**
     * Protected constructor for factory access
     * @param typeImplementingAccessor ElementMatcher describing the class to implement the accessor, usually some lowest-common denominator base or interface
     * @param accessor Accessor interface declaring the methods to use for access
     */
    protected DataAccessor(ElementMatcher<TypeDescription> typeImplementingAccessor, ElementMatcher<TypeDescription> typesImplementingAccessMethods, Class<?> accessor) {
        if (!accessor.isInterface()) {
            throw new IllegalArgumentException();
        }

        this.typeImplementingAccessor = typeImplementingAccessor;
        this.typesImplementingAccessMethods = typesImplementingAccessMethods;
        this.accessor = accessor;
    }

    /**
     * Factory method to create a DataAccessor which allows access to corresponding methods of the given exact class
     * @param className the exact className to project data access onto
     * @param accessor the accessor defining the access methods
     * @return a constructed DataAccessor
     */
    public static DataAccessor forClassNamed(String className, Class<?> accessor) {
        ElementMatcher<TypeDescription> typeMatcher = ElementMatchers.named(className);
        return new DataAccessor(typeMatcher, typeMatcher, accessor);
    }

    /**
     * Factory method to create a DataAccessor which allows access to the corresponding methods of any concrete subclasses of the given interface
     * @param interfaceName the name of the interface, for which implementing classes will have access methods defined
     * @param accessor the accessor defining the access methods
     * @return a constructed DataAccessor
     */
    public static DataAccessor forConcreteSubclassesOfInterface(String interfaceName, Class<?> accessor) {
        ElementMatcher<TypeDescription> typeMatcher = ElementMatchers.named(interfaceName).and(ElementMatchers.isInterface());
        return new DataAccessor(
                typeMatcher,
                ElementMatchers.hasSuperType(typeMatcher).and(ElementMatchers.not(ElementMatchers.isAbstract())),
                accessor
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        //first, have the target type(s) implement the accessor
        agentBuilder = agentBuilder.type(typeImplementingAccessor).transform((builder, typeDescription, classLoader, module) -> builder.implement(accessor));

        //now add methods where needed to satisfy DataAccessPath chains
        agentBuilder = agentBuilder.type(typesImplementingAccessMethods).transform((builder, typeDescription, classLoader, module) -> {
                for (Method method: accessor.getDeclaredMethods()) {
                    builder = processAccessorMethod(builder, method);
                }

                return builder;
            }

        );

        return agentBuilder;
    }

    /**
     * Instrument the target type(s) as necessary to acquire the data access semantics of the access method
     * @param builder the current DynamicType Builder instance for building interception rules
     * @param method the method being implemented for the accessor
     */
    static DynamicType.Builder<?> processAccessorMethod(DynamicType.Builder<?> builder, Method method) {
        //inspect any annotations it might have, otherwise it is a 'simple' form (i.e. primitive types only, perfectly matching the target method in name and signature)
        //if annotations present, synthesize the access method
        if (method.isAnnotationPresent(DataAccessPath.class)) {
            DataAccessPath dap = method.getAnnotation(DataAccessPath.class);
            builder = builder
                .define(method)
                    .intercept(Advice.to(ExceptionSafety.class)
                    .wrap(chainMethodCall(produceCallChain(dap.value()))))
                ;

            //the method must not collide with a method already present in the target type, or its superclasses/interfaces
            //we should check for this. For now, it's the responsibility of the user, undefined behavior etc.
        }

        return builder;
    }

    /**
     * Produce an ordered collection, in this case a Deque, representing the chain of calls expressed by a DataAccessPath annotation
     * @param path the 'path' of chained method calls expressed in a DataAccessPath annotation, of a form like "getFoo()/getBar()"
     * @return a Deque of the constituent parts of the path
     */
    static Deque<String> produceCallChain(String path) {
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        Deque<String> callChain = new LinkedList<>();
        while (tokenizer.hasMoreTokens()) {
            String call = tokenizer.nextToken();
            callChain.push(call);
        }
        return callChain;
    }

    /**
     * From a collection of method call instructions e.g. ["getFoo()", "getBar()"], produce a MethodCall implementation joining them
     * representing, in pseudo-Java, 'return getFoo().getBar();'
     * @param callChain the Deque of call chain parts
     * @return a MethodCall implementation representing all the calls chained together
     */
    static MethodCall chainMethodCall(Deque<String> callChain) {
        //TODO handle param passing.
        /* will look something like:
        StringTokenizer params = new StringTokenizer(call, "(,)");
        while (params.hasMoreTokens()) {
            String param = params.nextToken();
            int paramIndex = Integer.parseInt(param);
            Type paramType = method.getParameters()[paramIndex].getType();

            //produce the next method call with a 'withArgument()' call on the supplied argument
        }
        */
        //TODO for now, just assume methods taking no params
        MethodCall next = produceNextMethodCall(callChain);
        while (!callChain.isEmpty()) {
            next = produceNextMethodCall(callChain).onMethodCall(next);
        }

        return next;
    }

    /**
     * Produce the next chained method call in a chain of them, by creating a single MethodCall of the next one, applying it to
     * the accumulating chain so far
     * @param callChain the remaining call chain to be processed
     * @return the next chained method call of the complete chain
     */
    static MethodCall.WithoutSpecifiedTarget produceNextMethodCall(Deque<String> callChain) {
        String s = callChain.removeLast();
        String call = s.substring(0, s.indexOf('('));
        ElementMatcher<MethodDescription> methodDescriptionElementMatcher = ElementMatchers.named(call);

        return MethodCall.invoke(methodDescriptionElementMatcher);
    }

    /**
     * An Advice class to wrap generated methods in catch-all exception safety. Should any exception occur, such as a NullPointerException
     * from unchecked method chaining, it will be silently suppressed, and the return value of the access method will be the default
     * value for the return type i.e. 0 for primitive numbers, and null for reference types.
     */
    public static class ExceptionSafety {
        /**
         * Advice on method exit, to rewrite any thrown exception to be null, and therefore suppressed
         * @param thrown the exception which was thrown, which will be suppressed
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onMethodExit(@Advice.Thrown(readOnly = false) Throwable thrown) {
            thrown = null;
        }
    }
}
