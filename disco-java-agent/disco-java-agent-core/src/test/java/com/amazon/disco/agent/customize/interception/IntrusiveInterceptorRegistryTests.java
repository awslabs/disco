package com.amazon.disco.agent.customize.interception;

import com.amazon.disco.agent.interception.IntrusiveInterceptor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class IntrusiveInterceptorRegistryTests {
    @Before
    public void before() {
        IntrusiveInterceptorRegistry.uninstall();
    }

    @After
    public void after() {
        IntrusiveInterceptorRegistry.uninstall();
    }
    @Test
    public void testInstall() {
        IntrusiveInterceptor i = Mockito.mock(IntrusiveInterceptor.class);
        IntrusiveInterceptor old = IntrusiveInterceptorRegistry.install(i);
        Assert.assertEquals(old.getClass().getName(), "com.amazon.alphaone.agent.interception.IntrusiveInterceptorRegistry$DefaultIntrusiveInterceptor");
        old = IntrusiveInterceptorRegistry.uninstall();
        Assert.assertEquals(i, old);
    }
}
