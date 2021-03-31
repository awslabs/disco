package software.amazon.disco.agent.sql;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import software.amazon.disco.agent.event.EventBus;
import software.amazon.disco.agent.event.ServiceDownstreamRequestEvent;
import software.amazon.disco.agent.event.ServiceDownstreamResponseEvent;
import software.amazon.disco.agent.event.ServiceRequestEvent;
import software.amazon.disco.agent.event.ServiceResponseEvent;
import software.amazon.disco.agent.interception.Installable;
import software.amazon.disco.agent.logging.LogManager;
import software.amazon.disco.agent.logging.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * This class represents the Disco interception of SQL queries performed using the JDBC. Some relevant information
 * to know about the JDBC is that there are 3 Statement interfaces, which are implemented by various SQL databases
 * such as MySQL. Statement classes are used to define queries then execute them. The inheritance of the Statement
 * interfaces is:
 *
 * CallableStatement extends PreparedStatement extends Statement
 *
 * Callable and Prepared statements are typically used for queries whose query strings are defined in advanced and
 * reused several times, whereas the base Statement class is more often used for one-off queries. Each statement
 * interface defines three methods to execute queries, which are explained more below.
 */
public class JdbcExecuteInterceptor implements Installable {
    // Must be public for use in Advices
    public static final Logger log = LogManager.getLogger(JdbcExecuteInterceptor.class);
    public static final String SQL_ORIGIN = "SQL";

    /**
     * This method is inlined at the beginning of all execute methods matched by {@link #buildMethodMatcher}. It
     * extracts some information from the intercepted execute method and calling object, then publishes a SQL query
     * request event as a {@link ServiceDownstreamRequestEvent}. The Query "service" is modeled as:
     *
     * Origin = "SQL"
     * Service = Target database name
     * Operation = SQL query string
     * Request = JDBC Statement object constructed by user, intercepted by ByteBuddy
     *
     * Must be public for use in Advice methods https://github.com/raphw/byte-buddy/issues/761
     *
     * @param args parameters passed to execute method, potentially including the query string
     * @param origin Identifier of the intercepted method, for debugging/logging
     * @param stmt concrete statement class being used to make the query
     * @return a ServiceDownstreamRequestEvent with fields populated on a best effort basis
     */
    @Advice.OnMethodEnter
    public static ServiceRequestEvent enter(@Advice.AllArguments final Object[] args,
                                            @Advice.Origin final String origin,
                                            @Advice.This final Statement stmt) {
        if (LogManager.isDebugEnabled()) {
            log.debug("DiSCo(Sql) interception of " + origin);
        }

        String query = null;
        String db = null;
        try {
            query = parseQueryFromStatement(stmt, args);
        } catch (Exception e) {
            log.warn("Disco(Sql) failed to retrieve query string for SQL Downstream Service event", e);
        }

        try {
            db = stmt.getConnection().getCatalog();
        } catch (Exception e) {
            log.warn("Disco(Sql) failed to retrieve Database name for SQL Downstream Service event", e);
        }

        // TODO: Consider replacing Statement Request object with a serializable object containing only relevant metadata
        ServiceRequestEvent requestEvent = new ServiceDownstreamRequestEvent(SQL_ORIGIN, db, query)
                .withRequest(stmt);
        EventBus.publish(requestEvent);
        return requestEvent;
    }

    /**
     * This method is inlined with an execute method at the moment it would return or throw a {@link Throwable}.
     * It extracts the response and throwable if any and publishes a {@link ServiceDownstreamResponseEvent}.
     * See {@link #enter} for Event model.
     *
     * Must be public for use in Advice methods https://github.com/raphw/byte-buddy/issues/761
     *
     * @param requestEvent the returned value of {@link #enter} method, passed in using the Enter annotation
     * @param response the response of the JDBC execute method or null if an exception was thrown, passed in using the
     *                 Return annotation
     * @param thrown the Throwable thrown by the query, or null if query was successful. Passed in using the Thrown
     *               annotation. Typically a {@link SQLException}.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter final ServiceRequestEvent requestEvent,
                            @Advice.Return final Object response,
                            @Advice.Thrown final Throwable thrown) {

        ServiceResponseEvent responseEvent = new ServiceDownstreamResponseEvent(
                SQL_ORIGIN,
                requestEvent.getService(),
                requestEvent.getOperation(),
                requestEvent)
                .withResponse(response)
                .withThrown(thrown);

        EventBus.publish(responseEvent);
    }

    /**
     * Installs the Disco SQL interception library into a Java program. Intended to be invoked
     * during an agent's premain.
     *
     * @param agentBuilder - an AgentBuilder to append instructions to
     * @return - the {@code AgentBuilder} object for chaining
     */
    @Override
    public AgentBuilder install(final AgentBuilder agentBuilder) {
        return agentBuilder
                .type(buildClassMatcher())
                .transform(new AgentBuilder.Transformer.ForAdvice()
                    .advice(buildMethodMatcher(), JdbcExecuteInterceptor.class.getName()));
    }

