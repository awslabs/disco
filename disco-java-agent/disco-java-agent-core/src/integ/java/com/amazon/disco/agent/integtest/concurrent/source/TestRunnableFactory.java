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

package com.amazon.disco.agent.integtest.concurrent.source;

import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class TestRunnableFactory {
    static int numFactoryMethods;

    static {
        numFactoryMethods = 0;
        for (Method method: TestRunnableFactory.class.getDeclaredMethods()) {
            if (method.getReturnType().equals(TestableRunnable.class) && method.getName().startsWith("create")) {
                numFactoryMethods++;
            }
        }

        Assert.assertEquals("Incorrect number of factory methods found in static initializer", Data.provideAllRunnables().size(), numFactoryMethods);
    }

    public interface TestableRunnable extends TestableConcurrencyObject {
        Runnable getRunnable();
    }

    public static class NonThrowingTestableRunnable extends TestableConcurrencyObjectImpl implements TestableRunnable {
        public Runnable runnable;

        @Override
        public Runnable getRunnable() {
            return runnable;
        }

        @Override
        public void perform() {
            try {
                super.perform();
            } catch (Throwable t) {
                thrown = t;
            }
        }
    }

    public static class ThrowingTestableRunnable extends TestableConcurrencyObjectImpl implements TestableRunnable {
        public Runnable runnable;
        public final Error errorToThrow;
        public final RuntimeException rteToThrow;

        public ThrowingTestableRunnable(Error toThrow) {
            this.errorToThrow = toThrow;
            this.rteToThrow = null;
        }

        public ThrowingTestableRunnable(RuntimeException toThrow) {
            this.errorToThrow = null;
            this.rteToThrow = toThrow;
        }

        @Override
        public Runnable getRunnable() {
            return runnable;
        }

        @Override
        public void perform() {
            super.perform();
            if (errorToThrow != null) {
                throw errorToThrow;
            }
            if (rteToThrow != null) {
                throw rteToThrow;
            }
        }
    }

    public static TestableRunnable createConcreteRunnable() {
        NonThrowingTestableRunnable testableRunnable = new NonThrowingTestableRunnable();
        class ConcreteRunnable implements Runnable {
            @Override
            public void run() {
                testableRunnable.perform();
            }
        }
        Runnable concrete = new ConcreteRunnable();
        testableRunnable.runnable = concrete;
        return testableRunnable;
    }

    public static TestableRunnable createAnonymousRunnable() {
        NonThrowingTestableRunnable testableRunnable = new NonThrowingTestableRunnable();
        Runnable anonymous = new Runnable() {
            @Override
            public void run() {
                testableRunnable.perform();
            }
        };
        testableRunnable.runnable = anonymous;
        return testableRunnable;
    }

    public static TestableRunnable createInheritedRunnableWithConcreteBase() {
        NonThrowingTestableRunnable testableRunnable = new NonThrowingTestableRunnable();
        class BaseRunnable implements Runnable {
            @Override
            public void run() {
                testableRunnable.perform();
            }
        }

        class InheritedRunnable extends BaseRunnable implements Runnable {
            @Override
            public void run() {
                testableRunnable.perform();
            }
        }

        InheritedRunnable inherited = new InheritedRunnable();
        testableRunnable.runnable = inherited;
        return testableRunnable;
    }

    public static TestableRunnable createInheritedRunnableWithAbstractBase() {
        NonThrowingTestableRunnable testableRunnable = new NonThrowingTestableRunnable();
        abstract class BaseRunnable implements Runnable {
            @Override
            public void run() {
                testableRunnable.perform();
            }
        }

        class InheritedRunnable extends BaseRunnable implements Runnable {
        }

        InheritedRunnable inherited = new InheritedRunnable();
        testableRunnable.runnable= inherited;
        return testableRunnable;
    }

    public static TestableRunnable createLambdaRunnable() {
        NonThrowingTestableRunnable testableRunnable = new NonThrowingTestableRunnable();
        testableRunnable.runnable = ()-> {
            testableRunnable.perform();
        };
        return testableRunnable;
    }

    public static TestableRunnable createNestedLambdaRunnable() {
        NonThrowingTestableRunnable testableRunnable = new NonThrowingTestableRunnable();
        Runnable innerLambda = ()-> {
            testableRunnable.perform();
        };

        testableRunnable.runnable = ()-> innerLambda.run();
        return testableRunnable;
    }

    public static class Data {
        public static List<Object[]> provideAllRunnables() {
            return Arrays.asList(new Object[][] {
                    {"Concrete", createConcreteRunnable()},
                    {"Anonymous", createAnonymousRunnable()},
                    {"InheritedFromConcrete", createInheritedRunnableWithConcreteBase()},
                    {"InheritedFromAbstract", createInheritedRunnableWithAbstractBase()},
                    {"Lambda", createLambdaRunnable()},
                    {"NestedLambda", createNestedLambdaRunnable()}
            });
        }
    }
}
