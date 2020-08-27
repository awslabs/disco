package software.amazon.disco.agent.web;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.disco.agent.interception.Installable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class WebSupportTests {
    @Test
    public void testWebSupport() {
        Collection<Installable> pkg = new WebSupport().get();
        Set<Installable> installables = new HashSet<>();
        installables.addAll(pkg);
        Assert.assertEquals(7, installables.size());
    }
}
