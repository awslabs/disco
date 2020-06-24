package software.amazon.disco.agent.web.apache.source;

import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ApacheClientTestUtil {

    public static void verifyServiceRequestEvent(final HttpServiceDownstreamRequestEvent serviceDownstreamRequestEvent) {
        assertEquals(ApacheTestConstants.METHOD, serviceDownstreamRequestEvent.getMethod());
        assertEquals(ApacheTestConstants.URI, serviceDownstreamRequestEvent.getUri());
        assertEquals(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, serviceDownstreamRequestEvent.getOrigin());
        assertNull(serviceDownstreamRequestEvent.getRequest());
    }

    public static void verifyServiceResponseEvent(final ServiceDownstreamResponseEvent serviceDownstreamResponseEvent) {
        assertEquals(ApacheTestConstants.METHOD, serviceDownstreamResponseEvent.getOperation());
        assertEquals(ApacheTestConstants.URI, serviceDownstreamResponseEvent.getService());
        assertEquals(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN, serviceDownstreamResponseEvent.getOrigin());
    }
}
