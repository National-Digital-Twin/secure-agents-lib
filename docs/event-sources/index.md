# Event Sources
**Repository:** `secure-agents-lib`  
**Description:** `Event Sources provides access to a source of Events and is strongly typed`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

An `EventSource` provides access to a source of `Event`'s and is strongly typed. So for example an `EventSource<String,
Graph>` provides a source of events where each event consists of a `String` key and an RDF `Graph` value.

An Event Source is typically used as the starting point of a data processing pipeline, so events will be read from an
`EventSource` and passed onto another API for processing e.g. a [`Projector`](../projection/index.md) or a
[`Sink`](../sinks/index.md).

# Interface

## `Event`

The `Event` interface provides a common representation of events regardless of how the underlying `EventSource` might
represent them.  This is a strongly typed interface taking type parameters for both the key and value e.g.
`Event<Integer, String>` would be an event with an `Integer` key and a `String` value.  An event may also include
zero/more headers which are key value pairs represented using the `Header` record class.

The `key()` and `value()` methods provide access to the key and value of the event.

The `headers()` method provides a `Stream<Header>` over all the available headers, while the corresponding
`headers(String)` method provides the header values for the given key.  For example `event.headers("Content-Type")`
would return a `Stream<String>` values for the `Content-Type` header.  Additionally, the `lastHeader(String)` method
provides only the last value for a given key e.g. `event.lastHeader("Content-Type")` would return only the last value
for the `Content-Type` header.

An `Event` instance is immutable, however pipelines may mutate events, including their declared key and/or value types,
using the various replace methods:

- `replaceKey()` - Creates a new copy of the event with a new key.
- `replaceValue()` - Creates a new copy of the event with a new value.
- `replace()` - Creates a new copy of the event with a new key and value.

## `EventSource`

You can check whether events are available via the `availableImmediately()` which returns a `boolean` value indicating
whether events are available immediately without blocking. An implementation should return `true` for
`availableImmediately()` only when it already has events buffered in-memory that can be returned immediately. When this
method returns `true` any call to `poll()` is guaranteed to return a non-null event.

You can also call the `isExhausted()` method which returns a `boolean` indicating whether the source has been exhausted.
If `true` then there are no further events, nor will there ever be. Therefore, if a source is unbounded, i.e. it
represents a potentially infinite stream of events, then this **MUST** return `false` unless the source has been closed.

The `poll(Duration)` method polls for the next available event enforcing a timeout on that polling operation. This
either returns the next available event, or `null` if no events are currently available. If no events are immediately
available this method blocks up to the given timeout before returning either the next event, or `null` if no events are
yet available. A `null` return does not mean there are no further events, merely that there are no further events
available currently. Callers can check `isExhausted()` to determine whether there may be further events available in the
future.

The `processed(Collection<Event>)` method allows consumers of an event source to call back to the source to indicate
when they have finished processing events.  This may be a no-op for some sources while others may use this to record
state e.g. the [Kafka source](kafka.md) commits offsets when this method is called.

The `isClosed()` method indicates whether the event source has been closed while the corresponding `close()` method
tells the source that you are done reading from it allowing it to clean up any resources it may be holding open.

## `OffsetStore`

The `OffsetStore` interface abstracts the concept of storing offsets, whether for [Kafka](kafka.md) or any other event
source and has a relatively straightforward API.  Offsets are stored against arbitrary keys, and offset values may be of
any type that the implementation supports, which can be checked via the `supportsOffsetType(Class<T>)` method.

The `hasOffset(String)` method returns a `boolean` indicating whether an offset is stored for a given key.  The
`saveOffset(String, T)` and `T loadOffset(String)` methods respectively save and load an offset for a given key, and the
`deleteOffset(String)` method is used to remove a previously saved offset entirely.

Finally, the `flush()` and `close()` methods allow the store to be notified to persist the offsets (assuming the store is
persistent) and to release any resources it might be holding.

The lib module provides a concrete non-persistent `MemoryOffsetStore`.  This is built upon an `AbstractOffsetStore`
that implements much of the non-functional contract of the `OffsetStore` interface e.g. throwing a
`NullPointerException` if a `null` key is presented, throwing an `IllegalStateException` if operations are attempted
after a store has had `close()` called on it etc.  Developers who need to implement custom `OffsetStore` implementations
should consider using `AbstractOffsetStore` as the basis for any implementation of the interface.

In the `tests` classifier of the `event-source-lib` module you will also find an `AbstractOffsetStoreTests` class that
provides a test harness that can be used to verify that new `OffsetStore` implementations conform to the interface
contract.

# Usage

Typically, an `EventSource` is used by creating it and then polling for events in a loop before closing it when done
e.g.

```java
EventSource<TKey, TValue> source=createEventSource();

