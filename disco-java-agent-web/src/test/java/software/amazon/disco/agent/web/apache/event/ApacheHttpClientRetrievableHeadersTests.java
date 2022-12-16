/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.disco.agent.web.apache.event;

import org.apache.http.ProtocolVersion;
import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.agent.web.apache.source.InterceptedBasicHttpRequest;
import software.amazon.disco.agent.web.apache.source.InterceptedBasicHttpResponse;
import software.amazon.disco.agent.web.apache.source.InterceptedHttpRequestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ApacheHttpClientRetrievableHeadersTests {
    private InterceptedBasicHttpRequest request;
    private InterceptedHttpRequestBase requestBase;
    private InterceptedBasicHttpResponse response;

    @Before
    public void before() {
        request = new InterceptedBasicHttpRequest();
        request.addHeader("someheader", "somedata");
        request.addHeader("someheader2", "somedata2");
        requestBase = new InterceptedHttpRequestBase();
        requestBase.addHeader("someheader", "somedata");
        requestBase.addHeader("someheader2", "somedata2");
        response = new InterceptedBasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");
        response.addHeader("someheader", "somedata");
        response.addHeader("someheader2", "somedata2");

    }

    @Test
    public void testRetrievableHeaderWithHttpRequest() {
        ApacheHttpClientRetrievableHeaders headers = new ApacheHttpClientRetrievableHeaders(request);
        testHeaders(headers);
    }

    @Test
    public void testRetrievableHeaderWithHttpRequestBase() {
        ApacheHttpClientRetrievableHeaders headers = new ApacheHttpClientRetrievableHeaders(requestBase);
        testHeaders(headers);
    }

    @Test
    public void testRetrievableHeaderWithHttpResponse() {
        ApacheHttpClientRetrievableHeaders headers = new ApacheHttpClientRetrievableHeaders(response);
        testHeaders(headers);
    }

    private static void testHeaders(ApacheHttpClientRetrievableHeaders headers) {
        assertEquals(headers.getFirstHeader("someheader"), "somedata");
        assertEquals(headers.getFirstHeader("someheader2"), "somedata2");
        assertNull(headers.getHeaders("nonexistentheader"));
    }
}
