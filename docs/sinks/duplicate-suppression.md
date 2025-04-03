# Duplicate Suppression

Duplicate Suppression is provided by two sinks that serve slightly different purposes.  The simplest is the
`SupressDuplicatesSink` which simply maintains the Least Recently Used (LRU) Cache of inputs and only forwards an input
onwards to the destination sink if it isn't currently in the cache.  This sink should be used when the input type
correctly implements `hashCode()` and `equals()` and you want to suppress duplicates based upon the input type itself.

The more complex version is the `SuppressUnmodifiedSink`. This also maintains an LRU cache of recently seen inputs but
can forward an input that is in the cache if it is considered to have been modified.  With this sink the cache is not
based directly on the input type but instead upon applying functions to transform the input into a cache key and value
which may be of different types to the input. If an inputs cache key is already present in the cache then its cached
value is retrieved and compared against the value.  If the value is unchanged it is suppressed, otherwise the cache is
updated and it is forwarded.  This sink can be useful if you want to suppress duplicates for types that don't provide
`hashCode()` and `equals()`, or where you want to suppress duplicates based on some transformation of the input while
still forwarding the original input as-is.

## Behaviours

- Forwarding
- Transforming: No
- Batching: No

## Parameters

For the `SupressDuplicatesSink` a destination `Sink` and a Cache size are required.

For the `SupressUnmodifiedSink` a destination `Sink`, Cache size, Key and Value functions plus a Value comparator are
required.

## Example Usage

In this first example we suppress duplicates based on complete values:

```java
List<String> inputs = Arrays.asList("aardvark", "adder", "bee", "cat", "caterpillar", "chicken", "dolphin", "elephant")
NullSink<String> destination = NullSink.of();
try (SuppressDuplicatesSink<String> sink 
        = SuppressDuplicatesSink.<String>create()
                                .cacheSize(10_000)
                                .metricsLabel("animals")
                                .destination(destination)
                                .build()) {
    for (String input : inputs) {
        sink.send(input);
    }
    System.out.println("Received " + destination.count() + " items");
}
```

Since all inputs were unique it would have printed that it received 8 items.

In this second example we suppress duplicates based on the first letter only:

```java
List<String> inputs = Arrays.asList("aardvark", "adder", "bee", "cat", "caterpillar", "chicken", "dolphin", "elephant")
NullSink<String> destination = NullSink.of();
try (SuppressUnmodifiedSink<String, String, String> sink 
        = SuppressUnmodifedSink.<String, String, String>create()
                               .cacheSize(10_000)
                               .metricsLabel("animals")
                               .keyFunction(k -> k.substring(0, 1))
                               .valueFunction(v -> v.substring(0, 1))
                               .comparator(Comparator.naturalOrder())
                               .destination(destination)
                               .build()) {
    for (String input : inputs) {
        sink.send(input);
    }
    System.out.println("Received " + destination.count() + " items");
}
```

So in this example there were only 5 unique starting letters so only 5 items would have been received by the destination
sink.

## Metrics

The duplicate suppression sinks collects optional metrics about how many duplicate items they have suppressed, these
metrics are named as follows:

- `items.duplicates_suppressed` - A counter of how many duplicate items were suppressed.
- `items.unmodified_suppressed` - A counter of how many unmodified items were suppressed.

Both these metrics are labelled by `items.type` using the label value supplied to their constructors.  These metrics can
be disabled by supplying `null` as the label value.  In the above examples you would get observations for both metrics
labelled with `item.stype=animals`.