package software.amazon.disco.agent.web.apache.source;

import software.amazon.disco.agent.web.apache.utils.HttpRequestAccessor;

import java.util.HashMap;

public class InterceptedBasicHttpRequest implements HttpRequestAccessor {

    private HashMap<String,String> headers = new HashMap<String,String>();
    @Override
    public String getMethodFromRequestLine() {
        return ApacheTestConstants.METHOD;
    }
    @Override
    public String getUriFromRequestLine() {
        return ApacheTestConstants.URI;
    }
    @Override
    public void addHeader(String name, String value) {
        headers.put(name,value);
    }
    @Override
    public void removeHeaders(String name) {
        headers.remove(name);
    }
    public HashMap<String, String> getHeaders() {
        return headers;
    }

}
