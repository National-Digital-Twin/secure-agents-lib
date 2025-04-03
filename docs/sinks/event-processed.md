# Event Processed Sink

The `EventProcessedSink` is a terminal sink that calls the `processed()` method on the received events associated
[`EventSource`](../event-sources/index.md#event-sources)

Note that this sink is provided by the [`event-sources-lib` module](../event-sources/index.md#sinks) not the
`projectors-lib` module so requires a dependency on the `event-sources-lib` module to be available.

## Behaviours

- Terminal
- Transforming: No
- Batching: Optional

## Parameters

This sink optionally takes a batch size configured via the `batchSize()` of its builder.  If this is set to `1` or
`noBatching()` is called then no batching occurs.

## Example Usage

In this example we simply mark events as processed every 100 events:

```java
try (EventProcessedSink<Integer, String> sink 
        = EventProcessedSink.<Integer, String>create()
                   .batchSize(100)
                   .build()) {
    for (Event<Integer, String> input : someDataSource()) {
        sink.send(input);
    }
}
```
