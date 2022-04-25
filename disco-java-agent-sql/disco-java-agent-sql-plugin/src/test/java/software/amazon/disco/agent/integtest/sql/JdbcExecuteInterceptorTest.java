/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.agent.integtest.sql;

import org.junit.After;
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
import software.amazon.disco.agent.integtest.sql.source.MyCallableStatementImpl;
import software.amazon.disco.agent.integtest.sql.source.MyPreparedStatementImpl;
import software.amazon.disco.agent.integtest.sql.source.MyStatementImpl;
import software.amazon.disco.agent.reflect.event.EventBus;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JdbcExecuteInterceptorTest {
    private static final String QUERY = MyStatementImpl.QUERY;
    private static final String DB = "MY_DB";

    private TestListener listener;
    private Statement statement;
    private PreparedStatement preparedStatement;
    private CallableStatement callableStatement;

    @Mock
    Connection mockConnection;

    @Mock
    ResultSet mockResultSet;

    @Before
    public void setup() throws SQLException {
        listener = new TestListener();
        EventBus.addListener(listener);

        statement = new MyStatementImpl(mockConnection, mockResultSet);
        preparedStatement = new MyPreparedStatementImpl(mockConnection, mockResultSet);
        callableStatement = new MyCallableStatementImpl(mockConnection, mockResultSet);

        when(mockConnection.getCatalog()).thenReturn(DB);
    }

    @After
    public void cleanup() throws Exception {
        EventBus.removeAllListeners();
    }

    @Test
    public void testExecuteQueryOnStatement() throws SQLException {
        ResultSet rs = statement.executeQuery(QUERY);

        verifyRequestEvent(statement);
        verifyResponseEvent(rs);
    }

    @Test
    public void testExecuteUpdateOnStatement() throws SQLException {
        Integer res = statement.executeUpdate(QUERY);

        verifyRequestEvent(statement);
        verifyResponseEvent(res);
    }

    @Test
    public void testExecuteLargeUpdateOnStatement() throws SQLException {
        Long res = statement.executeLargeUpdate(QUERY);

        verifyRequestEvent(statement);
        verifyResponseEvent(res);
    }

    @Test
    public void testExecuteOnStatement() throws SQLException {
        Boolean res = statement.execute(QUERY);

        verifyRequestEvent(statement);
        verifyResponseEvent(res);
    }

    @Test
    public void testExecuteWithAdditionalArgsOnStatement() throws SQLException {
        Boolean res = statement.execute(QUERY, 0); // Tests JDBC 2.0

        verifyRequestEvent(statement);
        verifyResponseEvent(res);
    }

    @Test
    public void testExecuteQueryOnPreparedStatement() throws SQLException {
        ResultSet rs = preparedStatement.executeQuery();

        verifyRequestEvent(preparedStatement);
        verifyResponseEvent(rs);
    }

    @Test
    public void testExecuteQueryOnCallableStatement() throws SQLException {
        ResultSet rs = callableStatement.executeQuery();

        verifyRequestEvent(callableStatement);
        verifyResponseEvent(rs);
    }

    @Test(expected = SQLException.class)
    public void testExceptionCaughtAndThrown() throws SQLException {
        try {
            statement.executeUpdate(QUERY, new String[]{});  // This method implemented to throw exception
        } finally {
            verifyRequestEvent(statement);
            Assert.assertEquals(1, listener.responseEvents.size());
            Assert.assertTrue(listener.responseEvents.get(0).getThrown() instanceof SQLException);
        }
    }

    private void verifyRequestEvent(Statement statement) {
        Assert.assertEquals(1, listener.requestEvents.size());
        ServiceRequestEvent event = listener.requestEvents.get(0);
        Assert.assertEquals(statement, event.getRequest());
        Assert.assertEquals(QUERY, event.getOperation());
        Assert.assertEquals(DB, event.getService());
    }

    private void verifyResponseEvent(Object response) {
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
