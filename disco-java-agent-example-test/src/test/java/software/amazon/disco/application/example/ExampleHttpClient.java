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

package software.amazon.disco.application.example;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class ExampleHttpClient implements HttpClient {
    public int executeCallChainDepth = 0;
    public HttpResponse fakeResponse = new BasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpParams getParams() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientConnectionManager getConnectionManager() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse execute(final HttpUriRequest request) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return this.execute(request, (HttpContext)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse execute(final HttpUriRequest request, final HttpContext context) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return this.execute(null, request, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse execute(final HttpHost target, final HttpRequest request) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return this.execute(target, request, (HttpContext)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse execute(final HttpHost target, final HttpRequest request, final HttpContext context) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return actualExecute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return this.execute((HttpUriRequest)request, responseHandler, (HttpContext)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execute(final HttpUriRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return this.execute(null, request, responseHandler, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return this.execute(target, request, responseHandler, (HttpContext)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execute(final HttpHost target, final HttpRequest request, final ResponseHandler<? extends T> responseHandler, final HttpContext context) throws IOException, ClientProtocolException {
        executeCallChainDepth++;
        return responseHandler.handleResponse(this.execute(target, request, context));
    }

    /**
     * Actual control of this fake thing. It can either return proper {@link HttpResponse} or throw an exception.
     */
    public HttpResponse actualExecute() throws IOException {
        return fakeResponse;
    }
}
