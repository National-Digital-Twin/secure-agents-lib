# Cleanup Sink

The `CleanupSink` is a forwarding sink that passes through items unchanged but holds references to one/more `Closeable`
resources that should be `close()`'d when the pipeline is closed.

The `CleanupSink` makes some useful guarantees by virtue of its implementation:

1. It guarantees to always call the `close()` method of the destination sink first.  So any resources explicitly held by
   other sinks further along the pipeline can be cleaned up by the sinks that own those.
2. It guarantees to always call the `close()` method of all registered `Closeable` resources **regardless** of whether
   the destination sinks `close()` method throws an exception.
3. It guarantees to always call the `close()` method of all registered `Closeable`'s even if any of their `close()`
   methods throw an exception.

Therefore this sink is usually placed at the start of a pipeline and is provided with all the resources you want to
guarantee are cleaned up regardless of whether the pipeline exits normally or abnormally.

## Behaviours

- Forwarding
- Transforming: Yes
- Batching: No

## Parameters

This sink takes a destination `Sink<T>` and one/more `Closeable` resources that should be closed when the `close()`
method is called.

## Example Usage

In this example we are creating a `BufferedWriter` and using a lambda sink - `x -> output.println(x)` - as the
destination.  The `CleanupSink` is wrapped around this to ensure that the writer is closed when the sinks work is done.

```java
BufferedWriter output = new BufferedWriter(new FileWriter("example.txt"));
try (CleanupSink<String> sink 
        = CleanupSink.<String>create()
                   .resource(output)
                   .destination(x -> output.println(x))
                   .build()) {
    for (String input : someDataSource()) {
        sink.send(input);
    }
}
```

Obviously in this toy example you could achieve this without the `CleanupSink` by just wrapping the `BufferedWriter` in
its own try-with-resources block.  However, in real pipelines there may be `Closeable` resources for which your code
does not directly control the lifecycle and so can't use this pattern e.g. singleton instances owned by CLI option
modules.
