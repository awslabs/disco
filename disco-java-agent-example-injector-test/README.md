## Inject API Test

This package contains a small self-contained integ test for disco-java-agent-inject-api,
which is designed to solve for installation of Agents onto managed runtimes such as AWS Lambda.

The contained test performs a very simple assertion that Transaction Context was preserved over a Thread
hand-off boundary. The ``@BeforeClass`` Junit method installs the Agent via Injection.

If that method is commented out, or removed, or if that annotation is removed, you will see the test fail instead.