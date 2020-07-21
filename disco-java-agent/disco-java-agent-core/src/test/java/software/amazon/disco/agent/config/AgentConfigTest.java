package software.amazon.disco.agent.config;

import net.bytebuddy.agent.builder.AgentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.disco.agent.interception.Installable;

import java.util.function.BiFunction;

public class AgentConfigTest {
    static AgentConfig config = new AgentConfig(null);

    @Test
    public void testDefaultAgentBuilderTransformer() {
        Assert.assertNotNull(config.getAgentBuilderTransformer());
    }

    @Test
    public void testSetNonDefaultAgentBuilderTransformer() {
        BiFunction<AgentBuilder, Installable, AgentBuilder> defaultTransformer = config.getAgentBuilderTransformer();
        BiFunction<AgentBuilder, Installable, AgentBuilder> mockTransformer = Mockito.mock(BiFunction.class);

        AgentConfig.setAgentBuilderTransformer(mockTransformer);

        Assert.assertNotEquals(defaultTransformer, config.getAgentBuilderTransformer());
    }

    @Test
    public void testSetNullAgentBuilderTransformerResetsToDefaultTransformer(){
        AgentConfig.setAgentBuilderTransformer(null);

        Assert.assertNotNull(config.getAgentBuilderTransformer());
    }
}
