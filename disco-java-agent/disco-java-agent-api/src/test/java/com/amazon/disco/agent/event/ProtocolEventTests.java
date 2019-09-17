package com.amazon.disco.agent.event;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class ProtocolEventTests {
    private Object request = new Object();
    private Object response = new Object();

    @Test
    public void testHTTPServletProtocolRequestEvent() {
        Map<String, String> customHeaderMap = new HashMap<>();
        customHeaderMap.put("custom-header", "data");
        HttpNetworkProtocolRequestEvent event = new HttpServletNetworkRequestEvent("Origin", 80, 1500, "127.0.0.1", "0.0.0.0")
                .withDate("Tue, 24 Oct 1995 08:12:31 GMT")
                .withHost("amazon.com")
                .withHTTPOrigin("http://aws.amazon.com")
                .withURL("http://amazon.com/alphaone")
                .withUserAgent("Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/12.0")
                .withMethod("POST")
                .withReferer("http://amazon.com/explore/something")
                .withRequest(request)
                .withHeaderMap(customHeaderMap);

        test(event);

        Assert.assertEquals("Tue, 24 Oct 1995 08:12:31 GMT", event.getDate());
        Assert.assertEquals("amazon.com", event.getHost());
        Assert.assertEquals("http://aws.amazon.com", event.getHTTPOrigin());
        Assert.assertEquals("http://amazon.com/alphaone", event.getURL());
        Assert.assertEquals("Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/12.0", event.getUserAgent());
        Assert.assertEquals("POST", event.getMethod());
        Assert.assertEquals("http://amazon.com/explore/something", event.getReferer());
        Assert.assertEquals("127.0.0.1", event.getSourceIP());
        Assert.assertEquals("0.0.0.0", event.getDestinationIP());
        Assert.assertEquals(80, event.getSourcePort());
        Assert.assertEquals(1500, event.getDestinationPort());
        Assert.assertEquals(request, event.getRequest());

        Assert.assertEquals(event.getDate(), event.getHeaderData("date"));
        Assert.assertEquals(event.getHost(), event.getHeaderData("host"));
        Assert.assertEquals(event.getHTTPOrigin(), event.getHeaderData("origin"));
        Assert.assertEquals(event.getUserAgent(), event.getHeaderData("user-agent"));
        Assert.assertEquals(event.getReferer(), event.getHeaderData("referer"));
        Assert.assertEquals(event.getLocalIPAddress(), event.getDestinationIP());
        Assert.assertEquals(event.getRemoteIPAddress(), event.getSourceIP());
        Assert.assertEquals("data", event.getHeaderData("custom-header"));
    }

    @Test
    public void testHTTPServletProtocolResponseEvent() {
        Map<String, String> customHeaderMap = new HashMap<>();
        customHeaderMap.put("custom-header", "data");
        HttpNetworkProtocolRequestEvent requestEvent = Mockito.mock(HttpNetworkProtocolRequestEvent.class);
        HttpNetworkProtocolResponseEvent event = new HttpServletNetworkResponseEvent("Origin", requestEvent)
                .withResponse(response)
                .withStatusCode(200)
                .withHeaderMap(customHeaderMap);

        test(event);

        Assert.assertEquals(response, event.getResponse());
        Assert.assertEquals(requestEvent, event.getHttpRequestEvent());
        Assert.assertEquals(200, event.getStatusCode());
        Assert.assertEquals("data", event.getHeaderData("custom-header"));
    }

    private void test(NetworkProtocolEvent event) {
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(NetworkProtocolEvent.NetworkType.TCP, event.getNetworkType());
    }
}
