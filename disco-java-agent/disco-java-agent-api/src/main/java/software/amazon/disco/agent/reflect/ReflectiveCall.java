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

package software.amazon.disco.agent.reflect;

import software.amazon.disco.agent.reflect.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Everything in disco.agent.reflect basically works the same way - attempting to reflectively invoke methods,
 * usually static ones, that reside within the Agent. Any failures are degraded gracefully to no-ops in the
 * case that the Agent is not loaded, making it safe for clients to use the reflect library in their
 * Prod code.
 *
 * @param <T> - the return type of the method being called
 */
public class ReflectiveCall<T> {
    private static final String DISCO_AGENT_PACKAGE_ROOT = "software.amazon.disco.agent";
    private static final String AGENT_TEMPLATE_CLASS_NAME = DISCO_AGENT_PACKAGE_ROOT + ".DiscoAgentTemplate";
    private static final Map<String, Class> CACHED_TYPES = new ConcurrentHashMap<>();

    private static Boolean discoTemplateClassFound;

    private String fullClassName;
    private String methodName;
    private Object thiz;
    private Class returnType;
    private Class<?>[] argTypes;
    private T defaultValue;
    private Method method;
    static UncaughtExceptionHandler uncaughtExceptionHandler = null;

    /**
     * Private access. Use factory methods instead.
     *
     * @param returnType the return type of the method being called`
     */
    private ReflectiveCall(Class returnType) {
        this.returnType = returnType;
    }

    /**
     * Factory method to create a new ReflectiveCall which is on a void method
     *
     * @return a new ReflectiveCall to continue building/calling
     */
    public static ReflectiveCall returningVoid() {
        return new ReflectiveCall(void.class);
    }

    /**
     * Factory method to create a new ReflectiveCall which is on a method returning the given type
     *
     * @param clazz the class of the method's return type
     * @param <T>   the class of the method's return type
     * @return a new ReflectiveCall to continue building/calling
     */
    public static <T> ReflectiveCall<T> returning(Class<T> clazz) {
        return new ReflectiveCall<>(clazz);
    }

    /**
     * Set the default value to return in case no DiSCo agent is present. If none is provided, then null it
     * returns null.
     *
     * @param defaultValue The default value
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> withDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Set the partial class name which declares the method to be called as it appears after software.amazon.disco.agent
     * e.g. '.config.Config' including the prefixing '.'
     *
     * @param className the partial class name which declares the method
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> ofClass(String className) {
        fullClassName = DISCO_AGENT_PACKAGE_ROOT + className;
        return this;
    }

    /**
     * Set the name of the method to be called
     *
     * @param methodName the name of the method to be called
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> ofMethod(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * For non-static methods, set the instance object on which to invoke the method
     *
     * @param thiz the instance object on which to invoke the method
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> onInstance(Object thiz) {
        this.thiz = thiz;
        return this;
    }

    /**
     * Declare the expected method signature by its arguments
     *
     * @param argTypes array of types for method lookup
     * @return the ReflectiveCall to continue building/calling
     */
    public ReflectiveCall<T> withArgTypes(Class... argTypes) {
        this.argTypes = argTypes;
        return this;
    }

    /**
     * Test if the method about to be called exists or not
     *
     * @return true if the method configured exists
     */
    public boolean methodFound() {
        if (method == null) {
            createMethod();
        }
        return method != null;
    }

