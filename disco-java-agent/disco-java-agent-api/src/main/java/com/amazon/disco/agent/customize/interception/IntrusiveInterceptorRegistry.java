package com.amazon.disco.agent.customize.interception;


import com.amazon.disco.agent.customize.ReflectiveCall;
import com.amazon.disco.agent.customize.logging.Logger;
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
