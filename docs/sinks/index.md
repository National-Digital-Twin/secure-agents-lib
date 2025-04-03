# Sinks

A `Sink` is a simple functional interface to which items can be sent for processing. This is intended for defining
simple processing pipelines that implement data processing or transformation.

This is a strongly typed interface, so you might have a `Sink<Graph>` for a sink that takes in RDF Graphs or a
`Sink<Entity>` that takes in Entities.

# Interface

The interface for a `Sink` is very simple consisting of only two methods.

It has a `send(T)` method that sends an item to the sink for processing. This may throw an unchecked `SinkException` if
there is a problem processing the item.

Additionally, there is a `close()` method that should be called when no further items will be sent to the sink. A `Sink`
is also an `AutoCloseable` meaning it can be used in a try with resources block to ensure that `close()` is always
called when needed.  The `close()` method has a default no-op implementation meaning that if your `Sink` does not need
to release any resources it need not override this method at all.

As of 0.12.0 `Sink` is now marked as a Java `@FunctionalInterface` meaning that the compiler will allow you to define
simple `Sink` implementations that do not require a `close()` method as a lambda e.g.

```java
Sink<String> sink = item -> System.out.println(item);
```

Would be a valid `Sink` definition for a sink that prints the items received to standard output.

# Usage

Typically, a `Sink` will be used by creating it in a try with resources block, performing some data retrieval or
processing and sending the output data to the `Sink` e.g.

```java
try(Sink<String> sink = createSink()) {
  for(String item:someDataSource()){
    sink.send(item);
  }
}
```

Often you may not use a `Sink` directly but rather via another API that automates its usage e.g. a
[`Projector`](../projection/index.md) or a [`ProjectorDriver`](../projection/driver.md).

Sinks are built using a Fluent Builder API, each Sink type should have a static `create()` method that provides access
to its builder type than can be used to configure and build it.  Simple terminal sinks that require no configuration may
also provide a static `of()` method to create a new instance.  The `Sinks` static utility class also provides some
convenience methods for starting the definition of a sink based pipeline.

# Dependency

`Sink` implementations are primarily provided by the `projectors-lib` module which can be depended on from Maven like
so:

```xml

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>projectors-lib</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information. This module also provides the [`Projector` API](../projection/index.md).

# Behaviours

Sinks have several behavioural characteristics that are worth calling out explicitly.

Firstly a sink can either be a forwarding or a terminal sink. A forwarding sink is a sink that passes its output onto
another sink for further processing. Whereas a terminal sink is a sink that represents the last step of a processing
pipeline. Note that a terminal sink may have outputs but those outputs are not processed via the `Sink` API. For example
the [JSON Serialization](json.md) sink is a terminal sink, but it writes JSON output to an `OutputStream` so there is
output.

Secondly a sink can either be transforming or non-transforming. A transforming sink is a sink that transforms its input
before producing its output. Whereas a non-transforming sink leaves the input unchanged. A non-transforming sink does
not necessarily pass through all inputs. For example the [Filter](filter.md) sink does not transform its input, but it
will drop inputs that do not match its filter condition.

Additionally, a sink may be batching, meaning that its processing is batch oriented. This might mean it collects a batch
of inputs before doing any work, or that it only does work at certain intervals. For example the
[Throughput](throughput.md) sink tracks throughput metrics but only reports them when a given batch size is reached.

# Implementations

The `projectors-lib` module provides a number of general purpose implementations of the `Sink` API with useful common
functionality for data processing pipelines:

- [Filtering](filter.md): Filters data dropping filtered items.
    - [Rejecting](reject.md): Filters data with exceptions for rejected items.
- [Collection](collector.md): Collect data.
- [Null](null.md): Throws away data.
- [Duplicate Suppression](duplicate-suppression.md): Suppresses duplicate data.
- [Throughput Reporting](throughput.md): Tracks and reports throughput metrics.
- [JSON Serialization](json.md): Writes data out as JSON.
- [Resource Cleanup](cleanup.md): Guarantees clean up of `Closeable` resource(s) when pipelines are `close()`'d.

The [`event-sources-lib`](../event-sources/index.md#sinks) module provides the following additional
implementations:

- [`EventKeySink`](event-key.md)
- [`EventValueSink`](event-value.md)
- [`EventHeaderSink`](event-header.md)
- [`EventProcessedSink`](event-processed.md)

The [`event-source-kafka`](../event-sources/kafka.md) module provides the following additional implementations:

- [Kafka](kafka.md): Sends events to Kafka.

# Additional Utilities

The `projectors-lib` module also provides a couple of additional utility classes that you will find reused throughout
the codebase.

## `ThroughputTracker`

As discussed in relation to the [`ThroughputSink`](throughput.md) the actual throughput tracking and reporting is
encapsulating into a `ThroughputTracker` class.  This can be used directly if so desired by using the static `create()`
method to obtain a `ThroughputTrackerBuilder` and configure it as desired e.g.

```java
ThroughputTracker tracker 
  = ThroughputTracker.create()
                     .logger(someLogger)
                     .reportInteval(100_000)
                     .inSeconds()
                     .action("Processed")
                     .itemsName("Items")
                     .build();
