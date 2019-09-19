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

package com.amazon.disco.agent.reflect.interception;


import com.amazon.disco.agent.reflect.ReflectiveCall;
import com.amazon.disco.agent.reflect.logging.Logger;
import com.amazon.disco.agent.interception.IntrusiveInterceptor;

/**
 * Registry of all intrusive interceptors for an applicaiton
 */
public class IntrusiveInterceptorRegistry {

    static final String REGISTRY_CLASS = ".interception.IntrusiveInterceptorRegistry";

    /**
     * Add an intrusive interceptor to the registry
     *
     * @param interceptor the IntrusiveInterceptor to be installed to receive intrusive opportunities
     * @return the previous installed interceptor
     */
    public static IntrusiveInterceptor install(final IntrusiveInterceptor interceptor){
        Logger.info("Installing intrusive interceptor " + interceptor );
        return ReflectiveCall.returning(IntrusiveInterceptor.class)
                .ofClass(REGISTRY_CLASS)
                .ofMethod("install")
                .withArgTypes(IntrusiveInterceptor.class)
                .call(interceptor);
    }


    /**
     * Remove the intrusive interceptor from the registry
     *
     * @return the previous installed interceptor
     */
    public static IntrusiveInterceptor uninstall(){
        Logger.info("Uninstalling intrusive interceptors" );
        return ReflectiveCall.returning(IntrusiveInterceptor.class)
                .ofClass(REGISTRY_CLASS)
                .ofMethod("uninstall")
                .call();
    }

}
