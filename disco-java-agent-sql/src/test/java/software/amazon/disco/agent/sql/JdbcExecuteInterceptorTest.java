package software.amazon.disco.agent.sql;

import com.mysql.cj.jdbc.CallableStatement;
import com.mysql.cj.jdbc.ClientPreparedStatement;
import com.mysql.cj.jdbc.ConnectionImpl;
import com.mysql.cj.jdbc.StatementImpl;
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
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import software.amazon.disco.agent.sql.source.MyCallableStatementImpl;
import software.amazon.disco.agent.sql.source.MyPreparedStatementImpl;
import software.amazon.disco.agent.sql.source.MyStatementImpl;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JdbcExecuteInterceptorTest {
    private static final String DB_NAME = "My db";
    private static final String QUERY = "some sql";

    private Statement myStatement;
    private PreparedStatement myPreparedStatement;
    private java.sql.CallableStatement myCallableStatement;
    private JdbcExecuteInterceptor interceptor;
    private ServiceRequestEvent requestEvent;
    private MockEventBusListener mockListener;

    @Mock
    ConnectionImpl mockConnection;

    @Mock
    StatementImpl mockStatement;

    @Mock
    ClientPreparedStatement mockPreparedStatement;

    @Mock
    CallableStatement mockCallableStatement;

    @Before
    public void setup() throws SQLException {
        interceptor = new JdbcExecuteInterceptor();
        myStatement = new MyStatementImpl();
        myPreparedStatement = new MyPreparedStatementImpl();
        myCallableStatement = new MyCallableStatementImpl();
        requestEvent = new ServiceDownstreamRequestEvent(JdbcExecuteInterceptor.SQL_ORIGIN, DB_NAME, QUERY)
                .withRequest(mockStatement);
        mockListener = new MockEventBusListener();
        EventBus.addListener(mockListener);

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getCatalog()).thenReturn(DB_NAME);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareCall(anyString())).thenReturn(mockCallableStatement);
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
    public void testClassMatcherSucceedsOnRealStatements() throws SQLException {
        assertTrue(classMatches(mockConnection.createStatement().getClass()));
        assertTrue(classMatches(mockConnection.prepareCall("SOME SQL").getClass()));
        assertTrue(classMatches(mockConnection.prepareStatement("MORE SQL").getClass()));
    }

    @Test
    public void testClassMatcherSucceedsOnConcreteStatements() {
        assertTrue(classMatches(myStatement.getClass()));
        assertTrue(classMatches(myPreparedStatement.getClass()));
        assertTrue(classMatches(myCallableStatement.getClass()));
    }

    @Test
    public void testInvalidClassesDontMatch() {
        assertFalse(classMatches(String.class));
        assertFalse(classMatches(Statement.class));  // Interface
    }

    @Test
    public void testRealExecuteWithStringMatches() throws NoSuchMethodException {
        assertEquals(4, methodMatchedCount("execute", StatementImpl.class));
        assertEquals(4, methodMatchedCount("executeUpdate", StatementImpl.class));
        assertEquals(4, methodMatchedCount("executeLargeUpdate", StatementImpl.class));
        assertEquals(1, methodMatchedCount("executeQuery", StatementImpl.class));
    }

    @Test
    public void testRealExecuteWithoutStringMatches() throws NoSuchMethodException {
        assertEquals(1, methodMatchedCount("execute", ClientPreparedStatement.class));
        assertEquals(1, methodMatchedCount("executeUpdate", ClientPreparedStatement.class));
        assertEquals(1, methodMatchedCount("executeQuery", ClientPreparedStatement.class));
    }

    @Test
    public void testNonExecuteMethodDoesNotMatch() throws NoSuchMethodException {
        assertEquals(0, methodMatchedCount("addBatch", StatementImpl.class));
    }

    @Test
    public void testConcreteExecuteWithStringMatches() throws NoSuchMethodException {
        assertEquals(4, methodMatchedCount("execute", MyStatementImpl.class));
        assertEquals(4, methodMatchedCount("executeUpdate", MyStatementImpl.class));
        assertEquals(1, methodMatchedCount("executeQuery", MyStatementImpl.class));
    }

    @Test
    public void testConcreteExecuteWithoutStringMatches() throws NoSuchMethodException {
        assertEquals(1, methodMatchedCount("execute", MyPreparedStatementImpl.class));
        assertEquals(1, methodMatchedCount("executeUpdate", MyPreparedStatementImpl.class));
        assertEquals(1, methodMatchedCount("executeQuery", MyPreparedStatementImpl.class));
    }

    @Test
    public void testAbstractExecuteDoesNotMatch() throws NoSuchMethodException {
        assertEquals(0, methodMatchedCount("execute", MyStatementImpl.MyAbstractStatement.class));
    }

    @Test
    public void testQueryStringExtractedFromParams() throws NoSuchMethodException {
        Object[] params = {QUERY};
        String parsed = JdbcExecuteInterceptor.parseQueryFromStatement(myStatement, params);

        assertEquals(QUERY, parsed);
    }

    @Test
    public void testQueryStringExtractedWithToString() throws NoSuchMethodException {
        Object[] params = {};
        String parsed = JdbcExecuteInterceptor.parseQueryFromStatement(myCallableStatement, params);

        // MyCallableStatement has toString() overloaded
        assertEquals(myCallableStatement.toString(), parsed);
    }

    @Test
    public void testQueryStringNotExtracted() throws NoSuchMethodException {
        Object[] params = {};
        String parsed = JdbcExecuteInterceptor.parseQueryFromStatement(myPreparedStatement, params);

        // MyPreparedStatement does not have toString() overloaded
        assertNull(parsed);
    }

    @Test
    public void testRequestEventCreation() {
        Object[] params = {QUERY};
        ServiceRequestEvent event = JdbcExecuteInterceptor.enter(params, null, mockStatement);

        assertEquals(JdbcExecuteInterceptor.SQL_ORIGIN, event.getOrigin());
        assertEquals(DB_NAME, event.getService());
        assertEquals(QUERY, event.getOperation());
        assertEquals(mockStatement, event.getRequest());
    }

    @Test
    public void testRequestEventPublish() {
        Object[] params = {QUERY};
        ServiceRequestEvent event = JdbcExecuteInterceptor.enter(params, null, mockStatement);

        assertEquals(event, mockListener.getReceivedEvents().get(0));
    }

    @Test
    public void testResponseEventPublish() {
        JdbcExecuteInterceptor.exit(requestEvent, 1, null);

        ServiceResponseEvent received = (ServiceResponseEvent) mockListener.getReceivedEvents().get(0);

        assertEquals(requestEvent.getOrigin(), received.getOrigin());
        assertEquals(requestEvent.getOperation(), received.getOperation());
        assertEquals(requestEvent.getService(), received.getService());
        assertEquals(requestEvent, received.getRequest());
        assertEquals(1, received.getResponse());
        assertNull(received.getThrown());
    }

    @Test
    public void testResponseEventWithThrownPublish() {
        Exception ex = new SQLException();
        JdbcExecuteInterceptor.exit(requestEvent, null, ex);

        ServiceResponseEvent received = (ServiceResponseEvent) mockListener.getReceivedEvents().get(0);

        assertEquals(requestEvent, received.getRequest());
        assertEquals(ex, received.getThrown());
        assertNull(received.getResponse());
    }

    private boolean classMatches(Class clazz) {
        return JdbcExecuteInterceptor.buildClassMatcher().matches(new TypeDescription.ForLoadedType(clazz));
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
            if (JdbcExecuteInterceptor.buildMethodMatcher().matches(forLoadedMethod)) {
                matchedCount++;
            }
        }
        return matchedCount;
    }

    private class MockEventBusListener implements Listener {


        public List<Event> getReceivedEvents() {
            return receivedEvents;
        }

        private List<Event> receivedEvents = new ArrayList<>();

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void listen(Event event) {
            receivedEvents.add(event);
        }
    }
}
