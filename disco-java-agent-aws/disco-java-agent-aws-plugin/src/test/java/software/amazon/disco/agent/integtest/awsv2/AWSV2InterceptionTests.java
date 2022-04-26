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

package software.amazon.disco.agent.integtest.awsv2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.disco.agent.event.AwsServiceDownstreamEvent;
import software.amazon.disco.agent.event.AwsServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.AwsServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.HeaderReplaceable;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

public class AWSV2InterceptionTests {
    private static final int PORT = 8089;
    private static final String ENDPOINT = "http://127.0.0.1:" + PORT;
    private static final String REQUEST_ID = "12345";
    private static final String HEADER_KEY = "someKey";
    private static final String HEADER_VAL = "someVal";
    private static final String S3_BUCKET = "myBucket";
    private static final String S3_KEY = "myKey";

    private TestListener testListener;

    private static final String LIST_TABLES = "{\"TableNames\":[\"ATestTable\",\"dynamodb-user\",\"scorekeep-state\",\"scorekeep-user\"]}";

    /**
     * API info from:
     * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ListTables.html
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html
     */
    @Before
    public void setup() {
        testListener = new TestListener();
        EventBus.addListener(testListener);

        // Stub out fake response for ListTables, the "default" operation for these tests
        stubFor(post(urlEqualTo("/"))
                .withHeader("X-Amz-Target", containing("ListTables"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("x-amz-request-id", REQUEST_ID)
                        .withBody(LIST_TABLES)));

        // Stub out fake response for List S3 buckets
        stubFor(put(urlEqualTo("/" + S3_BUCKET + "/" + S3_KEY))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("x-amz-request-id", REQUEST_ID)
                    .withBody("")));  // putObject returns no body
    }

    @After
    public void cleanup() {
        EventBus.removeAllListeners();
        TransactionContext.clear();
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    @Test
    public void testNormalRequestInterception() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
            .region(Region.US_WEST_2);
        DynamoDbClient client = (DynamoDbClient) getTestableClient(builder).build();

        client.listTables();

        // Verify HTTP request was actually made
        verify(postRequestedFor(urlEqualTo("/")).withHeader("X-Amz-Target", containing("ListTables")));

        Assert.assertNotNull(testListener.requestEvent);
        verifyEvent(testListener.requestEvent, "DynamoDb", "ListTables");
        Assert.assertEquals(Region.US_WEST_2.toString(), testListener.requestEvent.getRegion());
        Assert.assertTrue(testListener.requestEvent.getRequest() instanceof SdkRequest);

        Assert.assertNotNull(testListener.responseEvent);
        verifyEvent(testListener.responseEvent, "DynamoDb", "ListTables");
        Assert.assertEquals(200, testListener.responseEvent.getStatusCode());
        Assert.assertEquals(REQUEST_ID, testListener.responseEvent.getRequestId());
        Assert.assertEquals(0, testListener.responseEvent.getRetryCount());
        Assert.assertTrue(testListener.responseEvent.getResponse() instanceof SdkResponse);
        Assert.assertNull(testListener.responseEvent.getThrown());
    }

    @Test
    public void testS3RequestInterception() throws URISyntaxException {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
                .region(Region.US_WEST_2);
        S3Client s3Client = (S3Client) getTestableClient(builder).build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket("myBucket")
                .key("myKey")
                .build();

        Path filePath = Paths.get(AWSV2InterceptionTests.class.getResource("/software/amazon/disco/agent/integtest/s3File.txt").toURI());

        s3Client.putObject(putObjectRequest, filePath);

        // Verify HTTP request was actually made
        verify(putRequestedFor(urlEqualTo("/" + S3_BUCKET + "/" + S3_KEY)));

        Assert.assertNotNull(testListener.requestEvent);
        verifyEvent(testListener.requestEvent, "S3", "PutObject");
        Assert.assertEquals(Region.US_WEST_2.toString(), testListener.requestEvent.getRegion());
        Assert.assertTrue(testListener.requestEvent.getRequest() instanceof SdkRequest);

        Assert.assertNotNull(testListener.responseEvent);
        verifyEvent(testListener.responseEvent, "S3", "PutObject");
        Assert.assertEquals(200, testListener.responseEvent.getStatusCode());
        Assert.assertEquals(REQUEST_ID, testListener.responseEvent.getRequestId());
        Assert.assertTrue(testListener.responseEvent.getResponse() instanceof SdkResponse);
        Assert.assertNull(testListener.responseEvent.getThrown());
        Assert.assertEquals(0, testListener.responseEvent.getRetryCount());
    }

