package software.amazon.disco.agent.awsv2;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.utils.AttributeMap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AWSClientBuilderInterceptorTests {
    private AWSClientBuilderInterceptor interceptor;

    @Spy
    SdkClientBuilder builderSpy;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        interceptor = new AWSClientBuilderInterceptor();
    }

    @Test
    public void testInstallation() {
        AgentBuilder agentBuilder = mock(AgentBuilder.class);
        AgentBuilder.Identified.Extendable extendable = mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = mock(AgentBuilder.Identified.Narrowable.class);
        when(agentBuilder.type(any(ElementMatcher.class))).thenReturn(narrowable);
        when(narrowable.transform(any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        AgentBuilder result = interceptor.install(agentBuilder);
        assertSame(extendable, result);
    }

    @Test
    public void testCorrectClassMatches() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        Assert.assertTrue(classMatches(builder.getClass()));
    }

    @Test
    public void testIncorrectClassDoesNotMatch() {
        Assert.assertFalse(classMatches(String.class));
    }

    @Test
    public void testMethodMatches() throws NoSuchMethodException {
        Assert.assertEquals(1, methodMatchedCount("build", MyBuilder.class));
    }

    @Test
    public void testInterfaceMethodDoesNotMatch() throws NoSuchMethodException {
        Assert.assertEquals(0, methodMatchedCount("build", SdkHttpClient.Builder.class));
    }

    @Test
    public void testMethodEnter() {
        AWSClientBuilderInterceptor.AWSClientBuilderInterceptorMethodDelegation.enter(builderSpy, "origin");

        verify(builderSpy, times(1)).overrideConfiguration((ClientOverrideConfiguration) any());
    }

    private static boolean classMatches(Class clazz) {
        return AWSClientBuilderInterceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
    }

    /**
     * Helper method to test the method matcher against an input class
     *
     * @param methodName name of method
     * @param paramType class we are verifying contains the method
     * @return Matched methods count
     * @throws NoSuchMethodException
     */
    private int methodMatchedCount(String methodName, Class paramType) throws NoSuchMethodException {
        List<Method> methods = new ArrayList<>();
        for (Method m : paramType.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                System.out.println(m);
                methods.add(m);
            }
        }

        if (methods.size() == 0) throw new NoSuchMethodException();

        int matchedCount = 0;
        for (Method m : methods) {
            MethodDescription.ForLoadedMethod forLoadedMethod = new MethodDescription.ForLoadedMethod(m);
            if (AWSClientBuilderInterceptor.buildMethodMatcher().matches(forLoadedMethod)) {
                matchedCount++;
            }
        }
        return matchedCount;
    }

    private static final class MyBuilder implements SdkHttpClient.Builder {

        @Override
        public final SdkHttpClient build() {
            return null;
        }

        @Override
        public SdkHttpClient buildWithDefaults(AttributeMap attributeMap) {
            return null;
        }
    }
}
