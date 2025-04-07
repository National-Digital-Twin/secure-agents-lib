# Projector Driver
**Repository:** `secure-agents-lib`  
**Description:** `Projector Driver`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The `ProjectorDriver` provides automation of a Secure Agents Projection pipeline. It takes in an
[`EventSource`](../event-sources/index.md), a `Projector` and a [`Sink`](../sinks/index.md) and automates the polling of
events from the event source and passing those through the projector and onto the `Sink`. As part of this automation it
provides throughput metrics reporting and automatic processing abortion based on several configurable triggers.

# Parameters

As there are lots of parameters, many of which are optional, a `ProjectorDriver` is built using a builder pattern.

```java
ProjectorDriverBuilder<TKey, TValue, TOutput> builder 
  = ProjectorDriver.<TKey, TValue, TOutput>create();
```

The type parameters denote the key and value type of the input events and the output type produced by the
[projector](index.md#projectors).

The parameters can be split into the following 4 groups:

1. Event Source
2. Projector
3. Sink
4. Lifecycle Management

Configuring an `EventSource` is done via the `source()` method which takes an `EventSource` instance and the
`pollTimeout()` is used to configure the timeout to use when the driver calls `poll()` on the event source.  

The `Projector` is supplied via the `projector()` method.

The `Sink` is the destination for the projected events, it is supplied indirectly via a `Supplier<Sink<TOutput>>` using
the `destination()` method.  This indirection is done because a driver is intended to be used as a runnable and a `Sink`
is `AutoCloseable`. Behind the scenes a driver wants to use the sink in a try with resources block so supplying the sink
via a `Supplier` ensures that the sink is only created when it is first needed and closed when it is no longer needed.
There are also overloads of `destination()` which takes a `Sink` directly, and a `destinationBuilder()` which takes a
`SinkBuilder`, in which case those are used to create a new `Supplier`.

The final group of parameters control the lifecycle of the driver i.e. how long it runs for. Firstly the `limit()`
method imposes a hard limit on the number of events the driver will process. In production usage this is rarely needed
because you want to process new events forever, but this is extremely useful for debugging and unit testing Secure Agents
projection pipelines. Any positive value acts as a limit, conversely any value less than zero will be treated as
unlimited i.e. the driver runs forever.

`maxStalls()` controls how many **consecutive** times the driver will allow the event source to stall before aborting. A
stall is when the event source produces no new events from a `poll()` within the provided timeout, configured via the
aforementioned `pollTimeout()` method. Any time new events are produced the stalls counter is reset to zero.

So a practical example probably helps illustrate this, assuming a `pollTimeout()` of 30 seconds and a `maxStalls()` of
10 the event source would have to fail to produce any events 10 times in a row. With a timeout of 30 seconds this would
mean there would need to be no new events for 300 seconds, i.e. 5 minutes, before the driver would abort. However, if
after 240 seconds new events started to be produced again the stall counter resets, and it would need to be another 300
seconds without events before the driver aborts.

Therefore `maxStalls()` can be used to naturally terminate a driver once there are no new events available for a
prolonged period of time. Obviously the choice of value for this parameter should take into account the `pollTimeout()`,
so if you had a timeout of 5 seconds then a `maxStalls()` of 10 would mean only 50 seconds without events before
termination.

Finally `reportBatchSize()` controls how often the driver will report throughput metrics for the projection pipeline.
This batch size is in terms of number of events read from the event source. If your projector produces multiple outputs
for each input event you may want to include a [`ThroughputSink`](../sinks/throughput.md) as part of your output sink
pipeline to report more fine-grained metrics.

# Example Usage

In this example we set relatively small `pollTimeout()` of 5 seconds, so we'll poll for up to 5 seconds at a time. We're
`limit()`'ing our events processed to 10 million, imposing a `maxStalls()` of 36 (36 * 5 seconds is 180 seconds) and
reporting throughput metrics every 100 thousand events.

```java
EventSource<Graph> source = createEventSource();
Projector<Event<Integer, Graph>, Event<Integer, Graph>> projector = new NoOpProjector<>();

ProjectorDriver<Integer, Graph, Event<Integer, Graph>> driver
        = ProjectorDriver.<Integer, Graph, Event<Integer, Graph>>create()
                          .source(source)
                          .projector(projector)
                          .destination(() -> NullSink.of())
                          .pollTimeout(Duration.ofSeconds(5))
                          .limit(10_000_000)
                          .maxStalls(36)
                          .reportBatchSize(100_000)
                          .build();

// Create a background thread to run the driver adding a shutdown hook that cancels it when the JVM is shutdown
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<?> future = executor.submit(driver);
Runtime.getRuntime().addShutdownHook(new Thread(() -> driver.cancel()));

// Wait for the driver to finish, or the user to Ctrl+C the application
try {
    future.get();
} catch (InterruptedException e) {
    LOGGER.warn("Interrupted while waiting for projection to finish");
} catch (ExecutionException e) {
    LOGGER.error("Unexpected error in projection: {}", e.getCause());
}
```

# Metrics

The `ProjectorDriver` automatically collects a number of metrics.  Internally it uses a
[`ThroughputTracker`](../sinks/throughput.md#metrics) which collects a number of metrics as detailed there, these will
be labelled with `items.type=events`.

Additionally, it exports the following driver specific metrics:

- `messaging.stalls.total` - A counter indicating how many times the projection has stalled i.e. the driver received no
  new events from the event source.
- `messaging.stalls.consecutive` - A gauge indicating how many consecutive times the projection has stalled.  This may
  go up and down depending on how caught up the driver is with the event source, and how frequently new data arrives
  from the event source.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.