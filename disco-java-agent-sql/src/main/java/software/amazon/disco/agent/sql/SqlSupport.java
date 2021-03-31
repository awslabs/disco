package software.amazon.disco.agent.sql;

import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.Package;

import java.util.Arrays;
import java.util.Collection;

/**
 * Package definition for the disco-java-agent-sql package.
 */
public class SqlSupport implements Package {

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Installable> get() {
        return Arrays.asList(new JdbcExecuteInterceptor(), new ConnectionInterceptor());
    }
}