// Continue reading as long as the source is not exhausted
while (!source.isExhausted()) {
    // Wait up to 5 seconds for next event
    Event<TKey, TValue> next = source.poll(Duration.ofSeconds(5));
        
    // Null signifies no event currently available
    if (next == null)
        continue;

    // Do something with the event...
}

source.close();
```

Often you may not actually use the `EventSource` directly but rather pass it to a higher level API like the
[`ProjectorDriver`](../projection/driver.md) that automates the polling of the events and passing them onto a data
processing pipeline.

## Additional APIs

### `RdfPayload`

The `event-sources-lib` module also provides the `RdfPayload` type.  This is a container type that can be used to hold
either an Apache Jena `DatasetGraph` or `RDFPatch`.  This allows for processing event sources that may contain a mixture
of purely additive events (`DatasetGraph`'s) and mutative events (`RDFPatch`'s).

This type has `isDataset()` and `isPatch()` methods for detecting the actual payload type and corresponding
`getDataset()` and `getPatch()` methods for actually retrieving the payload contents.

A payload **CANNOT** contain both a `DatasetGraph` and an `RDFPatch`, it will always contain one or the other, **NEVER**
both.

As of `0.14.0` payloads can also be lazily deserialized, see [Lazy Deserialization](kafka.md#lazy-deserialization) for
more details.  The new `isReady()` method can be used to check whether the payload has been deserialized yet.

# Dependency

The `EventSource` API is provided by the `event-sources-lib` module which can be depended on from Maven like so:

```xml

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>event-sources-lib</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information.

## Apache Kafka Dependency

Typically, you will also need an Apache Kafka backed event source which is provided by the `event-source-kafka` module:

```xml

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>event-source-kafka</artifactId>
    <version>VERSION</version>
</dependency>
```

If you bring in the Kafka Dependency then the `event-sources-lib` module will be obtained as a transitive dependency.

# Behaviours

When talking about Event Sources one of the primary behaviours we are interested in is whether they are
Bounded/Unbounded. By this we mean whether they represent a finite or a potentially infinite stream of events. For
example a Kafka event source which is backed by a Kafka topic would be considered unbounded because that topic may
receive new events infinitely that can be consumed. An Unbounded event source is expected to always return `false` from
`isExhausted()` unless it has been closed.

Another interesting behaviour is whether an event source is Buffered/Unbuffered. For performance reasons event sources
may choose to consume their source in chunks and buffer those events in memory. For example in Kafka the underlying
`KafkaConsumer` provided by the Kafka project actually fetches batches of records from the Kafka brokers rather than a
single record at a time. This is because events in Kafka are small in size so network overheads can be expensive, by
fetching records in batches these overheads are reduced. For buffered event sources the current state of the buffer is
likely reflected in the return value from `availableImmediately()`.

The final behaviour of interest is whether the read policy is configurable. As Event Sources may be unbounded
controlling what portion of the source is read can be important, both in terms of being able to implement exactly-once
processing semantics or to forcibly reprocess data. Read policy is typically an implementation level detail and the
`event-sources-lib` module does not provide an API for this, different event source implementations e.g. Kafka, may
provide their own APIs around this.

# Implementations

The `event-sources-lib` module provides only a simple [In-Memory](in-memory.md) implementation.

The `event-source-kafka` module provides a [Kafka](kafka.md) implementation.

The `event-source-file` module provides a [File Based](file.md) implementation.

# Sinks

The `event-sources-lib` module also provides several additional [`Sink`](../sinks/index.md) implementations
specifically for working with events:

- The [`EventKeySink<TKey, TValue>`](../sinks/event-key.md) takes in `Event<TKey, TValue>` and outputs just `TKey` to a
  destination sink.
- The [`EventValueSink<TKey, TValue>`](../sinks/event-value.md) takes in `Event<TKey, TValue>` and outputs just `TValue`
  to a destination sink.
- The [`EventHeaderSink<TKey, TValue`](../sinks/event-header.md) modifies events by adding headers to them based upon
  generator functions.
- The [`EventProcessedSink<TKey, TValue>`](../sinks/event-processed.md) is a terminal sink that calls the `processed()`
  callback on the originating `EventSource` either on a per-event or event batch basis.

As with other Sinks these all provide builders for creating them e.g.

```java
// Collect up just the string values
EventValueSink<Integer, String> values 
    = EventValueSink.<Integer, String>create()
                    .collect()
                    .build();
                
// Collect up just the integer values
EventKeySink<Integer, String> values
    = EventKeySink.<Integer, String>create()
                  .collect()
                  .build();

// Add IANode standard headers
EventHeaderSink<Integer, String> headers
    = EventHeaderSink.<Integer, String>create()
                     .addStandardHeaders("Your-App")
                     .collect()
                     .build();

// Just mark the events as processed every 100 events
EventProcessedSink<Integer, String> processed
    = EventProcessedSink.<Integer, String>create()
                        .batchSize(100)
                        .build();
```
© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.