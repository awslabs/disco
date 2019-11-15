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

package software.amazon.disco.agent.web;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for the concrete accessors of publicly accessible fields and methods.
 */
public abstract class AccessorBase {

    protected static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();
    protected final Object accessingObject;

    /**
     * Construct a new accessor with any object.
     *
     * @param accessingObject the object which this accessor accesses
     */
    protected AccessorBase(final Object accessingObject) {
        this.accessingObject = accessingObject;
    }

    /**
     * Get the real class of this accessor.
     *
     * @return the class which this accessor accesses
     */
    protected abstract Class<?> getClassOf();

    /**
     * Get the actual object, this accessor accesses, contained by this instance.
     *
     * @return an object which this accessor accesses
     */
    public Object getObject() {
        return accessingObject;
    }

    /**
     * Lookup a MethodHandle if encountering for the first time, or reuse a cached one on subsequent calls
     *
     * @param current a boxed MethodHandle, contained in an AtomicReference, null on first usage
     * @param methodName the name of the method to reflect e.g. getStatus, getHeader...
     * @param returnType the return type of the reflected method e.g. int.class or String.class
     * @param args the optional arguments to pass in to the reflected method
     * @return the returned value from the called reflected method, if any
     */
    @SafeVarargs
    protected final Object maybeInitAndCall(final AtomicReference<MethodHandle> current,
                                            final String methodName,
                                            final Class<?> returnType,
                                            final Map.Entry<Class<?>, Object>... args) {
        // ensure returnType is not null
        if (returnType == null) {
            return null;
        }

        // prefer array access via indexes over declarative way e.g. Arrays.stream(args) for the sake of performance
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < paramTypes.length; i++) {
            final Class<?> currentParamType = args[i].getKey();
            // ensure paramTypes are not null
            if (currentParamType == null) {
                return null;
            }
            paramTypes[i] = currentParamType;
        }

        maybeInit(current, methodName, MethodType.methodType(returnType, paramTypes));

        // insert `getObject` right in front of `args`
        // prefer array access via indexes for the sake of performance
        Object[] callArgs = new Object[args.length + 1];
        Arrays.setAll(callArgs, i -> i == 0 ? getObject() : args[i-1].getValue());

        return invokeSafely(current, callArgs);
    }

    /**
     * Helper method to safely try to initialize the MethodHandle prior to calling it.
     *
     * @param call the reference to the MethodHandle
     * @param methodName the name of the method to reflect e.g. getStatus, getHeader...
     * @param type the MethodType of the underlying method based on its rType and pType
     */
    private void maybeInit(final AtomicReference<MethodHandle> call, final String methodName, final MethodType type) {
        if (getClassOf() == null) {
            return;
        }

        try {
            if (call.get() == null) { //outer check to avoid the findVirtualCall being made unnecessarily
                call.compareAndSet(null, PUBLIC_LOOKUP.findVirtual(getClassOf(), methodName, type));
            }
        }   catch (NoSuchMethodException | IllegalAccessException e) {
            //swallow , try again next time
        }
    }

    /**
     * Helper method to safely invoke the reflected MethodHandle.
     *
     * @param call the reference to the MethodHandle
     * @param args any args it takes, including the receiver of the virtual invoke
     * @return any returned value
     */
    private Object invokeSafely(final AtomicReference<MethodHandle> call, final Object[] args) {
        if (call.get() != null) {
            try {
                //would prefer to use invokeExact for speed, but that needs compile-time visibility of the real classes
                //i.e. HttpServletRequest and HttpServletResponse, which we cannot explicitly reference since that would cause
                //classloading and interfere with interception
                return call.get().invokeWithArguments(args);
            } catch (Throwable t) {
                //swallow for safety
            }
        }
        return null;
    }
}
