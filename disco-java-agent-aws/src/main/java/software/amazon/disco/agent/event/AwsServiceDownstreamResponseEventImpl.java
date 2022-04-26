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


package software.amazon.disco.agent.event;

import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static software.amazon.disco.agent.event.AwsServiceDownstreamEvent.DataKey.HEADER_MAP;

public class AwsServiceDownstreamResponseEventImpl extends AwsServiceDownstreamResponseEvent {
    private SdkHttpResponse sdkHttpResponse;

    /**
     * Construct a new AwsServiceDownstreamRequestEventImpl
     * @param requestEvent the associated request event
     */
    public AwsServiceDownstreamResponseEventImpl(AwsServiceDownstreamRequestEvent requestEvent) {
        super(requestEvent.getOrigin(), requestEvent.getService(), requestEvent.getOperation(), requestEvent);
    }

    /**
     * Set the SdkHttpResponse for the event. This is used to retrieve the Http specific metadata
     * such as the header map and status code.
     * @param sdkHttpResponse the SdkHttpResponse
     * @return 'this' for method chaining
     */
    public AwsServiceDownstreamResponseEventImpl withSdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
        this.sdkHttpResponse = sdkHttpResponse;
        return this;
    }

    /**
     * Set the HTTP method in this event
     * @param requestId the method e.g. GET, POST etc
     * @return 'this' for method chaining
     */
    public AwsServiceDownstreamResponseEventImpl withRequestId(String requestId) {
        withData(DataKey.REQUEST_ID.name(), requestId);
        return this;
    }

    /**
     * Set the underlying Header Map which contains the underlying Http Header that's going out to the service.
     *
     * @param headerMap The header map to add
     * @return 'this' for method chaining
     */
    public AwsServiceDownstreamResponseEventImpl withHeaderMap(Map<String, List<String>> headerMap) {
        this.withData(HEADER_MAP.name(), headerMap);
        return this;
    }

    /**
     * Set the retry count for the event. If none is set, retrieval would default to 0.
     * @param retryCount The amount of retries taken
     * @return 'this' for method chaining
     */
    public AwsServiceDownstreamResponseEventImpl withRetryCount(int retryCount) {
        this.withData(DataKey.RETRIES.name(), retryCount);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getStatusCode() {
        if (sdkHttpResponse == null) return -1;

        return sdkHttpResponse.statusCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueForField(String fieldName, Class clazz) {
        if (!(this.getResponse() instanceof SdkResponse)) {
            return Optional.empty(); // Response accessor could be null if the sdk call failed.
        }

        SdkResponse response = (SdkResponse) this.getResponse();
        return response.getValueForField(fieldName, clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFirstHeader(String key) {
        List<String> headers = getHeaders(key);
        return headers !=null && !headers.isEmpty() ? headers.get(0) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getHeaders(String key) {
        return getAllHeaders().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getAllHeaders() {
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headerMap = (Map<String, List<String>>) getData(HEADER_MAP.name());
        return headerMap;
    }
}
