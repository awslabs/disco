package software.amazon.disco.agent.awsv2;

import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.disco.agent.event.AwsServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.AwsServiceDownstreamRequestEventImpl;
import software.amazon.disco.agent.event.AwsServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.AwsServiceDownstreamResponseEventImpl;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.agent.reflect.concurrent.TransactionContext;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the ExecutionInterceptor interface provided by the AWS SDK, which is the officially
 * recommended way of intercepting and manipulating AWS SDK requests. In our implementation, we publish Disco
 * Events before and after the execution, including if there was an error. We also use provided hooks to allow
 * Disco consumers to manipulate request headers, and add metadata to events like number of retries.
 *
 *  On a successful call the ordering of method calls is:
 *  beforeExecution to modifyHttpRequest to beforeTransmission to afterExecution
 *
 * On an unsuccessful execution, the ordering method calls is:
 * beforeExecution - ... -- onExecutionFailure
 * Meaning that it could fail on any portion of the call chain.
 *
 */
public class DiscoExecutionInterceptor implements ExecutionInterceptor {
    private static final Logger log = LogManager.getLogger(DiscoExecutionInterceptor.class);

    /**
     * Common header keys to retrieve the request Id
     */
    private static final List<String> REQUEST_ID_KEYS = Arrays.asList("x-amz-request-id", "x-amzn-requestid");

    /**
     * Disco Origin. Visible for testing.
     */
    static final String AWS_SDK_V2_CLIENT_ORIGIN = "AWSv2";

    /**
     * The AWS Region Execution Attribute, used to look up what region this request is being made in. We acquire it
     * reflectively because it's defined in the "aws-core" library, which we don't compile against because it's
     * possible that customers could be using this class without having "aws-core" on their classpath.
     */
    private static ExecutionAttribute<Object> regionExecutionAttribute;

    /**
     * Transaction Context keys to retrieve the request event and retry counts since these events could happen
     * between different threads on the same transaction.
     * Visible for testing
     */
    static final String TX_REQUEST_EVENT_KEY = "AWSv2RequestEvent";
    static final String TX_RETRY_COUNT_KEY = "AWSv2RetryCount";

    static {
        try {
            Class clazz = Class.forName("software.amazon.awssdk.awscore.AwsExecutionAttribute", true, ClassLoader.getSystemClassLoader());
            Field field = clazz.getField("AWS_REGION");
            regionExecutionAttribute = (ExecutionAttribute<Object>) field.get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            log.debug("Failed to access AwsExecutionAttribute.AWS_REGION field, region will not be recorded in AwsServiceEvents");
        }
    }

