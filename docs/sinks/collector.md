# Collector Sink

The `CollectorSink` is a terminal sink that collects up the inputs into a `List<T>` for later inspection.  Similar to a
[`NullSink`](null.md) this is primarily intended for unit testing and debugging of pipelines.

## Behaviours

- Terminal
- Transforming: No
- Batching: No

## Parameters

This sink takes no parameters.  An instance can be created via the static `of()` method.

## Example Usage

In this example we collect up the inputs and display the first and last inputs:

```java
try (CollectorSink<String> sink = CollectorSink.of()) {
    for (String input : someDataSource()) {
        sink.send(input);
    }

    List<String> collected = sink.get();
    System.out.println("First Input: " + collected.get(0));
    System.out.println("Last Input: " + collected.get(collected.size() - 1));
}
```