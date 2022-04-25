## Inject API Test

This package contains a small self-contained integ test for disco-java-agent-inject-api, 
which is designed to solve for installation of Agents onto managed runtimes such as AWS Lambda.
 
The first test case 'testAgentThreadInterception' performs a very simple assertion that Transaction Context can be
preserved over a Thread hand-off boundary and that expected thread concurrency events are being published. 

The second test case 'testServletInterception' demonstrates the outcome of runtime instrumenting a subclass of 
'HttpServlet' with the help of the disco-java-agent-web plugin, resulting in Disco events now being published when 
invoking the ``service()`` method of the instrumented subclass.

The ``@BeforeClass`` Junit method installs the Agent via Injection. When injected, the agent is also capable of 
performing a discovery of plugins located under a supplied path. In this case of this test package, the path to 
the folder containing the disco-java-agent-web plugin Jar is supplied to Disco via agent arg. 

If that method or annotation were commented out or removed, the 2 test cases stated above would fail instead.