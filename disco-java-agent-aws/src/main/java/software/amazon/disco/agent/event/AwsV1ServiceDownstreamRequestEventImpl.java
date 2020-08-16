package software.amazon.disco.agent.event;

import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.lang.reflect.Method;

/**
 * Extension of {@link ServiceDownstreamRequestEvent} that implements a AWS SDK specific replace header method.
 */
public class AwsV1ServiceDownstreamRequestEventImpl extends ServiceDownstreamRequestEvent implements HeaderReplaceable {
    private static final Logger log = LogManager.getLogger(AwsV1ServiceDownstreamRequestEventImpl.class);
    private static final String ADD_HEADER = "addHeader";

    /**
     * {@inheritDoc}
     */
    public AwsV1ServiceDownstreamRequestEventImpl(String origin, String service, String operation) {
        super(origin, service, operation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replaceHeader(String key, String value) {
        Object awsSdkRequest = this.getRequest();
        try {
            Method addHeader = awsSdkRequest.getClass().getDeclaredMethod(ADD_HEADER, String.class, String.class);
            addHeader.invoke(awsSdkRequest, key, value);
            return true;
        } catch (Exception e) {
            log.warn("Disco(AWSv1) Failed to add header '" + key + "' to AWS SDK Request", e);
            return false;
        }
    }
}
