package software.amazon.disco.agent.plugin.source;

import net.bytebuddy.agent.builder.AgentBuilder;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.interception.Package;

import java.util.Arrays;
import java.util.Collection;

public class PluginPackage implements Package {
    @Override
    public Collection<Installable> get() {
        return Arrays.asList(
                new PluginInstallable(),
                new OtherInstallable()
        );
    }

    public static class OtherInstallable implements Installable {
        @Override
        public AgentBuilder install(AgentBuilder agentBuilder) {
            return null;
        }
    }
}
