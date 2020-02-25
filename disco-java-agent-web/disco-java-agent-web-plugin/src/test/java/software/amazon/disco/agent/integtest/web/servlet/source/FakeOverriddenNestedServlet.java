package software.amazon.disco.agent.integtest.web.servlet.source;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FakeOverriddenNestedServlet extends FakeOverriddenServlet {
    public void setThrow() {
        toThrow = true;
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }
}
