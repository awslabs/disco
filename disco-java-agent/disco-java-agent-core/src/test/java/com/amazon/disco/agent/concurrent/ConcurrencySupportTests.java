package com.amazon.disco.agent.concurrent;

import com.amazon.disco.agent.interception.Installable;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ConcurrencySupportTests {
    @Test
    public void testPackageContentCorrect() {
        List<Installable> installables = (List<Installable>)new ConcurrencySupport().get();
        Assert.assertEquals(5, installables.size());
        Assert.assertEquals(ExecutorInterceptor.class, installables.get(0).getClass());
        Assert.assertEquals(ForkJoinPoolInterceptor.class, installables.get(1).getClass());
        Assert.assertEquals(ForkJoinTaskInterceptor.class, installables.get(2).getClass());
        Assert.assertEquals(ThreadInterceptor.class, installables.get(3).getClass());
        Assert.assertEquals(ThreadSubclassInterceptor.class, installables.get(4).getClass());
    }
}
