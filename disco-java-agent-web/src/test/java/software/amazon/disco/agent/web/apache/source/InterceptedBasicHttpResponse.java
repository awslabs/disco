package software.amazon.disco.agent.web.apache.source;

import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import software.amazon.disco.agent.web.apache.utils.HttpResponseAccessor;

public class InterceptedBasicHttpResponse extends BasicHttpResponse implements HttpResponseAccessor {

        public InterceptedBasicHttpResponse(final ProtocolVersion ver, final int code, final String reason) {
            super(ver, code, reason);
        }
        @Override
        public int getStatusCode() {
            return super.getStatusLine().getStatusCode();
        }

        @Override
        public long getContentLength() {
            return 0;
        }

    }