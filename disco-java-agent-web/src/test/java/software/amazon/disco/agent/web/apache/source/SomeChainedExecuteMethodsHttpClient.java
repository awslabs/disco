/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class SomeChainedExecuteMethodsHttpClient implements HttpClient {
        int executeMethodChainingDepth = 0;
        private HttpResponse expectedResponse;
        private IOException expectedIOException;
        /**
         * Doesn't matter in this test suite.
         * Just a thing required as part of the HttpClient implementation
         */
        @Override
        public HttpParams getParams() {
            return null;
        }

        /**
         * Doesn't matter in this test suite.
         * Just a thing required as part of the HttpClient implementation
         */
        @Override
        public ClientConnectionManager getConnectionManager() {
            return null;
        }

        /**
         * Target method - chained.
         */
        @Override
        public HttpResponse execute(final HttpUriRequest request) throws IOException {
            executeMethodChainingDepth++;
            return this.execute(request, (HttpContext)null);
        }

        /**
         * Target method - chained.
         */
        @Override
        public HttpResponse execute(final HttpUriRequest request, final HttpContext context) throws IOException {
            executeMethodChainingDepth++;
            return this.execute(null, request, context);
        }

        /**
         * Target method - chained.
         */
        @Override
        public HttpResponse execute(final HttpHost target, final HttpRequest request) throws IOException {
            executeMethodChainingDepth++;
            return this.execute(target, request, (HttpContext)null);
        }

        /**
         * Target method - chained.
         * This method returns {@link HttpResponse}
         */
        @Override
        public HttpResponse execute(final HttpHost target, final HttpRequest request, final HttpContext context) {
            executeMethodChainingDepth++;
            return expectedResponse;
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
            executeMethodChainingDepth++;
            return this.execute((HttpUriRequest)request, responseHandler, (HttpContext)null);
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
            executeMethodChainingDepth++;
            return this.execute(null, request, responseHandler, context);
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException {
            executeMethodChainingDepth++;
            return this.execute(target, request, responseHandler, (HttpContext)null);
        }

        /**
         * Target method - chained.
         */
        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException {
            executeMethodChainingDepth++;
            return responseHandler.handleResponse(this.execute(target, request, context));
        }

        /**
         * Non-target method - should not be intercepted.
         * This method has the right method name, but doesn't have any argument whose super type is HttpRequest
         */
        public HttpResponse execute(String msg) {
            executeMethodChainingDepth++;
            return null;
        }

        /**
         * Target method - has an argument whose super type is HttpRequest.
         */
        public HttpResponse execute(SomeClassSuperTypeIsHttpRequest request, String msg) {
            executeMethodChainingDepth++;
            return null;
        }

        /**
         * Target method - has an argument whose super type is HttpRequest, but different order.
         */
        public HttpResponse execute(String msg, SomeClassSuperTypeIsHttpRequest request) {
            executeMethodChainingDepth++;
            return null;
        }

        /**
         * Target method - has an argument whose super type is HttpRequest,
         * throwing {@link IOException}.
         */
        public HttpResponse execute(InterceptedHttpRequestBase request) throws IOException {
            executeMethodChainingDepth++;
            throw expectedIOException;
        }

       public int getExecuteMethodChainingDepth() {
          return executeMethodChainingDepth;
        }

        public void setExecuteMethodChainingDepth(int executeMethodChainingDepth) {
         this.executeMethodChainingDepth = executeMethodChainingDepth;
        }

    public HttpResponse getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(HttpResponse expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public IOException getExpectedIOException() {
        return expectedIOException;
    }

    public void setExpectedIOException(IOException expectedIOException) {
        this.expectedIOException = expectedIOException;
    }

    abstract class SomeClassSuperTypeIsHttpRequest implements HttpRequest { }
    }


