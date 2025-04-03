# Reject Sink

A `RejectSink` is a forwarding sink that only forwards inputs that match its filter condition and explicitly rejects
anything else by throwing a `SinkException` for any input that does not match the filter condition.  This differs from
the [`FilterSink`](filter.md) which merely drops items that don't match the filter condition.

Since this sink throws exceptions it should only be used in pipelines that are able to gracefully handle those
exceptions, e.g. by routing rejected items to a Dead Letter Queue.  Otherwise any rejected items will cause the pipeline
to be aborted which may lead to an application restart.

## Behaviours

- Forwarding
- Transforming: No
- Batching: No

## Parameters

This sink takes a destination `Sink<T>` and a filter condition expressed as a `Predicate<T>`.

If there is no provided filter condition then the filter condition defaults to forwarding all inputs i.e. it becomes
`t -> true`.

It can also optionally take a error message generator function - `Function<T, String>` - that can be used to customise
the rejection message on a per-input basis.  If this isn't set then a default error message of `Rejected` is produced
for all rejected inputs.

## Example Usage

In this example we create a `RejectSink` whose condition filters for even numbers and then send it a bunch of input
numbers.  Only those inputs that met the filter condition, i.e. were even numbers, will have been passed onto our
destination sink, anything else will throw an error which in this example we catch and print:

```java
NullSink<Integer> destination = NullSink.of();
try (RejectSink<Integer> sink 
        = RejectSink.<Integer>createRejecting()
                    .predicate(i -> i % 2 == 0)
                    .errorMessageGenerator(i -> i + " is not an even number")
                    .destination(destination)
                    .build()) {
    for (int i = 0; i < 1_000_000; i++) {
        try {
            sink.send(i);
        } catch (SinkException e) {
            System.out.println(e.getMessage());
        }
    }

    System.out.println("Found " + destination.count() + " even numbers");
}
```
**NB** As `RejectSink` extends `FilterSink` we need to use the `createRejecting()` method to get a builder for a
`RejectSink` as plain `create()` will given us a builder for a `FilterSink`.

## Metrics

A reject sink can optionally collect metrics about how many items it has filtered out, to use this functionality you
must create an instance with a `metricsLabel` value i.e.

```java
NullSink<Integer> destination = NullSink.of();
try (RejectSink<Integer> sink
         = RejectSink.<Integer>createRejecting()
                     .predicate(i -> i % 2 == 0)
                     .errorMessageGenerator(i -> i + " is not an even number")
                     .metricsLabel("even_numbers")
                     .destination(destination)
                     .build()) {
    for (int i = 0; i < 1_000_000; i++) {
        try {
            sink.send(i);
        } catch (SinkException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

The metric is named `items.filtered` and will be labelled with an `items.type` of your provided label value, so in the
above example you'd get observations with `items.type=even_numbers`.