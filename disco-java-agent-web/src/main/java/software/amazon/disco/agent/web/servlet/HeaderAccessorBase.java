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

package software.amazon.disco.agent.web.servlet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for the concrete accessors of HttpServletRequest and HttpServletResponse
 */
public abstract class HeaderAccessorBase implements HeaderAccessor {
    final static MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    final Object requestOrResponse;

    /**
     * Construct a new HeaderAccessor with either an HttpServletRequest or HttpServletResponse object
     * @param requestOrResponse the request or response object
     */
    HeaderAccessorBase(Object requestOrResponse) {
        this.requestOrResponse = requestOrResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getObject() {
        return requestOrResponse;
    }

    /**
     * Lookup a MethodHandle if encountering for the first time, or reuse a cached one on subsequent calls
     *
     * @param current a boxed MethodHandle, contained in an AtomicReference, null on first usage
     * @param methodName the name of the method to reflect e.g. getStatus, getHeader...
     * @param returnType the return type of the reflected method e.g. int.class or String.class
     * @return the returned value from the called reflected method, if any
     */
    Object maybeInitAndCall(AtomicReference<MethodHandle> current, String methodName, Class<?> returnType) {
        maybeInit(current, methodName, MethodType.methodType(returnType));
        return invokeSafely(current, new Object[]{getObject()});
    }

    /**
     * Lookup a MethodHandle if encountering for the first time, or reuse a cached one on subsequent calls
     *
     * @param current a boxed MethodHandle, contained in an AtomicReference, null on first usage
     * @param methodName the name of the method to reflect e.g. getStatus, getHeader...
     * @param returnType the return type of the reflected method e.g. int.class or String.class
     * @param paramType the param type of the singular parameter the methods takes. Methods with arity>1 not supported. Can be void.class for a void method.
     * @param arg the optional argument to pass in to the reflected method
     * @return the returned value from the called reflected method, if any
     */
    Object maybeInitAndCall(AtomicReference<MethodHandle> current, String methodName, Class<?> returnType, Class<?> paramType, Object arg) {
        maybeInit(current, methodName, MethodType.methodType(returnType, paramType));
        return invokeSafely(current, new Object[]{getObject(), arg});
    }

    /**
     * Helper method to safely try to initialize the MethodHandle prior to calling it
     * @param call the reference to the methodhandle
     * @param methodName the name of the method to reflect e.g. getStatus, getHeader...
     * @param type the MethodType of the underlying method based on its rType and pType
     */
    private void maybeInit(AtomicReference<MethodHandle> call, String methodName, MethodType type) {
        if (getClassOf() == null) {
            return;
        }

        try {
            if (call.get() == null) { //outer check to avoid the findVirtualCall being made unnecessarily
                call.compareAndSet(null, lookup.findVirtual(getClassOf(), methodName, type));
            }
        }   catch (NoSuchMethodException | IllegalAccessException e) {
            //swallow , try again next time
        }
    }

    /**
     * Helper method to safely invoke the reflected MethodHandle
     * @param call the reference to the methodhandle
     * @param args any args it takes, including the receiver of the virtual invoke
     * @return any returned value
     */
    private Object invokeSafely(AtomicReference<MethodHandle> call, Object[] args) {
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
