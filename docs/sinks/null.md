# Null Sink

The `NullSink` is a terminal sink that throws away all inputs.  It's primary use is for unit testing and debugging in
conjunction with another sink.  It does count the inputs it has discarded which can be inspected via the `count()`
method.

## Behaviours

- Terminal
- Transforming: No
- Batching: No

## Parameters

This sink takes no parameters.

## Example Usage

In this example we create a `NullSink` and send it a bunch of input before reporting how many inputs we discarded:

```java
try (NullSink<String> sink = NullSink.of()) {
    for (String input : someDataSource()) {
        sink.send(input);
    }

    System.out.println("Discarded " + sink.count() + " inputs");
}
```