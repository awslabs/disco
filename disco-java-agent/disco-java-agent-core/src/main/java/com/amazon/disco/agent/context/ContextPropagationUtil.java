package com.amazon.disco.agent.context;

import com.amazon.disco.agent.concurrent.TransactionContext;
import com.amazon.disco.agent.logging.LogManager;
import com.amazon.disco.agent.logging.Logger;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A helper/utility class that provides the mechanisms for packing and unpacking
 * various key value pairs to and from a header attribute. Specifically,
 * TransactionContext attributes which have been marked for propagation.
 * Allowing the attributes to be placed in a single outgoing http attribute and
 * then placed back into the transaction context by the downstream service.
 * <p>
 * Data Format
 * <p>
 * The packed header attribute value contains a series of key-value pairs:
 * <p>
 * key3=value3|key3=value3|key3=value3 [...]
 * <p>
 * Where each key and value represents an entry in the TransactionContext that
 * was marked to be propagated.
 * <p>
 * For serialization the above string is base 64 encoded
 */
public class ContextPropagationUtil {
    private final static Logger log = LogManager.getLogger(ContextPropagationUtil.class);
    private final static String PROPAGATE_IN_REQUEST_TAG = "PROPAGATE_IN_REQUEST";
    private static final String DELIMITER_KEY = "=";
    private static final String DELIMITER_PAIR = "&";
    private static final String UTF_8 = "UTF-8";
    public static final String CONTEXT_PROPAGATION_HEADER = "x-amzn-ctxt-prop-data";
    public static final Set<String> EMPTY_WHITELIST = new HashSet<String>();


    public static Map<String, Object> getWhiteListAttributesFromContext(Set<String> whitelist) {
        Map<String, Object> keyValuesToPropagate = TransactionContext.getMetadataWithTag(PROPAGATE_IN_REQUEST_TAG);
        keyValuesToPropagate.keySet().retainAll(whitelist);
        return keyValuesToPropagate;
    }


    /**
     * This method serializes the attributes to be propagated from the AlphaOne
     * TransactionContext and packs them into a single attribute string. This
     * string can be sent out as a HTTP attribute.  The downstream can then
     * unpack the attributes, inserting them back into the TransactionContext.
     *
     * @return A URL encoded string of attributes to be propagated.
     */
    public static String packPropagationAttributeForHeaderFromContext() {
        return packPropagationAttributeForHeaderFromContext(EMPTY_WHITELIST);
    }

    /**
     * This method serializes the attributes to be propagated from the AlphaOne
     * TransactionContext and packs them into a single attribute string. This
     * string can be sent out as a HTTP attribute.  The downstream can then
     * unpack the attributes, inserting them back into the TransactionContext.
     *
     * @param whitelist a list of attribute keys to be excluded from the packed header attribute
     *                  as they have been whitelisted to be sent as individual attributes.
     *
     * @return A URL encoded string of attributes to be propagated.
     */
    public static String packPropagationAttributeForHeaderFromContext(Set<String> whitelist) {
        Map<String, Object> keyValuesToPropagate = TransactionContext.getMetadataWithTag(PROPAGATE_IN_REQUEST_TAG);
        keyValuesToPropagate.keySet().removeAll(whitelist);
        if (!keyValuesToPropagate.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> attribute : keyValuesToPropagate.entrySet()) {
                if (sb.length() > 0)
                    sb.append(DELIMITER_PAIR);
                try {
                    sb.append(URLEncoder.encode(attribute.getKey(), UTF_8));
                    sb.append(DELIMITER_KEY);
                    sb.append(URLEncoder.encode((String) attribute.getValue(), UTF_8));
                }
                catch (UnsupportedEncodingException e) {
                    log.error("AlphaOne(ContextPropagationUtil) threw exception" + e);
                }
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * This method deserializes the packed propagation attributes from
     * the http header of an upstream request into the TransactionContext.
     *
     * @param packedHeader The string value of the header attribute used for
     *                     propagation.
     *                     <p>
     *                     Each key value it decodes is inserted into the TransactionContext.
     */
    public static void unpackPropagationHeaderIntoContext(String packedHeader) {
        if (packedHeader.isEmpty()) {
            return;
        }
        String[] pairs = packedHeader.split(DELIMITER_PAIR);
        for (String pair : pairs) {
            String[] keyValue = pair.split(DELIMITER_KEY);
            try {
                TransactionContext.putMetadata(URLDecoder.decode(keyValue[0], UTF_8), (Object) URLDecoder.decode(keyValue[1], UTF_8));
            }
            catch (UnsupportedEncodingException e) {
                log.error("AlphaOne(ContextPropagationUtil) threw exception" + e);
            }
        }
    }
}
