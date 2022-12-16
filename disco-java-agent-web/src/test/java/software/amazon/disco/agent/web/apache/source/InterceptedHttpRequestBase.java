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
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHeader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class InterceptedHttpRequestBase extends HttpRequestBase {

    private HashMap<String, BasicHeader> headers = new HashMap<>();

    @Override
    public String getMethod() {
        return ApacheTestConstants.METHOD;
    }

    @Override
    public URI getURI() {
        try {
            return new URI(ApacheTestConstants.URI);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public void removeHeaders(String name) {
        headers.remove(name);
    }

    @Override
    public void addHeader(String name, String value) {
        headers.put(name, new BasicHeader(name, value));
    }

    @Override
    public Header getFirstHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Header[] getHeaders(String name) {
        return new Header[]{headers.get(name)};
    }

    @Override
    public Header[] getAllHeaders() {
        return headers.values().toArray(new BasicHeader[0]);
    }
}