    /**
     * The first API to be called by the execution interceptor chain. This is called before the request is modified by
     * other interceptors.
     *
     * @param context The Context object passed in by the execution interceptor.
     *                This changes as we progress through different method calls.
     * @param executionAttributes The execution attributes which contain information such as region, service name, etc.
     */
    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        // getAttribute returns an arbitrary object. For the service name and operation name, they are returned as Strings.
        String serviceName = executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME);
        String operationName = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);

        // For the region, getAttribute returns a Region object, hence why we need to call toString().
        String region = null;
        if (regionExecutionAttribute != null) {
            region = executionAttributes.getAttribute(regionExecutionAttribute).toString();
        }

        AwsServiceDownstreamRequestEvent awsEvent = new AwsServiceDownstreamRequestEventImpl(AWS_SDK_V2_CLIENT_ORIGIN, serviceName, operationName)
                .withRegion(region);

        awsEvent.withRequest(context.request());
        TransactionContext.putMetadata(TX_REQUEST_EVENT_KEY, awsEvent);
    }

    /**
     * This modifies the Http request object before it is transmitted. The modified Http request must be returned
     *
     * @param context The Context object passed in by the execution interceptor.
     *                This changes as we progress through different method calls.
     * @param executionAttributes The execution attributes which contain information such as region, service name, etc.
     * @return the modified Http request
     */
    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        AwsServiceDownstreamRequestEventImpl requestEvent = (AwsServiceDownstreamRequestEventImpl) TransactionContext.getMetadata(TX_REQUEST_EVENT_KEY);

        requestEvent.withSdkHttpRequest(context.httpRequest())
                .withHeaderMap(context.httpRequest().headers());

        // Modification may happen by consumers of the request event
        EventBus.publish(requestEvent);

        return context.httpRequest();
    }

    /**
     * This is called before every API attempt. We use this to keep track of how many API attempts are made
     * @param context The Context object passed in by the execution interceptor.
     *                This changes as we progress through different method calls.
     * @param executionAttributes The execution attributes which contain information such as region, service name, etc.
     */
    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        Object retryCountObj = TransactionContext.getMetadata(TX_RETRY_COUNT_KEY);
        int retryCount;
        if (retryCountObj == null) {
            retryCount = 0;
        } else {
            retryCount = (int) retryCountObj + 1;
        }

        TransactionContext.putMetadata(TX_RETRY_COUNT_KEY, retryCount);
    }

    /**
     * This is called after it has been potentially modified by other request interceptors before it is sent to the service.
     * @param context The Context object passed in by the execution interceptor.
     *                This changes as we progress through different method calls.
     * @param executionAttributes The execution attributes which contain information such as region, service name, etc.
     */
    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        AwsServiceDownstreamRequestEvent requestEvent = (AwsServiceDownstreamRequestEvent) TransactionContext.getMetadata(TX_REQUEST_EVENT_KEY);

        Object txRetryCount = TransactionContext.getMetadata(TX_RETRY_COUNT_KEY);
        int retryCount = txRetryCount == null ? 0 : (int) txRetryCount;

        SdkHttpResponse httpResponse = context.httpResponse();
        AwsServiceDownstreamResponseEvent awsEvent = new AwsServiceDownstreamResponseEventImpl(requestEvent)
                .withSdkHttpResponse(httpResponse)
                .withHeaderMap(httpResponse.headers())
                .withRequestId(extractRequestId(httpResponse))
                .withRetryCount(retryCount);

        // Populate AWS SDK response
        awsEvent.withResponse(context.response());
        EventBus.publish(awsEvent);
    }

    /**
     * This is called on failure during any point of the lifecycle of the request.
     * @param context The Context object passed in by the execution interceptor.
     *                This changes as we progress through different method calls.
     *                At this point, it's a failed execution context object.
     * @param executionAttributes The execution attributes which contain information such as region, service name, etc.
     */
    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        AwsServiceDownstreamRequestEvent requestEvent = (AwsServiceDownstreamRequestEvent) TransactionContext.getMetadata(TX_REQUEST_EVENT_KEY);

        Object txRetryCount = TransactionContext.getMetadata(TX_RETRY_COUNT_KEY);
        int retryCount = txRetryCount == null ? 0 : (int) txRetryCount;

        AwsServiceDownstreamResponseEvent awsEvent = new AwsServiceDownstreamResponseEventImpl(requestEvent)
                .withSdkHttpResponse(context.httpResponse().orElse(null))
                .withHeaderMap(context.httpResponse().map(SdkHttpResponse::headers).orElse(CollectionUtils.unmodifiableMapOfLists(new HashMap<>())))
                .withRequestId(context.httpResponse().map(this::extractRequestId).orElse(null))
                .withRetryCount(retryCount);
        awsEvent.withThrown(context.exception());
        EventBus.publish(awsEvent);
    }

    /**
     * Helper method for extracting the request ID from the HTTP Response.
     * @param httpResponse The HTTP Response object with headers which contain the request ID
     * @return The request ID or null if we failed to find it.
     */
    private String extractRequestId(SdkHttpResponse httpResponse) {
        Map<String, List<String>> headerMap = httpResponse.headers();
        if (headerMap == null) return null;

        for(String request_id_key : REQUEST_ID_KEYS) {
            List<String> requestIdList = headerMap.get(request_id_key);
            if (requestIdList != null && requestIdList.size() > 0) {
                return requestIdList.get(0); // Arbitrarily get the first one since headers are one to many.
            }
        }
        return null;
    }
}