tracker.start();
while (someCondition) {
    // Get an item from somewhere...
    tracker.itemReceived();
    // Do something with the item...
    tracker.itemProcessed();
}

// Report throughput and reset when done
tracker.reportThroughput();
tracker.reset();
```

See `ProjectorDriver` for an example of this used in real application code.

You can inspect the state of the tracker via various methods e.g. `receivedCount()` and `processedCount()` for items
seen, `firstTime()` and `lastTime()` for when the tracker was started/received its first item and when it processed the
last item, and `getOverallRate()` to get the overall processing rate for the lifetime of the tracker.

## `PeriodicAction`

Additionally, we have a `PeriodicAction` class which is a leaky bucket rate limiter using the "as a meter" semantics.
This permits an action to run at most once during a configurable interval.  This can be used for various purposes e.g.

- Calculating and reporting an expensive metric periodically
- Avoiding flooding the logs with a warning when certain conditions occur

The action is provided either as a `Runnable` or as a `Callable<Boolean>`, in the latter case the return value is used
to indicate whether the action was actually performed.  For example, you might have an action that needs to calculate
some expensive statistic in order to decide whether to issue a warning, you want to issue that warning as soon as the
statistical test is met **BUT** don't want to issue it continuously.  In this case using a `Callable<Boolean>` allows
your action to decide whether to output that warning and indicate when it has output it.

A practical example helps illustrate this, here a warning is issued every 30 seconds (at most), but only when a
performance threshold is exceeded.

```java
Callable<Boolean> callable = () -> {
  double yourStat = computePerformanceStatistic();
  if (yourStat < warningThreshold) {
    LOGGER.warn("Performance {} fell below acceptable minimum {}", yourStat, warningThreshold);
    return true;
  }
  return false;
};

// Create an action that issues the warning at most every 30 seconds when the performance falls below some threshold
PeriodicAction action = new PeriodicAction(callable, Duration.ofSeconds(30));

while (someCondition) {
    // Do some work

    // Call run() to invoke the action, if the action has already run within the configured interval then this is a
    // no-op
    action.run();
}
```

Contrast this with the case where you just want to log a statistic on a predictable interval:

```java
Runnable runnable = () -> { LOGGER.info("Current performance is {}", computePerformanceStatistic()); };

// Create an action that logs the performance statistic once every 5 minutes
PeriodicAction action = new PeriodicAction(runnable, Duration.ofMinutes(5));

while (someCondition) {
    // Do some work

    // Call run() to invoke the action, if the action has already run within the configured interval then this is a
    // no-op
    action.run();
}
```
Here we just want to log a performance statistic once every 5 minutes.

In practice, if you want to guarantee to run an action once within an interval use a `Runnable`, if you conditionally
want to run an action at most once within an interval then use a `Callable<Boolean>`.

