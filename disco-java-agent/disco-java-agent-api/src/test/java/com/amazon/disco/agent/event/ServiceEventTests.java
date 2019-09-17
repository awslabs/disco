package com.amazon.disco.agent.event;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ServiceEventTests {
    private Object request = new Object();
    private Object response = new Object();
    private Throwable thrown = new RuntimeException();

    @Test
    public void testServiceActivityRequestEvent() {
        AbstractServiceRequestEvent event = new ServiceActivityRequestEvent("Origin", "Service", "Operation")
            .withRequest(request);
        test(event);

        Assert.assertEquals(request, event.getRequest());
    }

    @Test
    public void testServiceActivityResponseEvent() {
        ServiceRequestEvent requestEvent = Mockito.mock(ServiceRequestEvent.class);
        AbstractServiceResponseEvent event = new ServiceActivityResponseEvent("Origin", "Service", "Operation", requestEvent)
            .withResponse(response)
            .withThrown(thrown);
        test(event);

        Assert.assertEquals(response, event.getResponse());
        Assert.assertEquals(thrown, event.getThrown());
    }

    @Test
    public void testServiceDownstreamRequestEvent() {
        AbstractServiceRequestEvent event = new ServiceDownstreamRequestEvent("Origin", "Service", "Operation")
            .withRequest(request);
        test(event);

        Assert.assertEquals(request, event.getRequest());
    }

    @Test
    public void testServiceDownstreamResponseEvent() {
        ServiceRequestEvent requestEvent = Mockito.mock(ServiceRequestEvent.class);
        AbstractServiceResponseEvent event = new ServiceDownstreamResponseEvent("Origin", "Service", "Operation", requestEvent)
            .withResponse(response)
            .withThrown(thrown);
        test(event);

        Assert.assertEquals(response, event.getResponse());
        Assert.assertEquals(thrown, event.getThrown());
    }

    private void test(AbstractServiceEvent event) {
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals("Service", event.getService());
        Assert.assertEquals("Operation", event.getOperation());
    }
}
