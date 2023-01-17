/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.integtest.concurrent.source;

import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class TestCallableFactory {
    static int numFactoryMethods;

    static {
        numFactoryMethods = 0;
        for (Method method: TestCallableFactory.class.getDeclaredMethods()) {
            if (method.getReturnType().equals(NonThrowingTestableCallable.class) && method.getName().startsWith("create")) {
                numFactoryMethods++;
            }
        }

        Assert.assertEquals("Incorrect number of factory methods found in static initializer", Data.provideAllCallables().size(), numFactoryMethods);
    }

    public static class NonThrowingTestableCallable extends TestableConcurrencyObjectImpl {
        public static final String result = "Result";
        public Callable<String> callable;

        @Override
        public void perform() {
            try {
                super.perform();
            } catch (Throwable t) {
                thrown = t;
            }
        }
    }

    public static NonThrowingTestableCallable createConcreteCallable() {
        NonThrowingTestableCallable testableCallable = new NonThrowingTestableCallable();
        class ConcreteCallable implements Callable<String> {
            @Override
            public String call() {
                testableCallable.perform();
                return NonThrowingTestableCallable.result;
            }
        }
        ConcreteCallable concrete = new ConcreteCallable();
        testableCallable.callable = concrete;
        return testableCallable;
    }

    public static NonThrowingTestableCallable createAnonymousCallable() {
        NonThrowingTestableCallable testableCallable = new NonThrowingTestableCallable();
        Callable anonymous = new Callable() {
            @Override
            public Object call() throws Exception {
                testableCallable.perform();
                return NonThrowingTestableCallable.result;
            }
        };
        testableCallable.callable = anonymous;
        return testableCallable;
    }

    public static NonThrowingTestableCallable createInheritedCallableWithConcreteBase() {
        NonThrowingTestableCallable testableCallable = new NonThrowingTestableCallable();
        class BaseCallable implements Callable<String> {
            @Override
            public String call() {
                testableCallable.perform();
                return NonThrowingTestableCallable.result;
            }
        }

        class InheritedCallable extends BaseCallable implements Callable<String> {
            @Override
            public String call() {
                testableCallable.perform();
                return NonThrowingTestableCallable.result;
            }
        }

        InheritedCallable inherited = new InheritedCallable();
        testableCallable.callable = inherited;
        return testableCallable;
    }

    public static NonThrowingTestableCallable createInheritedCallableWithAbstractBase() {
        NonThrowingTestableCallable testableCallable = new NonThrowingTestableCallable();
        abstract class BaseCallable implements Callable<String> {
            @Override
            public String call() {
                testableCallable.perform();
                return NonThrowingTestableCallable.result;
            }
        }

        class InheritedCallable extends BaseCallable implements Callable<String> {
        }

        InheritedCallable inherited = new InheritedCallable();
        testableCallable.callable = inherited;
        return testableCallable;
    }

    public static NonThrowingTestableCallable createLambdaCallable() {
        NonThrowingTestableCallable testableCallable = new NonThrowingTestableCallable();
        testableCallable.callable = ()-> {
            testableCallable.perform();
            return NonThrowingTestableCallable.result;
        };
        return testableCallable;
    }

    public static NonThrowingTestableCallable createNestedLambdaCallable() {
        NonThrowingTestableCallable testableCallable = new NonThrowingTestableCallable();
        Callable<String> innerLambda = ()-> {
            testableCallable.perform();
            return NonThrowingTestableCallable.result;
        };

        testableCallable.callable = ()-> innerLambda.call();
        return testableCallable;
    }

    public static class Data {
        public static List<Object[]> provideAllCallables() {
            return Arrays.asList(new Object[][]{
                    {"Concrete", createConcreteCallable()},
                    {"Anonymous", createAnonymousCallable()},
                    {"InheritedFromConcrete", createInheritedCallableWithConcreteBase()},
                    {"InheritedFromAbstract", createInheritedCallableWithAbstractBase()},
                    {"Lambda", createLambdaCallable()},
                    {"NestedLambda", createNestedLambdaCallable()}
            });
        }
    }
}
