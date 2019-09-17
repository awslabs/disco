package com.amazon.disco.agent.customize.interception;

import com.amazon.disco.agent.interception.IntrusiveInterceptor;
import org.junit.Test;
import org.mockito.Mockito;

public class IntrusiveInterceptorRegistryTests {

    @Test
    public void testInstallWhenAlphaOneNotLoaded(){
        IntrusiveInterceptorRegistry.install(Mockito.mock(IntrusiveInterceptor.class));
    }


    @Test
    public void testUninstallWhenAlphaOneNotLoaded(){
        IntrusiveInterceptorRegistry.uninstall();
    }
}
