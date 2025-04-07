# Projectors
**Repository:** `secure-agents-lib`  
**Description:** `Projectors`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

A `Projector` implements a data projection, it takes in an input and produces zero or more outputs to a provided `Sink`.
Projectors implement the main logic of a projection, for example selection of entities of interest, using sinks to
provide further processing of their outputs. A projector is strongly typed so, you might have a `Projector<Graph,
Entity>` that projects from RDF Graphs to Entities.

A `ProjectorDriver` automates the connection of the various core concepts into a runnable application. It takes in an
[`EventSource`](../event-sources/index.md), a `Projector` and a [`Sink`](../sinks/index.md) and automates the polling of
events from the event source and passing those through the projector and onto the `Sink`.

# Interface

The interface for a `Projector` consists of a single method `project(TInput input, Sink<TOutput> output)` that should be
called for each input. This method should implement the projection logic and for each output produced call `send()` on
the provided output `Sink`.

`ProjectorDriver` is a concrete class rather than an interface, it implements the `Runnable` interface meaning it can be
run by calling the `run()` method. Preferably this is done by putting the instance onto a separate thread. It also
provides a `cancel()` method that can be used to tell the driver to stop and abort further processing.

# Usage

A `Projector` is not typically used directly but rather via a `ProjectorDriver` like so:

```java
EventSource<Graph> source = createEventSource();
Projector<Graph, Graph> projector = new NoOpProjector<>();

ProjectorDriver driver 
    = new ProjectorDriver(source, Duration.ofSeconds(30), Duration.ofSeconds(10), projector,
                          ()->new NullSink<Graph>(), -1, -1, 100_000);

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

As can be seen a `ProjectorDriver` takes a number of parameters, see the [Driver documentation](driver.md) for more
details.

# Dependency

`Projector` implementations are primarily provided by the `projectors-lib` module which can be depended on from Maven
like so:

```xml

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>projectors-lib</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information. This module also provides the [`Sink` API](../sinks/index.md).

You will also find additional implementations of this interface in other modules such as the [Entity Collection
API](https://github.com/National-Digital-Twin/secure-agent-search/blob/main/docs/entity-collector/index.md) which will require
dependencies on those modules.

`ProjectorDriver` is provided by a separate `projector-driver` module which can be depended on from Maven like so:

```xml

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>projector-driver</artifactId>
    <version>VERSION</version>
</dependency>
```

# Implementations

The `projectors-lib` only provides a single implementation, the `NoOpProjector`. This implementation simply passes the
input directly to the output sink as-is. The `entity-collector` module provides the
[`EntityCentricProjector`](https://github.com/National-Digital-Twin/secure-agent-search/blob/main/docs/entity-collector/projector.
md) which is the only other concrete implementation currently.

The `projector-driver` module provides the [`ProjectorDriver`](driver.md) which has its own dedicated documentation.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.
