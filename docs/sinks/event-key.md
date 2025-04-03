# Event Key Sink

The `EventKeySink` is a forwarding sink that takes in events and only outputs the keys.

Note that this sink is provided by the [`event-sources-lib` module](../event-sources/index.md#sinks) not the
`projectors-lib` module so requires a dependency on the `event-sources-lib` module to be available.

## Behaviours

- Forwarding
- Transforming: Yes
- Batching: No

## Parameters

This sink only needs a destination sink to which it forwards the keys of the events it receives.

## Example Usage

In this toy example we sum up the keys:

```java
AtomicInteger sum = new AtomicInteger(0);
try (EventKeySink<Integer, String> sink 
        = EventKeySink.<Integer, String>create()
                   .destination(x -> sum.addAndGet(x)))
                   .build()) {
    for (Event<Integer, String> input : someDataSource()) {
        sink.send(input);
    }
    System.out.println("Key Sum was " + sum.get());
}
```
