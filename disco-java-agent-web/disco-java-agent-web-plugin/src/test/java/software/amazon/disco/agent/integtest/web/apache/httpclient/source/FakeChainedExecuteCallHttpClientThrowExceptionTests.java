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

package software.amazon.disco.agent.integtest.web.apache.httpclient.source;

import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Internal tests for {@link FakeChainedExecuteCallHttpClientThrowException}
 * Expecting an exception.
 */
public class FakeChainedExecuteCallHttpClientThrowExceptionTests {

    @Test(expected = IOException.class)
    public void testFakeChainedExecuteCallHttpClientThrowException() throws IOException {
        HttpUriRequest request = mock(HttpUriRequest.class);
        RequestLine requestLine = mock(RequestLine.class);

        when(request.getRequestLine()).thenReturn(requestLine);
        when(requestLine.getUri()).thenReturn("http://amazon.com/explore/something");
        when(requestLine.getMethod()).thenReturn("GET");

        // Set up victim http client
        FakeChainedExecuteCallHttpClientThrowException httpClient = new FakeChainedExecuteCallHttpClientThrowException();

        try {
            httpClient.execute(request);
        } finally {
            assertEquals(3, httpClient.executeCallChainDepth);
        }
    }
}