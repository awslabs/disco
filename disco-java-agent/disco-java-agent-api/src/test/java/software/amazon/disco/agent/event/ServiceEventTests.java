/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.agent.event;

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

    @Test
    public void testHttpServiceDownstreamResponseEvent() {
        ServiceDownstreamRequestEvent requestEvent = Mockito.mock(ServiceDownstreamRequestEvent.class);
        HttpServiceDownstreamResponseEvent responseEvent = new HttpServiceDownstreamResponseEvent("Origin", "Service", "Operation", requestEvent)
                .withStatusCode(200)
                .withContentLength(42L);

        test(responseEvent);

        Assert.assertEquals(200, responseEvent.getStatusCode());
        Assert.assertEquals(42L, responseEvent.getContentLength());
    }

    @Test
    public void testHttpServiceDownstreamResponseEventWithoutResponse() {
        ServiceDownstreamRequestEvent requestEvent = Mockito.mock(ServiceDownstreamRequestEvent.class);
        HttpServiceDownstreamResponseEvent responseEvent = new HttpServiceDownstreamResponseEvent("Origin", "Service", "Operation", requestEvent);

        test(responseEvent);

        Assert.assertEquals(-1, responseEvent.getStatusCode());
        Assert.assertEquals(-1L, responseEvent.getContentLength());
    }

    private void test(AbstractServiceEvent event) {
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals("Service", event.getService());
        Assert.assertEquals("Operation", event.getOperation());
    }
}
