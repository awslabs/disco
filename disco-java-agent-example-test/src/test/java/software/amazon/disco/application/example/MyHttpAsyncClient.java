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

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.concurrent.Future;

public class MyHttpAsyncClient implements HttpAsyncClient {
    public HttpResponse fakeResponse = new BasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");
    public int executeCallChainDepth = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> execute(final HttpAsyncRequestProducer requestProducer, final HttpAsyncResponseConsumer<T> responseConsumer, final HttpContext context, final FutureCallback<T> callback) {
        executeCallChainDepth++;
        return actualExecute(requestProducer, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> execute(final HttpAsyncRequestProducer requestProducer, final HttpAsyncResponseConsumer<T> responseConsumer, final FutureCallback<T> callback) {
        executeCallChainDepth++;
        return execute(requestProducer, responseConsumer, HttpClientContext.create(), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<HttpResponse> execute(final HttpHost target, final HttpRequest request, final HttpContext context, final FutureCallback<HttpResponse> callback) {
        executeCallChainDepth++;
        return execute(
                HttpAsyncMethods.create(target, request),
                HttpAsyncMethods.createConsumer(),
                context, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<HttpResponse> execute(final HttpHost target, final HttpRequest request, final FutureCallback<HttpResponse> callback) {
        executeCallChainDepth++;
        return execute(target, request, HttpClientContext.create(), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<HttpResponse> execute(final HttpUriRequest request, final HttpContext context, final FutureCallback<HttpResponse> callback) {
        executeCallChainDepth++;
        return execute(new HttpHost("http://amazon.com"), request, context, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<HttpResponse> execute(final HttpUriRequest request, final FutureCallback<HttpResponse> callback) {
        executeCallChainDepth++;
        return execute(request, HttpClientContext.create(), callback);
    }

    private <T> Future<T> actualExecute(final HttpAsyncRequestProducer requestProducer, final FutureCallback<T> callback) {
        try {
            requestProducer.generateRequest();
        } catch (IOException | HttpException e) {
            // swallow
        }

        BasicFuture<T> future = new BasicFuture<>(callback);
        future.completed((T) fakeResponse);
        return future;
    }
}
