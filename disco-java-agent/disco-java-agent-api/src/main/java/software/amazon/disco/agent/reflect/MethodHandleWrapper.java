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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Simple utility to produce MethodHandles for commonly accessed methods
 * Generally we need to access methods reflectively, to account for times when the Agent is built such that it
 * is loaded into the Bootstrap classloader, which will not have direct access to classes loaded by the System classloader
 * such as Apache classes, Servlet classes, ...
 */
public class MethodHandleWrapper {
    static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private final Class rtype;
    private final MethodHandle handle;

    /**
     * Create a new MethodHandleWrapper
     * @param className the fully qualified name of the owning class
     * @param classLoader the ClassLoader to be used for finding className, should probably by ClassLoader.getSystemClassLoader()
     * @param methodName the name of the method being looked up
     * @param rtype the return type of the method
     * @param ptypes the types of the parameters to the method
     */
    public MethodHandleWrapper(String className, ClassLoader classLoader, String methodName, Class rtype, Class... ptypes) {
        MethodHandle handle = null;
        try {
            Class owner = Class.forName(className, true, classLoader);

            MethodType methodType;
            if (ptypes == null || ptypes.length == 0) {
                methodType = MethodType.methodType(rtype);
            } else {
                methodType = MethodType.methodType(rtype, ptypes);
            }

            handle = LOOKUP.findVirtual(owner, methodName, methodType);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            //do nothing?
        } finally {
            this.rtype = rtype;
            this.handle = handle;
        }
    }

    /**
     * Create a new MethodHandleWrapper
     * @param className the fully qualified name of the owning class
     * @param classLoader the ClassLoader to be used for finding className, should probably by ClassLoader.getSystemClassLoader()
     * @param methodName the name of the method being looked up
     * @param rtype the return type of the method
     */
    public MethodHandleWrapper(String className, ClassLoader classLoader, String methodName, Class rtype) {
        this(className, classLoader, methodName, rtype, (Class[])null);
    }

    /**
     * Create a new MethodHandleWrapper
     * @param className the fully qualified name of the owning class
     * @param classLoader the ClassLoader to be used for finding className, should probably by ClassLoader.getSystemClassLoader()
     * @param methodName the name of the method being looked up
     * @param rtype the return type of the method
     * @param ptypes the names of the types of the parameters to the method
     */
    public MethodHandleWrapper(String className, ClassLoader classLoader, String methodName, Class rtype, String... ptypes) {
        this(className, classLoader, methodName, rtype, classesFromClassNames(classLoader, ptypes));
    }

    /**
     * Create a new MethodHandleWrapper
     * @param className the fully qualified name of the owning class
     * @param classLoader the ClassLoader to be used for finding className, should probably by ClassLoader.getSystemClassLoader()
     * @param methodName the name of the method being looked up
     * @param rtype the name of the return type of the method
     * @param ptypes the names of the types of the parameters to the method
     * @throws Exception - ClassNotFoundException could be thrown if classpath for rtype is not found.
     */
    public MethodHandleWrapper(String className, ClassLoader classLoader, String methodName, String rtype, String... ptypes) throws Exception {
        this(className, classLoader, methodName, Class.forName(rtype, true, classLoader), ptypes);
    }

    /**
     * Invoke the method referred to by this MethodHandleWrapper
     * @param receiver the object upon which this method is being invoked, the 'this' of the method call
     * @param args the arguments being passed to the method, which should match the types given during construction as 'ptypes'
     * @return the return value of the invoked method
     */
    public Object invoke(Object receiver, Object... args) {
        Object[] invokeArgs;

        if (handle == null || receiver == null) {
            return null;
        } else {
            if (receiver != null) {
                invokeArgs = new Object[args.length+1];
                invokeArgs[0] = receiver;
                for (int i = 0; i < args.length; i++) {
                    invokeArgs[i+1] = args[i];
                }
            } else {
                invokeArgs = args;
            }

            try {
                return handle.invokeWithArguments(invokeArgs);
            } catch (Throwable t) {
                return null;
            }
        }
    }

    /**
     * Get the return type of the method referred to by this MethodHandleWrapper
     * @return the class of the method's return type
     */
    public Class getRtype() {
        return rtype;
    }

    /**
     * Helper method to transform an array of class names, into an array of actual classes
     * @param classLoader the ClassLoader used to load the given class names
     * @param names an array of fully qualified class names
     * @return an array of classes corresponding to the array of names given. If a class is not found, Object.class is used in its place
     */
    private static Class[] classesFromClassNames(ClassLoader classLoader, String[] names) {
        if (names == null) {
            return null;
        }

        Class[] classes = new Class[names.length];
        for (int i = 0; i < names.length; i++) {
            try {
                classes[i] = Class.forName(names[i], true, classLoader);
            } catch (ClassNotFoundException e) {
                classes[i] = Object.class;
            }
        }

        return classes;
    }
}
