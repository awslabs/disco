package software.amazon.disco.agent.web.apache.event;

import org.apache.http.ProtocolVersion;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.concurrent.TransactionContext;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.HttpServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.web.apache.source.MockEventBusListener;
import software.amazon.disco.agent.web.apache.source.ApacheClientTestUtil;
import software.amazon.disco.agent.web.apache.source.ApacheTestConstants;
import software.amazon.disco.agent.web.apache.utils.HttpResponseAccessor;
import software.amazon.disco.agent.web.apache.source.InterceptedBasicHttpRequest;
import software.amazon.disco.agent.web.apache.source.InterceptedBasicHttpResponse;
import software.amazon.disco.agent.web.apache.source.InterceptedHttpRequestBase;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class ApacheEventFactoryTests {

    private InterceptedBasicHttpRequest accessor;
    private InterceptedHttpRequestBase httpRequestBaseAccessor;
    private MockEventBusListener mockEventBusListener;

    @Before
    public void before() {
        accessor = new InterceptedBasicHttpRequest();
        mockEventBusListener = new MockEventBusListener();
        httpRequestBaseAccessor = new InterceptedHttpRequestBase();
        TransactionContext.create();
        EventBus.addListener(mockEventBusListener);
    }
    @Test
    public void testForRequestEventCreationForRequest() {
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN,
                accessor);
        ApacheClientTestUtil.verifyServiceRequestEvent(event);
        assertFalse(accessor.getHeaders().containsKey("TEST"));
        event.replaceHeader("TEST","TEST");
        assertTrue(accessor.getHeaders().containsKey("TEST"));
    }

    @Test
    public void testForRequestEventCreationForRequestBase() {
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN,
                   httpRequestBaseAccessor);
        ApacheClientTestUtil.verifyServiceRequestEvent(event);
    }

    @Test
    public void testForResponseEventCreationForSuccessfulResponse() {
        HttpResponseAccessor expectedResponse =  new InterceptedBasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN,accessor);
        ServiceDownstreamResponseEvent responseEvent = ApacheEventFactory.createServiceResponseEvent(expectedResponse,event,null);
        ApacheClientTestUtil.verifyServiceResponseEvent(responseEvent);
        assertNull(responseEvent.getThrown());
    }

    @Test
    public void testForResponseEventCreationForFailureResponse() {
        Exception error = new Exception("CUSTOM EXCEPTION");
        HttpServiceDownstreamRequestEvent event = ApacheEventFactory.createDownstreamRequestEvent(ApacheTestConstants.APACHE_HTTP_CLIENT_ORIGIN,accessor);
        ServiceDownstreamResponseEvent responseEvent = ApacheEventFactory.createServiceResponseEvent(null,event,error);
        ApacheClientTestUtil.verifyServiceResponseEvent(responseEvent);
        assertEquals(responseEvent.getThrown(),error);
    }
}
