# IA Node Live Reporting

The [`live-reporter`](#dependency) module supplies APIs that allow for status and error reporting so that applications
based upon these libraries can be monitored and visualised in our IA Node Live application.

Currently, there are two main APIs:

- [`LiveReporter`](#live-reporter) for reporting Heartbeat status.
- [`LiveErrorReporter`](#live-error-reporter) for reporting errors.

# Live Reporter

The Live Reporter, embodied in the `LiveReporter` class, is a utility that spawns a background thread within the JVM
that periodically reports a status heartbeat to some underlying [`Sink`](../sinks/index.md).  In a production
environment this will be a [`KafkaSink`](../sinks/kafka.md) allowing the heartbeats to be read elsewhere within the
platform and visualised in our IA Node Live monitoring application.

The Heartbeat Status reporting feature is outlined in the [IA Node Status Reporting][1] design document. But to summarise
it here, each application running on the platform should report periodic status heartbeats to a Kafka topic.  Monitoring
applications can then process these heartbeats to detect, report and visualise what is running on the platform.  A
heartbeat status message is a JSON payload that typically looks like the following:

```json
{
   "id": "rdf-dump",
   "name": "Kafka RDF Dumper",
   "timestamp": "2023-02-27T13:32:08.279+00:00",
   "input": {
      "name": "knowledge",
      "type": "topic"
   },
   "output": {
      "name": "stdout",
      "type": "stream"
   },
   "status": "STARTED",
   "instance_id": "eb1b5de5-6b9e-49ea-a88b-435fd6646f0b",
   "component_type": "projector",
   "reporting_period": 15
}
```

The `LiveReporter` implementation handles the details of generating and sending these heartbeats based upon the
configuration provided when it is [created](#creating-a-livereporter) as detailed below.

## Creating a `LiveReporter`

As a `LiveReporter` requires a large number of parameters we use a Builder pattern for creating an instance e.g.

```java
LiveReporter reporter 
    = LiveReporter.create()
                  .id("your-app-id")
                  .name("Your Secure Agents")
                  .componentType("secureagent")
                  .input("knowledge", "topic")
                  .output("some-db", "database")
                  .reportingPeriod(Duration.ofSeconds(30))
                  .toKafka(k -> k.bootstrapServers("kakfa-broker:9092"))
                  .build();
```

In the above example we create a reporter for an Application with an ID of `your-app-id`, a human-readable name of `Your
Secure Agents` and component type `secureagent`.  It also declares its input source and output destination and sets a
reporting period of 30 seconds i.e. heartbeats will be sent once per 30 seconds.  Finally, it is configured to send these
heartbeats to the Kafka cluster at `kafka-broker:9092`.

Note that this example did not supply a Kafka topic, in which case the default topic of `provenance.live` is used.
However, the `toKafka()` method operates upon a builder for the [`KafkaSink`](../sinks/kafka.md) so any additional
supported Kafka configuration may be customised as desired.

## Starting the Reporter

Once you have a `LiveReporter` instance simply call `start()` to launch the background thread and start emitting
heartbeat statuses periodically i.e.

```java
reporter.start()
```

Since the reporting thread is run in the background this is a non-blocking call and your code can proceed normally once
this method has returned.

Note that calling `start()` includes registering a JVM shutdown hook that will attempt to stop the reporter in the event
of an unanticipated JVM shutdown.  Remember that shutdown hooks are not guaranteed to run, so you should ensure you [stop
the reporter explicitly](#stopping-error-reporting) wherever possible.

## Stopping the Reporter

When your application terminates you can stop the reporter via the `stop()` method which takes in the desired
final status to report.  Typically, this will be one of `LiveStatus.COMPLETED`, `LiveStatus.TERMINATED` or
`LiveStatus.ERRORING`.

```java
reporter.stop(LiveStatus.COMPLETED)
```

Note that calling `stop()` will potentially block for a short time while it attempts to gracefully terminate the
background thread but is guaranteed to not block indefinitely.

# Live Error Reporter

The Live Error Reporter, provided by the `LiveErrorReporter` class, is a utility that allows for reporting errors that
occur within an application.  These errors may be recoverable or non-recoverable, but by reporting them the platforms
monitoring infrastructure is able to visualise them.  As with the `LiveReporter` these errors go to some underlying
[`Sink`](../sinks/index.md), in a production environment this will be a [`KafkaSink`](../sinks/kafka.md).

The Error reporting feature is outlined in the [IA Node Status Reporting][1] design document, essentially each error is a
JSON payload containing error details e.g.

```json
{
	"id": "ExampleProjector",
	"level": "WARN",
	"timestamp": "2023-04-26T08:53:17.200+00:00",
	"counter": -1,
	"error_type": "Throwable",
	"error_message": "Randomly generated error",
	"stack_trace": "java.base/java.lang.Thread.getStackTrace(Thread.java:1610)\nuk.gov.dbt.ndtp.secure.agent.live.LiveErrorReporter.reportError(LiveErrorReporter.java:77)\nuk.gov.dbt.ndtp.secure.agent.cli.commands.debug.FakeReporter.lambda$run$0(FakeReporter.java:108)\nuk.gov.dbt.ndtp.secure.agent.projectors.utils.PeriodicAction.lambda$wrapRunnable$0(PeriodicAction.java:89)\nuk.gov.dbt.ndtp.secure.agent.projectors.utils.PeriodicAction.run(PeriodicAction.java:103)\nuk.gov.dbt.ndtp.secure.agent.projectors.utils.PeriodicAction.lambda$autoTrigger$1(PeriodicAction.java:168)\njava.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539)\njava.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\njava.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)\njava.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)\njava.base/java.lang.Thread.run(Thread.java:833)\n"
}
```

The `LiveErrorReporter` handles the details of generating this data structure based upon the `LiveError` class provided
by this module.

# Live Error Reporter

## Creating a `LiveErrorReporter`

A `LiveErrorReporter` again uses a builder pattern to create:

```java
LiveErrorReporter errorReporter 
  = LiveErrorReporter.create()
                     .id("your-app-id")
                     .toKafka(k -> k.bootstrapServers("kakfa-broker:9092"))
                     .build();
```

Note that the Application ID used for your error reporter **MUST** match the Application ID used for your heartbeat
reporter otherwise the reported errors will not be properly correlated with the application within IA Node Live.

As with our earlier example we did not supply a Kafka topic, in which case the default topic of `provenance.errors` is
used for error reporters. However, the `toKafka()` method operates upon a builder for the
[`KafkaSink`](../sinks/kafka.md) so any additional supported Kafka configuration may be customised as desired.

## Registering an Error Reporter

Typically, you will not want to create and track `LiveErrorReporter` instances everywhere your application might produce
an error that needs reporting.  Instead, you can use the `IANodeLive` static class to register, and later retrieve,
your configured error reporter:

```java
// In your application setup
IANodeLive.setErrorReporter(errorReporter);
```

And elsewhere in your code:

```java
LiveErrorReporter errorReporter = IANodeLive.getErrorReporter();
if (errorReporter != null) {
   // Report error...
}
```

## Reporting an Error

To actually report an error we need to create a `LiveError` instance, since this has many fields, most of which are
optional (since `LiveErrorReporter` will inject suitable default values at report time), it is created via a Builder
pattern:

```java
LiveError e = LiveError.create()
                        .id("your-app-id")
                        .now()
                        .type("RuntimeException")
                        .message("An unexpected error occurred")
                        .level(Level.WARN)
                        .recordCounter(11489L)
                        .build();
errorReporter.reportError(e);
```

Once created call `reportError()` on your error reporter to actually report the error.

## Stopping error reporting

When your application is terminating you should make a best effort to call `close()` on the `LiveErrorReporter`.  This
has the effect of closing the underlying `Sink` which depending on the implementation may release any held resources and
ensures any unpublished error reports have been submitted.

# Dependency

These APIs are provided by the `live-reporter` module which can be depended on from Maven like so:

```xml

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>live-reporter</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information.

[1]: https://github.com/IANode-ndtp/dbt/gov/uk-architecture/blob/status-reporting/IANodePlatform/IANode-Status-Reporting.md
