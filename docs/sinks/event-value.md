# Event Value Sink

The `EventValueSink` is a forwarding sink that takes in events and only outputs the values.

Note that this sink is provided by the [`event-sources-lib` module](../event-sources/index.md#sinks) not the
`projectors-lib` module so requires a dependency on the `event-sources-lib` module to be available.

## Behaviours

- Forwarding
- Transforming: Yes
- Batching: No

## Parameters

This sink only needs a destination sink to which it forwards the keys of the events it receives.

## Example Usage

In this example we print out the values:

```java
try (EventValueSink<Integer, String> sink 
        = EventValueSink.<Integer, String>create()
                   .destination(x -> System.out.println(x)))
                   .build()) {
    for (Event<Integer, String> input : someDataSource()) {
        sink.send(input);
    }
}
```
