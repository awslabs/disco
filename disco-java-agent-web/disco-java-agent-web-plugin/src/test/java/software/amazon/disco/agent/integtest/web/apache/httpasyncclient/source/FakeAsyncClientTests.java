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

package software.amazon.disco.agent.integtest.web.apache.httpasyncclient.source;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Internal tests for {@link FakeAsyncClient}
 */
@RunWith(DataProviderRunner.class)
public class FakeAsyncClientTests {

    @Test
    @UseDataProvider("desiredCallbackStates")
    public void testCallbackStates(FakeAsyncClient.DesiredState state) {
        HttpUriRequest request = mockRequest();
        FakeAsyncClient client = new FakeAsyncClient(state);
        client.execute(request, null);

        assertEquals(4, client.executeCallChainDepth);
    }

    @Test(expected = FakeAsyncClient.FakeRuntimeException.class)
    public void testThrowableThrown() {
        HttpUriRequest request = mockRequest();
        FakeAsyncClient client = new FakeAsyncClient(FakeAsyncClient.DesiredState.THROWABLE_THROWN);

        try {
            client.execute(request, null);
        } finally {
            assertEquals(4, client.executeCallChainDepth);
        }
    }

    @DataProvider
    public static Object[][] desiredCallbackStates() {
        return new Object[][] {
                { FakeAsyncClient.DesiredState.CALLBACK_COMPLETED },
                { FakeAsyncClient.DesiredState.CALLBACK_FAILED },
                { FakeAsyncClient.DesiredState.CALLBACK_CANCELLED }
        };
    }

    private static HttpUriRequest mockRequest() {
        HttpUriRequest request = mock(HttpUriRequest.class);
        RequestLine requestLine = mock(RequestLine.class);

        when(request.getRequestLine()).thenReturn(requestLine);
        when(requestLine.getUri()).thenReturn("http://amazon.com/explore/something");
        when(requestLine.getMethod()).thenReturn("GET");

        return request;
    }
}