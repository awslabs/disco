package software.amazon.disco.agent.integtest.sql;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import software.amazon.disco.agent.integtest.sql.source.MyConnectionImpl;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionInterceptorTest {
    private static final String QUERY = "SELECT * FROM my_table WHERE id=?";
    static final String DB = "MY_DB";

    private TestListener listener;
    private Connection connection;

    @Mock
    PreparedStatement mockPreparedStatement;

    @Mock
    CallableStatement mockCallableStatement;

    @Before
    public void setup() {
        listener = new TestListener();
        connection = new MyConnectionImpl(mockPreparedStatement, mockCallableStatement);
        EventBus.addListener(listener);
    }

    @Test
    public void testPrepareStatementInterception() throws SQLException {
        connection.prepareStatement(QUERY);
        verifyRequestEvent();
        verifyResponseEvent(mockPreparedStatement);
    }

    @Test
    public void testPrepareCallInterception() throws SQLException {
        connection.prepareCall(QUERY);
        verifyRequestEvent();
        verifyResponseEvent(mockCallableStatement);
    }

    @Test(expected = SQLException.class)
    public void testCaptureException() throws SQLException {
        try {
            connection.prepareStatement(QUERY, 1);  // This method has been stubbed to throw
        } finally {
            verifyRequestEvent();
            Assert.assertEquals(1, listener.responseEvents.size());
            ServiceResponseEvent event = listener.responseEvents.get(0);
            Assert.assertTrue(event.getThrown() instanceof SQLException);
            Assert.assertNull(event.getResponse());
        }
    }

    private void verifyRequestEvent() {
        Assert.assertEquals(1, listener.requestEvents.size());
        ServiceRequestEvent event = listener.requestEvents.get(0);
        Assert.assertEquals(connection, event.getRequest());
        Assert.assertEquals(QUERY, event.getOperation());
        Assert.assertEquals(DB, event.getService());
    }

    private void verifyResponseEvent(PreparedStatement response) {
        Assert.assertEquals(1, listener.responseEvents.size());
        ServiceResponseEvent event = listener.responseEvents.get(0);
        Assert.assertEquals(response, event.getResponse());
        Assert.assertNull(event.getThrown());
    }

    private static class TestListener implements Listener {
        List<ServiceRequestEvent> requestEvents = new ArrayList<>();
        List<ServiceResponseEvent> responseEvents = new ArrayList<>();

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event e) {
            if (e instanceof ServiceRequestEvent) {
                requestEvents.add((ServiceRequestEvent) e);
            } else if (e instanceof ServiceResponseEvent) {
                responseEvents.add((ServiceResponseEvent) e);
            } else {
                Assert.fail("Unexpected event");
            }
        }
    }
}
