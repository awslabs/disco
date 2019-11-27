## Example Agent Tests

This package contains two suites of tests.

### Testing a standalone agent

The tests succeed when running the 'test' target due to the presence of the disco-java-agent-example Agent

### Testing a pluggable agent

The tests succeed when running the 'testViaPlugin' target due to the presence of the Pluggable disco-java-agent
and its discovery of disco-java-agent-web-plugin.

---

See the mentioned packages, and the content of the build.gradle.kts file for a deep dive into why the tests succeed.