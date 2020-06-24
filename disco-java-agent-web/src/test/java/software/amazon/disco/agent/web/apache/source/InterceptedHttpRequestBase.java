package software.amazon.disco.agent.web.apache.source;

import org.apache.http.client.methods.HttpRequestBase;
import software.amazon.disco.agent.web.apache.utils.HttpRequestAccessor;
import software.amazon.disco.agent.web.apache.utils.HttpRequestBaseAccessor;

public class InterceptedHttpRequestBase extends HttpRequestBase implements HttpRequestBaseAccessor, HttpRequestAccessor {

        @Override
        public String getMethod() {
            return ApacheTestConstants.METHOD;
        }

        @Override
        public String getUri() {
            return ApacheTestConstants.URI;
        }

        @Override
        public String getMethodFromRequestLine() {
            return null;
        }

        @Override
        public String getUriFromRequestLine() {
            return null;
        }

    }
