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
package software.amazon.disco.agent.web.apache.source;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.mockito.Mockito;

import java.util.HashMap;

public class InterceptedBasicHttpRequest implements HttpRequest {
    private HashMap<String, BasicHeader> headers = new HashMap<>();
    private RequestLine requestLine = Mockito.mock(RequestLine.class);

    public InterceptedBasicHttpRequest(){
        Mockito.when(requestLine.getUri()).thenReturn(ApacheTestConstants.URI);
        Mockito.when(requestLine.getMethod()).thenReturn(ApacheTestConstants.METHOD);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return null;
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public Header[] getHeaders(String name) {
        return new Header[]{headers.get(name)};
    }

    @Override
    public Header getFirstHeader(String name) {
        return headers.getOrDefault(name, new BasicHeader(name, null));
    }

    @Override
    public Header getLastHeader(String name) {
        return null;
    }

    @Override
    public Header[] getAllHeaders() {
        return headers.values().toArray(new BasicHeader[0]);
    }

    @Override
    public void addHeader(Header header) {}

    @Override
    public void addHeader(String name, String value) {
        headers.put(name, new BasicHeader(name, value));
    }

    @Override
    public void setHeader(Header header) { }

    @Override
    public void setHeader(String name, String value) { }

    @Override
    public void setHeaders(Header[] headerArray) {}

    @Override
    public void removeHeader(Header header) { }

    @Override
    public void removeHeaders(String name) {
        headers.remove(name);
    }

    @Override
    public HeaderIterator headerIterator() { return null; }
    @Override
    public HeaderIterator headerIterator(String name) { return null; }
    @Override
    public HttpParams getParams() { return null; }
    @Override
    public void setParams(HttpParams params) { }
    @Override
    public RequestLine getRequestLine() {
        return requestLine;
    }
}