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

package software.amazon.disco.agent.web.apache.httpclient.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class HttpResponseAccessorTests {
    private HttpResponse response;
    private HttpResponseAccessor accessor;

    @Before
    public void before() {
        response = Mockito.mock(HttpResponse.class);
        accessor = new HttpResponseAccessor(response);
    }

    @Test
    public void testGetStatusCode() {
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(statusLine.getStatusCode()).thenReturn(404);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);

        Assert.assertEquals(404, accessor.getStatusCode());
    }

    @Test
    public void testGetContentLength() {
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        Mockito.when(entity.getContentLength()).thenReturn(1234L);
        Mockito.when(response.getEntity()).thenReturn(entity);

        Assert.assertEquals(1234L, accessor.getContentLength());
    }
}
