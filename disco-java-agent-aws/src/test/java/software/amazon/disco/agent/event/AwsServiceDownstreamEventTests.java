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

package software.amazon.disco.agent.event;

import com.amazonaws.DefaultRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.http.SdkHttpFullRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AwsServiceDownstreamEventTests {
    private static final String ORIGIN = "origin";
    private static final String SERVICE = "service";
    private static final String OPERATION = "operation";
    private static final String REGION = "region";
    private static final String REQUEST_ID = "123456789";
    private static final String HEADER_KEY = "some-header";
    private static final String HEADER_VALUE = "some-value";

    private Map<String, List<String>> headerMap;

    @Mock
    SdkHttpFullRequest httpRequestMock;

    @Mock
    SdkHttpFullRequest.Builder httpRequestBuilderMock;

    @Before
    public void setup() {
        headerMap = new HashMap<>();

        MockitoAnnotations.openMocks(this);
        when(httpRequestMock.toBuilder()).thenReturn(httpRequestBuilderMock);
        when(httpRequestBuilderMock.build()).thenReturn(httpRequestMock);
        when(httpRequestBuilderMock.appendHeader(anyString(), anyString())).thenAnswer(new AppendHeader(headerMap));
        when(httpRequestMock.headers()).thenReturn(headerMap);
    }

    @Test
    public void testAwsServiceDownstreamRequestEvent() {
        AwsServiceDownstreamRequestEvent requestEvent = new AwsServiceDownstreamRequestEventImpl(ORIGIN, SERVICE, OPERATION)
                .withRegion(REGION);

        verifyEvent(requestEvent);
        Assert.assertEquals(REGION, requestEvent.getRegion());
    }

    @Test
    public void testAwsServiceDownstreamResponseEvent() {
        AwsServiceDownstreamRequestEvent requestEvent = new AwsServiceDownstreamRequestEventImpl(ORIGIN, SERVICE, OPERATION)
                .withRegion(REGION);
        AwsServiceDownstreamResponseEvent responseEvent = new AwsServiceDownstreamResponseEventImpl(requestEvent)
                .withRetryCount(5)
                .withRequestId(REQUEST_ID);

        verifyEvent(responseEvent);
        Assert.assertEquals(5, responseEvent.getRetryCount());
        Assert.assertEquals(REQUEST_ID, responseEvent.getRequestId());
    }

    @Test
    public void testReplaceHeaderInV1Request() {
        DefaultRequest<?> request = new DefaultRequest<>("my_service");
        Assert.assertTrue(request.getHeaders().isEmpty());

        ServiceDownstreamRequestEvent event = new AwsV1ServiceDownstreamRequestEventImpl(ORIGIN, SERVICE, OPERATION);
        event.withRequest(request);

        HeaderReplaceable replaceable = (HeaderReplaceable) event;
        replaceable.replaceHeader(HEADER_KEY, HEADER_VALUE);

        Map<String, String> headers = request.getHeaders();
        Assert.assertEquals(1, headers.size());
        Assert.assertEquals(HEADER_VALUE, headers.get(HEADER_KEY));
    }

    @Test
    public void testReplaceHeaderInV2Request() {
        AwsServiceDownstreamRequestEventImpl requestEvent = new AwsServiceDownstreamRequestEventImpl(ORIGIN, SERVICE, OPERATION)
                .withHeaderMap(headerMap)
                .withSdkHttpRequest(httpRequestMock);

        requestEvent.replaceHeader(HEADER_KEY, HEADER_VALUE);

        Assert.assertEquals(headerMap, requestEvent.getAllHeaders());
        Assert.assertEquals(1, headerMap.size());
        Assert.assertEquals(1, headerMap.get(HEADER_KEY).size());
        Assert.assertEquals(HEADER_VALUE, headerMap.get(HEADER_KEY).get(0));
    }

    private void verifyEvent(ServiceEvent event) {
        Assert.assertEquals(ORIGIN, event.getOrigin());
        Assert.assertEquals(SERVICE, event.getService());
        Assert.assertEquals(OPERATION, event.getOperation());
    }

    private static class AppendHeader implements Answer<Object> {
        public final Map<String, List<String>> headers;

        public AppendHeader(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        @Override
        public Object answer(InvocationOnMock invocationOnMock) {
            List<String> valueList = new ArrayList<>();
            valueList.add(invocationOnMock.getArgument(1));
            headers.put(invocationOnMock.getArgument(0), valueList);
            return invocationOnMock.getMock();
        }
    }
}
