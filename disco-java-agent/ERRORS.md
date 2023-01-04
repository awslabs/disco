# disco-java-agent errors

The below describes unrecoverable errors that may be reported by *disco-java-agent*: their symptoms, likely causes,
and ways to resolve them.

## DiSCo(Concurrency) failed to instrument class java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask

### Symptom

Disco tried to instrument the class `java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask` upon Disco agent loading, but failed.

### Causes

This is likely because another Java agent was loaded before Disco and caused `ScheduledFutureTask` to be loaded before
Disco had the opportunity to instrument it. Java documentation (e.g., [that of Java 11](https://docs.oracle.com/en/java/javase/11/docs/api/java.instrument/java/lang/instrument/Instrumentation.html)) states
“The [re]transformation must not add, remove or rename fields or methods, change the signatures of methods, or change inheritance”.
As Disco's instrumentation of this class adds a field and an interface implementation, it cannot succeed in these conditions.

### Resolution

Please ensure that any other Java agents whose `premain()` method schedules work using `java.util.concurrent.ScheduledThreadPoolExecutor`
are loaded after the Disco agent. Alternatively, instrumenting your application at build time with Disco pre-processing should also
resolve this problem.
