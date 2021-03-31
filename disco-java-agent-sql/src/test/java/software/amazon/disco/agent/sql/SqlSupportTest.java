package software.amazon.disco.agent.sql;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.disco.agent.interception.Installable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SqlSupportTest {
    @Test
    public void testSqlSupport() {
        Collection<Installable> pkg = new SqlSupport().get();
        Set<Installable> installables = new HashSet<>();
        installables.addAll(pkg);
        Assert.assertEquals(2, installables.size());
    }
}
