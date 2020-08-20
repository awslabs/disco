package software.amazon.disco.agent.integtest.awsv1;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.HeaderReplaceable;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceEvent;
import software.amazon.disco.agent.reflect.event.EventBus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class AWSV1InterceptionTests {
    private static final int PORT = 8089;
    private static final String ENDPOINT = "http://127.0.0.1:" + PORT;
    private static final String REGION = "us-west-2";
    private static final String HEADER_KEY = "someKey";
    private static final String HEADER_VAL = "someVal";

    private TestListener testListener;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    /**
     * API info for DDB requests from: https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ListTables.html
     */
    @Before
    public void setup() {
        testListener = new TestListener();
        EventBus.addListener(testListener);

        // Stub out fake response for ListTables, the "default" operation for these tests
        String result = "{\"TableNames\":[\"ATestTable\",\"dynamodb-user\",\"scorekeep-state\",\"scorekeep-user\"]}";
        stubFor(post(urlEqualTo("/"))
                .withHeader("X-Amz-Target", containing("ListTables"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(result)));
    }

    @After
    public void cleanup() {
        EventBus.removeAllListeners();
    }

    @Test
    public void testNormalRequestIntercepted() {
        AmazonDynamoDB client = (AmazonDynamoDB) getTestableClient(AmazonDynamoDBClientBuilder.standard()).build();
        ListTablesRequest listTablesRequest = new ListTablesRequest();
        client.listTables(listTablesRequest);

        // Verify the HTTP request to AWS was made
        verify(postRequestedFor(urlEqualTo("/")).withHeader("X-Amz-Target", containing("ListTables")));

        Assert.assertNotNull(testListener.requestEvent);
        verifyEvent(testListener.requestEvent, "AmazonDynamoDBv2", "ListTables");
        Assert.assertTrue(testListener.requestEvent.getRequest() instanceof Request);

        Assert.assertNotNull(testListener.responseEvent);
        verifyEvent(testListener.responseEvent, "AmazonDynamoDBv2", "ListTables");
        Assert.assertTrue(testListener.responseEvent.getResponse() instanceof Response);
        Assert.assertNull(testListener.responseEvent.getThrown());
    }

    // TODO: Add integration test for S3 once supported

    @Test
    public void testReplaceHeaders() {
        EventBus.addListener(new HeaderListener());

        AmazonDynamoDB client = (AmazonDynamoDB) getTestableClient(AmazonDynamoDBClientBuilder.standard()).build();
        client.listTables();

        // Verify the HTTP request to AWS was made with our injected header
        verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("X-Amz-Target", containing("ListTables"))
                .withHeader(HEADER_KEY, equalTo(HEADER_VAL)));
    }

    @Test
    public void testAwsServiceException() {
        stubFor(post(urlEqualTo("/"))
                .withHeader("X-Amz-Target", containing("DescribeTable"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("A service error occurred")));

        AmazonDynamoDB client = (AmazonDynamoDB) getTestableClient(AmazonDynamoDBClientBuilder.standard()).build();
        DescribeTableRequest describeTableRequest = new DescribeTableRequest();
        AmazonDynamoDBException exception = null;

        try {
            client.describeTable(describeTableRequest);
            Assert.fail();  // Request should throw exception
        } catch (AmazonDynamoDBException e) {
            exception = e;
        }

        // Verify the HTTP request to AWS was made
        verify(postRequestedFor(urlEqualTo("/")).withHeader("X-Amz-Target", containing("DescribeTable")));

        Assert.assertNotNull(testListener.requestEvent);
        verifyEvent(testListener.requestEvent, "AmazonDynamoDBv2", "DescribeTable");
        Assert.assertTrue(testListener.requestEvent.getRequest() instanceof Request);

        Assert.assertNotNull(testListener.responseEvent);
        verifyEvent(testListener.responseEvent, "AmazonDynamoDBv2", "DescribeTable");
        Assert.assertNull(testListener.responseEvent.getResponse());
        Assert.assertNotNull(exception);
        Assert.assertEquals(exception, testListener.responseEvent.getThrown());
    }

    @Test
    public void testAwsClientException() {
        // Override endpoint from WireMock to fake endpoint, causing a SDK Client exception
        AmazonDynamoDB client = (AmazonDynamoDB) getTestableClient(AmazonDynamoDBClientBuilder.standard())
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://some.nonexistent.endpoint.com/path/to/page", "us-west-2"))
                .build();
        SdkClientException exception = null;

        try {
            client.listTables();
            Assert.fail();  // SDK should throw connection refused exception
        } catch (SdkClientException e) {
            exception = e;
        }

        Assert.assertNotNull(testListener.requestEvent);
        verifyEvent(testListener.requestEvent, "AmazonDynamoDBv2", "ListTables");
        Assert.assertTrue(testListener.requestEvent.getRequest() instanceof Request);

        Assert.assertNotNull(testListener.responseEvent);
        verifyEvent(testListener.responseEvent, "AmazonDynamoDBv2", "ListTables");
        Assert.assertNull(testListener.responseEvent.getResponse());
        Assert.assertNotNull(exception);
        Assert.assertEquals(exception, testListener.responseEvent.getThrown());
    }

    private AwsClientBuilder getTestableClient(AwsClientBuilder builder) {
        AWSCredentialsProvider fakeCredentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials("fake", "fake"));
        AwsClientBuilder.EndpointConfiguration mockEndpoint = new AwsClientBuilder.EndpointConfiguration(ENDPOINT, REGION);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        clientConfiguration.setRequestTimeout(1000);

        return builder
                .withEndpointConfiguration(mockEndpoint)
                .withCredentials(fakeCredentials)
                .withClientConfiguration(clientConfiguration);
    }

    private void verifyEvent(ServiceEvent event, String service, String operation) {
        Assert.assertEquals("AWSv1", event.getOrigin());
        Assert.assertEquals(service, event.getService());
        Assert.assertEquals(operation, event.getOperation());
    }

    private static class TestListener implements Listener {
        ServiceDownstreamRequestEvent requestEvent;
        ServiceDownstreamResponseEvent responseEvent;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ServiceDownstreamRequestEvent) {
                requestEvent = (ServiceDownstreamRequestEvent) e;
            } else if (e instanceof ServiceDownstreamResponseEvent) {
                responseEvent = (ServiceDownstreamResponseEvent) e;
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