    /**
     * Invoke the given method with the given args. If a DiSCo agent is present the call
     * will proceed. In the case that building the method was not possible, due to the agent being
     * absent, this method will have no side effects.
     *
     * @param args any arguments to pass to the method invocation.
     * @return any value which the method returns, or null if the call could not be made
     */
    public T call(Object... args) {
        try {
            if (method == null) {
                createMethod();
            }

            if (method == null) {
                return defaultValue;
            }

            return (T) method.invoke(thiz, args);
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
     * Test if a DiSCo agent is present.
     *
     * @return true if a DiSCo agent is present
     */
    public static boolean isAgentPresent() {
        if (discoTemplateClassFound == null) {
            // perform a reflective call only if 'discoTemplateClassFound' is null.
            // the value of 'discoTemplateClassFound' will be cached and returned until the 'resetCache()' method is invoked.
            discoTemplateClassFound = retrieveCachedType(AGENT_TEMPLATE_CLASS_NAME) != null;
        }
        return discoTemplateClassFound;
    }

    /**
     * Install an UncaughtExceptionHandler, to receive a notification whe any exception is thrown by any method in the
     * DiSCo reflect APIs. Without such a handler installed, all exceptions - even unchecked ones - will be suppressed
     *
     * @param handler a callback to receive any unhandled exceptions in client code
     */
    public static void installUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        uncaughtExceptionHandler = handler;
    }

    /**
     * Tries to dispatch an exception to the installed UncaughtExceptionHandler. Does nothing if no such handler is
     * installed
     *
     * @param t    the throwable to notify the calling code, via any installed UncaughtExceptionHandler
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
        builder.append(returnType.getName() + " " + fullClassName + "::" + methodName + "(");
        List<String> argTypeNames = Arrays.stream(argTypes).map((x) -> x.getName()).collect(Collectors.toList());
        builder.append(String.join(", ", argTypeNames));
        builder.append(")");
        return builder.toString();
    }

    /**
     * Get the class name specified in this ReflectiveCall
     *
     * @return the class name
     */
    public String getClassName() {
        return fullClassName;
    }

    /**
     * Get the class return type of the method specified in this ReflectiveCall
     *
     * @return the return type of this method
     */
    public Class getReturnType() {
        return returnType;
    }

    /**
     * Get the name of the method specified in this ReflectiveCall
     *
     * @return the name of this method
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Get the array of argument types specified in this ReflectiveCall
     *
     * @return the argument types of this method
     */
    public Class[] getArgTypes() {
        return argTypes;
    }

    /**
     * Reset the type cache and the value of isAgentPresent.
     */
    public static void resetCache() {
        CACHED_TYPES.clear();
        discoTemplateClassFound = null;
    }

    /**
     * Getter for all types loaded via Reflection and cached.
     * <p>
     * Package private for testing.
     *
     * @return map of all cached types with fully qualified class name as key and class definition as value.
     */
    static Map<String, Class> getCachedTypes() {
        return CACHED_TYPES;
    }

    /**
     * Set the value for 'discoTemplateClassFound' for testing purposes.
     * <p>
     * Package private for testing
     *
     * @param classFound boolean value to be set for the field in question.
     */
    static void setDiscoTemplateClassFound(final boolean classFound) {
        discoTemplateClassFound = classFound;
    }

    /**
     * Get the value for 'discoTemplateClassFound' for testing purposes.
     * <p>
     * Package private for testing
     *
     * @return Boolean value of discoTemplateClassFound if set, null otherwise
     */
    static Boolean getDiscoTemplateClassFound() {
        return discoTemplateClassFound;
    }

    /**
     * Using the supplied class, method name and argument types information, create a callable Method.
     */
    private void createMethod() {
        try {
            // prevent further reflective operations to be made when the agent is absent.
            if (isAgentPresent()) {
                final Class clazz = retrieveCachedType(fullClassName);

                if (clazz != null) {
                    method = clazz.getDeclaredMethod(methodName, argTypes);
                }
            }
        } catch (Throwable t) {
            //do nothing
        }
    }

    /**
     * Attempt to retrieve the class type using the supplied fully qualified class name. If successful, the type will be returned and cached, otherwise null will be returned.
     * <p>
     * Package private for testing.
     *
     * @param fullClassName fully qualified class name of the type
     * @return type of the supplied class name if can be resolved, null otherwise.
     */
    static Class retrieveCachedType(final String fullClassName) {
        if (CACHED_TYPES.get(fullClassName) == null) {
            try {
                CACHED_TYPES.put(fullClassName, Class.forName(fullClassName));
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        return CACHED_TYPES.get(fullClassName);
    }
}