    @Test
    public void testReplaceHeader() {
        EventBus.addListener(new HeaderListener());
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
                .region(Region.US_WEST_2);
        DynamoDbClient client = (DynamoDbClient) getTestableClient(builder).build();

        client.listTables();

        verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("X-Amz-Target", containing("ListTables"))
                .withHeader(HEADER_KEY, equalTo(HEADER_VAL)));
    }

    @Test
    public void testRetriesRecorded() {
        // Set up a scenario where the service fails once then succeeds
        String RETURN_SUCCESS_STATE = "return success";
        stubFor(post(urlEqualTo("/")).inScenario("retries")
                .whenScenarioStateIs(STARTED)
                .withHeader("X-Amz-Target", containing("DescribeTable"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("A service error occurred"))
                .willSetStateTo(RETURN_SUCCESS_STATE));

        stubFor(post(urlEqualTo("/")).inScenario("retries")
                .whenScenarioStateIs(RETURN_SUCCESS_STATE)
                .withHeader("X-Amz-Target", containing("DescribeTable"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{}")));

        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
                .region(Region.US_WEST_2);
        DynamoDbClient client = (DynamoDbClient) getTestableClient(builder).build();

        DescribeTableRequest describeTableRequest = DescribeTableRequest.builder().tableName("myTable").build();
        client.describeTable(describeTableRequest);

        Assert.assertNotNull(testListener.responseEvent);
        Assert.assertTrue(testListener.responseEvent.getResponse() instanceof SdkResponse);
        Assert.assertEquals(1, testListener.responseEvent.getRetryCount());  // Exactly one retry for one failure
    }

    @Test
    public void testAwsServiceFailure() {
        stubFor(post(urlEqualTo("/"))
                .withHeader("X-Amz-Target", containing("ListTables"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("A service error occurred")));

        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
                .region(Region.US_WEST_2);
        DynamoDbClient client = (DynamoDbClient) getTestableClient(builder).build();
        DynamoDbException exception = null;

        try {
            client.listTables();
            Assert.fail();  // Should throw exception from service error
        } catch (DynamoDbException e) {
            exception = e;
        }

        Assert.assertNotNull(testListener.requestEvent);
        verifyEvent(testListener.requestEvent, "DynamoDb", "ListTables");
        Assert.assertEquals(Region.US_WEST_2.toString(), testListener.requestEvent.getRegion());
        Assert.assertTrue(testListener.requestEvent.getRequest() instanceof SdkRequest);

        Assert.assertNotNull(testListener.responseEvent);
        verifyEvent(testListener.responseEvent, "DynamoDb", "ListTables");
        Assert.assertNotNull(testListener.responseEvent.getThrown());
        Assert.assertEquals(exception, testListener.responseEvent.getThrown());
        Assert.assertNull(testListener.responseEvent.getResponse());

        // None should be set because no attempt was made
        Assert.assertEquals(500, testListener.responseEvent.getStatusCode());
        Assert.assertNull(testListener.responseEvent.getRequestId());
    }

    @Test
    public void testAwsClientException() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
                .region(Region.US_WEST_2);
        DynamoDbClient client = (DynamoDbClient) getTestableClient(builder)
                .endpointOverride(URI.create("http://some.fake.endpoint.moc/path/to/page"))
                .build();
        SdkClientException exception = null;

        try {
            client.listTables();
            Assert.fail();  // Request should throw
        } catch (SdkClientException e) {
            exception = e;
        }

        Assert.assertNotNull(testListener.requestEvent);
        verifyEvent(testListener.requestEvent, "DynamoDb", "ListTables");
        Assert.assertEquals(Region.US_WEST_2.toString(), testListener.requestEvent.getRegion());
        Assert.assertTrue(testListener.requestEvent.getRequest() instanceof SdkRequest);

        Assert.assertNotNull(testListener.responseEvent);
        verifyEvent(testListener.responseEvent, "DynamoDb", "ListTables");
        Assert.assertNotNull(testListener.responseEvent.getThrown());
        Assert.assertEquals(exception, testListener.responseEvent.getThrown());
        Assert.assertNull(testListener.responseEvent.getResponse());

        // None should be set because no attempt was made
        Assert.assertEquals(-1, testListener.responseEvent.getStatusCode());
        Assert.assertNull(testListener.responseEvent.getRequestId());
    }

    private SdkClientBuilder getTestableClient(SdkClientBuilder builder) {
        ClientOverrideConfiguration configuration = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(1))
                .build();

        return builder
                .overrideConfiguration(configuration)
                .endpointOverride(URI.create(ENDPOINT));
    }

    private void verifyEvent(AwsServiceDownstreamEvent event, String service, String operation) {
        Assert.assertEquals("AWSv2", event.getOrigin());
        Assert.assertEquals(service, event.getService());
        Assert.assertEquals(operation, event.getOperation());
    }

    private static class TestListener implements Listener {
        AwsServiceDownstreamRequestEvent requestEvent;
        AwsServiceDownstreamResponseEvent responseEvent;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof AwsServiceDownstreamRequestEvent) {
                requestEvent = (AwsServiceDownstreamRequestEvent) e;
            } else if (e instanceof AwsServiceDownstreamResponseEvent) {
                responseEvent = (AwsServiceDownstreamResponseEvent) e;
            } else {
                Assert.fail("Unexpected event");
            }
        }
    }

    private static class HeaderListener implements Listener {

        @Override
        public int getPriority() {
            return 1;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof HeaderReplaceable) {
                ((HeaderReplaceable) e).replaceHeader(HEADER_KEY, HEADER_VAL);
            }
        }
    }
}
