package software.amazon.disco.agent.safety;

import org.junit.Test;

public class AWSLibrariesAbsentTests {

    @Test(expected = ClassNotFoundException.class)
    public void testSDKCoreLibraryNotPresent() throws ClassNotFoundException {
        Class.forName("software.amazon.awssdk.core.client.builder.SdkClientBuilder");
    }
}