    /**
     * This helper method attempts to get the query string in two ways before giving up and returning null. The first is
     * just retrieving it from the arguments passed to the execute method being intercepted. If it is not present there,
     * then we must be dealing with a Prepared or Callable statement that had its query string pre-loaded rather than
     * passed as an argument. In this case, there is no way provided by the JDBC to extract the query. However many DB
     * Drivers implement the {@code toString} method on their PreparedStatement class to return the pre-loaded SQL query
     * string. So the second way is to check if the {@code toString} method is overridden, and if so we use it.
     *
     * See: https://stackoverflow.com/questions/2382532/how-can-i-get-the-sql-of-a-preparedstatement
     *
     * Must be public for use in Advice methods https://github.com/raphw/byte-buddy/issues/761
     *
     * @param stmt the JDBC Statement object being used to make the query
     * @param params the parameters passed for the execution of the SQL query. If they exist, they must contain the
     *               SQL query string.
     * @return the query string used in this SQL query if readable, or {@code null} otherwise
     */
    public static String parseQueryFromStatement(Statement stmt, Object[] params) throws NoSuchMethodException {
        String query = null;
        if (params != null && params.length > 0 && params[0] instanceof String) {
            query = (String) params[0];
        } else if (stmt instanceof PreparedStatement && Statement.class.isAssignableFrom(stmt.getClass().getMethod("toString").getDeclaringClass())) {
            query = stmt.toString();
        }

        return query;
    }

    /**
     * Builds an element matcher that will match any implementation of the Statement classes:
     * Statement, PreparedStatement, and CallableStatement. Checking for the Statement class super type is sufficient
     * because the other two are just extensions of Statement.
     * Exposed for testing.
     *
     * @return - An ElementMatcher suitable to pass to the type() method of an AgentBuilder
     */
    static ElementMatcher<? super TypeDescription> buildClassMatcher() {
        return hasSuperType(named("java.sql.Statement"))
                .and(not(isInterface()));
    }

    /**
     * Builds an ElementMatcher for the 4 methods used by statements to carry out a SQL query:
     * execute, executeQuery, and executeUpdate, and executeLargeUpdate. All 4 methods can be called with a
     * String parameter containing the SQL query string in all Statement classes. However, in the PreparedStatement and
     * CallableStatement classes, the first 3 are implemented without any parameters, because the query string is
     * passed when the Statement is constructed instead. So we match against normal statements that take a string
     * parameter, or prepared/callable statements that do not.
     * Exposed for testing.
     *
     * @return - An ElementMatcher that can match one of the methods to perform a SQL query
     */
    static ElementMatcher<? super MethodDescription> buildMethodMatcher() {
        // These are used for Prepared and Callable statements where execute methods do not have to take args
        ElementMatcher.Junction<TypeDescription> preparedStatementMatcher = hasSuperType(named("java.sql.PreparedStatement")).and(not(isInterface()));
        ElementMatcher.Junction<MethodDescription> takesNoArgsMatcher = isDeclaredBy(preparedStatementMatcher).and(takesArguments(0));

        // This is for regular Statements which always provide a SQL string as the first arg to execute methods
        ElementMatcher.Junction<MethodDescription> takesSqlArgMatcher = isDeclaredBy(buildClassMatcher()).and(takesArgument(0, String.class));

        ElementMatcher.Junction<MethodDescription> executeMatcher = named("execute").and(returns(boolean.class));
        ElementMatcher.Junction<MethodDescription> executeUpdateMatcher = named("executeUpdate").and(returns(int.class));
        ElementMatcher.Junction<MethodDescription> executeLargeUpdateMatcher = named("executeLargeUpdate").and(returns(long.class));
        ElementMatcher.Junction<MethodDescription> executeQueryMatcher = named("executeQuery").and(returns(ResultSet.class));

        ElementMatcher.Junction<MethodDescription> methodNameMatches = executeMatcher.or(executeUpdateMatcher).or(executeQueryMatcher).or(executeLargeUpdateMatcher);
        ElementMatcher.Junction<MethodDescription> argumentTypesCorrect = takesNoArgsMatcher.or(takesSqlArgMatcher);

        return methodNameMatches.and(argumentTypesCorrect).and(not(isAbstract()));
    }
}
