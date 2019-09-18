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

package com.amazon.disco.agent.customize;

import com.amazon.disco.agent.customize.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Everything in Customization basically works the same way - attempting to reflectively invoke methods,
 * usually static ones, that reside within the Agent. Any failures are degraded gracefully to no-ops in the
 * case that the Agent is not loaded, making it safe for clients to use then Customization library in their
 * Prod code.
 *
 * @param <T> - the return type of the method being called
 */
public class ReflectiveCall<T> {
    private static final String ALPHA_ONE_AGENT_PACKAGE_ROOT = "com.amazon.disco.agent";
    private static Class templateClass = null;
    private String className;
    private String methodName;
    private Object thiz;
    private Class returnType;
    private Class<?>[] argTypes;
    private T defaultValue;
    private Method method;
    static UncaughtExceptionHandler uncaughtExceptionHandler = null;

    /**
     * Private access. Use factory methods instead.
     * @param returnType the return type of the method being called`
     */
    private ReflectiveCall(Class returnType) {
        this.returnType = returnType;
    }

    /**
     * Factory method to create a new ReflectiveCall which is on a void method
     * @return a new ReflectiveCall to continue building/calling
     */
    public static ReflectiveCall returningVoid() {
        return new ReflectiveCall(void.class);
    }

    /**
     * Factory method to create a new ReflectiveCall which is on a method returning the given type
     * @param clazz the class of the method's return type
     * @param <T> the class of the method's return type
     * @return a new ReflectiveCall to continue building/calling
     */
    public static <T> ReflectiveCall<T> returning(Class<T> clazz) {
        return new ReflectiveCall<>(clazz);
    }

    /**
     * Set the default value to return in case the AlphaOne agent is not present. If none is provided, then null it
     * returns null.
     * @param defaultValue The default value
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> withDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Set the partial class name which declares the method to be called as it appears after com.amazon.alphaone.agent
     * e.g. '.config.Config' including the prefixing '.'
     * @param className the class name which declares the method
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> ofClass(String className) {
        this.className = className;
        return this;
    }

    /**
     * Set the name of the method to be called
     * @param methodName the name of the method to be called
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> ofMethod(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * For non-static methods, set the instance object on which to invoke the method
     * @param thiz the instance object on which to invoke the method
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> onInstance(Object thiz) {
        this.thiz = thiz;
        return this;
    }

    /**
     * Declare the expected method signature by its arguments
     * @param argTypes array of types for method lookup
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> withArgTypes(Class... argTypes) {
        this.argTypes = argTypes;
        return this;
    }

    /**
     * Test if the method about to be called exists or not
     * @return true if the method configured exists
     */
    public boolean methodFound() {
        if (method == null) {
            createMethod();
        }
        return method != null;
    }

    /**
     * Invoke the given method with the given args. If the AlphaOne agent is present the call
     * will proceed. In the case that building the method was not possible, due to the agent being
     * absent, this method will have no side effects.
     * @param args any arguments to pass to the method invocation.
     * @return any value which the method returns, or null if the call could not be made
     */
    public T call(Object... args) {
        String fullClassName = ALPHA_ONE_AGENT_PACKAGE_ROOT + className;
        try {
            Logger.debug("Trying to reflectively call " + fullClassName + ":" + methodName + " with parameter types:");
            Logger.debug("with arguments:");
            if (args != null) for (Object arg: args) {
                if (arg != null) {
                    Logger.debug(String.valueOf(arg));
                }
            }

            if (method == null) {
                createMethod();
            }

            if (method == null) {
                return defaultValue;
            }

            return (T)method.invoke(thiz, args);
        } catch (IllegalAccessException e) {
            Logger.warn("IllegalAccessException when trying to call " + fullClassName + ":" + methodName);
        } catch (InvocationTargetException e) {
            //the reflected method actually threw a Throwable, so pass it to the calling code if an UncaughtExceptionHandler
            //is installed
            dispatchException(e.getCause(), args);

        }

        return null;
    }

    /**
     * Test if an AlphaOne agent is present
     * @return true if an AlphaOne agent is present
     */
    public static boolean isAgentPresent() {
        try {
            if (templateClass == null) {
                templateClass = Class.forName(ALPHA_ONE_AGENT_PACKAGE_ROOT + ".AlphaOneAgentTemplate");
            }
            return true;
        } catch(ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Install an UncaughtExceptionHandler, to receive a notification whe any exception is thrown by any method in the
     * AlphaOne Customization APIs. Without such a handler installed, all exceptions - even unchecked ones - will be suppressed
     * @param handler
     */
    public static void installUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        uncaughtExceptionHandler = handler;
    }

    /**
     * Tries to dispatch an exception to the installed UncaughtExceptionHandler. Does nothing if no such handler is
     * installed
     * @param t the throwable to notify the calling code, via any installed UncaughtExceptionHandler
     * @param args any arguments to notify the UncaughtExceptionHandler of
     */
    public void dispatchException(Throwable t, Object... args) {
        if (uncaughtExceptionHandler != null) {
            uncaughtExceptionHandler.handleUncaughtException(this, args, t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(returnType.getName()+" "+ALPHA_ONE_AGENT_PACKAGE_ROOT+className+"::"+methodName+"(");
        List<String> argTypeNames = Arrays.stream(argTypes).map((x)->x.getName()).collect(Collectors.toList());
        builder.append(String.join(", ", argTypeNames));
        builder.append(")");
        return builder.toString();
    }

    /**
     * Get the class name specified in this ReflectiveCall
     * @return the class name
     */
    public String getClassName() {
        return ALPHA_ONE_AGENT_PACKAGE_ROOT + className;
    }

    /**
     * Get the class return type of the method specified in this ReflectiveCall
     * @return the return type of this method
     */
    public Class getReturnType() {
        return returnType;
    }

    /**
     * Get the name of the method specified in this ReflectiveCall
     * @return the name of this method
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Get the array of argument types specified in this ReflectiveCall
     * @return the argument types of this method
     */
    public Class[] getArgTypes() {
        return argTypes;
    }

    /**
     * Using the supplied class, method name and argument types information, create a callable Method.
     */
    private void createMethod() {
        String fullClassName = ALPHA_ONE_AGENT_PACKAGE_ROOT + className;
        try {
            Logger.debug("Trying to reflectively create " + fullClassName + ":" + methodName + " with parameter types:");
            if (argTypes != null) for (Class clazz: argTypes) {
                Logger.debug(clazz.getName());
                Logger.debug(clazz.getName());
            }
            Class clazz = Class.forName(fullClassName);
            method = clazz.getDeclaredMethod(methodName, argTypes);
        } catch (Throwable t) {
            Logger.debug("Method " + fullClassName + ":" + methodName + " was not found.");
        }
    }
}
