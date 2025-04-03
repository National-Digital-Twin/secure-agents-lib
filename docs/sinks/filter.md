# Filter Sink

A `FilterSink` is a forwarding sink that only forwards inputs that match its filter condition. Inputs that do not match
the condition are simply dropped, if you'd prefer them to be rejected with an exception see the
[`RejectSink`](reject.md) instead.

## Behaviours

- Forwarding
- Transforming: No
- Batching: No

## Parameters

This sink takes a destination `Sink<T>` and a filter condition expressed as a `Predicate<T>`.

If there is no provided filter condition then the filter condition defaults to forwarding all inputs i.e. it becomes
`t -> true`.

## Example Usage

In this example we create a `FilterSink` whose condition filters for even numbers and then send it a bunch of input
numbers.  Only those inputs that met the filter condition, i.e. were even numbers, will have been passed onto our
destination sink.

```java
NullSink<Integer> destination = NullSink.of();
try (FilterSink<Integer> sink 
        = FilterSink.<Integer>create()
                    .predicate(i -> i % 2 == 0)
                    .destination(destination)
                    .build()) {
    for (int i = 0; i < 1_000_000; i++) {
        sink.send(i);
    }

    System.out.println("Found " + destination.count() + " even numbers");
}
```

## Metrics

A filter sink can optionally collect metrics about how many items it has filtered out, to use this functionality you
must create an instance with a `metricsLabel` value i.e.

```java
NullSink<Integer> destination = NullSink.of();
try (FilterSink<Integer> sink
         = FilterSink.<Integer>create()
                     .predicate(i -> i % 2 == 0)
                     .metricsLabel("even_numbers")
                     .destination(destination)
                     .build()) {
    for (int i = 0; i < 1_000_000; i++) {
        sink.send(i);
    }
}
```

The metric is named `items.filtered` and will be labelled with an `items.type` of your provided label value, so in the
above example you'd get observations with `items.type=even_numbers`.