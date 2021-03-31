package software.amazon.disco.agent.sql;

import com.mysql.cj.jdbc.ConnectionImpl;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.Listener;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionInterceptorTest {
    private static final String SQL = "sql";
    private static final String DB_NAME = "myDb";

    private ConnectionInterceptor interceptor;
    private TestListener listener;


    @Mock
    ConnectionImpl mockConnection;

    @Mock
    PreparedStatement mockPreparedStatement;

    @Before
    public void setup() throws SQLException {
        interceptor = new ConnectionInterceptor();
        listener = new TestListener();
        EventBus.addListener(listener);

        when(mockConnection.getCatalog()).thenReturn(DB_NAME);
    }

    @Test
    public void testInstallation() {
        AgentBuilder agentBuilder = mock(AgentBuilder.class);
        AgentBuilder.Identified.Extendable extendable = mock(AgentBuilder.Identified.Extendable.class);
        AgentBuilder.Identified.Narrowable narrowable = mock(AgentBuilder.Identified.Narrowable.class);
        when(agentBuilder.type(any(ElementMatcher.class))).thenReturn(narrowable);
        when(narrowable.transform(any(AgentBuilder.Transformer.class))).thenReturn(extendable);
        AgentBuilder result = interceptor.install(agentBuilder);
        assertSame(extendable, result);
    }

    @Test
    public void testClassMatchesCorrectly() {
        assertTrue(classMatches(ConnectionImpl.class));

        assertFalse(classMatches(Connection.class));
        assertFalse(classMatches(ConnectionInterceptorTest.class));
    }

    @Test
    public void testMethodsMatch() throws NoSuchMethodException {
        assertEquals(3, methodMatchedCount("prepareCall", Connection.class));
        assertEquals(6, methodMatchedCount("prepareStatement", Connection.class));
        assertEquals(0, methodMatchedCount("createStatement", Connection.class));
    }

    @Test
    public void testRequestEventPublished() {
        ServiceRequestEvent requestEvent = ConnectionInterceptor.enter(SQL, ConnectionInterceptor.SQL_PREPARE_ORIGIN, mockConnection);

        assertEquals(ConnectionInterceptor.SQL_PREPARE_ORIGIN, requestEvent.getOrigin());
        assertEquals(DB_NAME, requestEvent.getService());
        assertEquals(SQL, requestEvent.getOperation());
        assertEquals(mockConnection, requestEvent.getRequest());
        assertEquals(requestEvent, listener.getRequestEvent());
    }

    @Test
    public void testResponseEventPublished() {
        ServiceRequestEvent requestEvent = ConnectionInterceptor.enter(SQL, ConnectionInterceptor.SQL_PREPARE_ORIGIN, mockConnection);
        ConnectionInterceptor.exit(requestEvent, mockPreparedStatement, null);

        ServiceResponseEvent responseEvent = listener.getResponseEvent();
        assertEquals(requestEvent, listener.getRequestEvent());
        assertNotNull(responseEvent);
        assertEquals(requestEvent.getService(), responseEvent.getService());
        assertEquals(requestEvent.getOrigin(), responseEvent.getOrigin());
        assertEquals(requestEvent.getOperation(), responseEvent.getOperation());
        assertEquals(requestEvent, responseEvent.getRequest());
        assertEquals(mockPreparedStatement, responseEvent.getResponse());
        assertNull(responseEvent.getThrown());
    }

    @Test
    public void testResponseEventCapturesException() {
        SQLException ex = new SQLException("some error");
        ServiceRequestEvent requestEvent = ConnectionInterceptor.enter(SQL, ConnectionInterceptor.SQL_PREPARE_ORIGIN, mockConnection);
        ConnectionInterceptor.exit(requestEvent, null, ex);

        ServiceResponseEvent responseEvent = listener.getResponseEvent();
        assertNotNull(responseEvent);
        assertNull(responseEvent.getResponse());
        assertEquals(ex, responseEvent.getThrown());
    }

    private boolean classMatches(Class clazz) {
        return ConnectionInterceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
    }

    /**
     * Helper method to test the method matcher against an input class
     *
     * @param methodName name of method
     * @param paramType class we are verifying contains the method
     * @return Matched methods count
     * @throws NoSuchMethodException
     */
    private int methodMatchedCount(String methodName, Class paramType) throws NoSuchMethodException {
        List<Method> methods = new ArrayList<>();
        for (Method m : paramType.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                methods.add(m);
            }
        }

        if (methods.size() == 0) throw new NoSuchMethodException();

        int matchedCount = 0;
        for (Method m : methods) {
            MethodDescription.ForLoadedMethod forLoadedMethod = new MethodDescription.ForLoadedMethod(m);
            if (ConnectionInterceptor.buildMethodMatcher().matches(forLoadedMethod)) {
                matchedCount++;
            }
        }
        return matchedCount;
    }

    private static class TestListener implements Listener {
        ServiceRequestEvent getRequestEvent() {
            return requestEvent;
        }
        ServiceResponseEvent getResponseEvent() {
            return responseEvent;
        }

        private ServiceRequestEvent requestEvent;
        private ServiceResponseEvent responseEvent;

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event event) {
            if (event instanceof ServiceRequestEvent) {
                requestEvent = (ServiceRequestEvent) event;
            } else if (event instanceof ServiceResponseEvent) {
                responseEvent = (ServiceResponseEvent) event;
            } else {
                throw new IllegalArgumentException("Unexpected event");
            }
        }
    }
}
