# Throughput Sink

The `ThroughputSink` is a forwarding sink that tracks and reports throughput metrics for a pipeline.  Every input is
counted as it is received and then processed by the destination sink and the elapsed time tracked.  Elapsed time
encompasses the destination sink processing time thus represents the full processing time for the portion of the
pipeline that the sink wraps.

Throughput metrics are reported based on a reporting batch size i.e. metrics are only reported when sufficient inputs
have been seen.  Currently, these metrics are only reporting by logging them, future iterations of this API may expand
this capability, however there is some control over how the metrics are presented in the log output.

Note that the actual tracking and reporting is actually provided via a separate class `ThroughputTracker` that can also
be reused anywhere else you want throughput tracking independently of the `Sink` API itself.

A `ThroughputSink` should always be used in a try with resources block, or explicitly have `close()` called upon it, so
that it can report the final throughput metrics if processing does not finish exactly on a reporting batch size
boundary.

## Behaviours

- Forwarding
- Transforming: No
- Batching: Yes

## Parameters

At a minimum this sink requires a destination `Sink` and a reporting batch size expressed in terms of number of inputs.
For example a reporting batch size of 100,000 means throughput metrics are reported every 100,000 inputs.

Optionally you may also provide parameters to control how the log lines reporting the metrics are formatted.  This
includes the action string, the time unit in which elapsed time is reported and the name by which to refer to the
inputs.  When not configured these parameters default to `Processed`, `TimeUnit.MILLISECONDS` and `items` respectively.

## Example Usage

In this example we see both basic and fully configured sinks used to report the throughput statistics at different
intervals and in different time units:

```java
NullSink<String> terminal = NullSink.of();
try (ThroughputSink<String> coarseMetrics 
        = ThroughputSink.<String>create()
                        .tracker(t -> t.reportInterval(1_000_000)
                                        .inMinutes()
                                        .action("Discarded")
                                        .itemsName("strings")
                                        .metricsLabel("my_metric"))
                        .throughput(x -> x.tracker(y -> y.reportInterval(10_000)
                                                         .discard()))) {
    for (T input : someDataSource()) {
        coarseMetrics.send(input);
    }
}

```

And this would produce logging like the following (most logging fields and lines omitted for brevity):

```
Processed 10,000 items in 1,030 milliseconds at 9.709 items/milliseconds
Processed 20,000 items in 2,134 milliseconds at 9.372 items/milliseconds
...
Processed 1,000,000 items in 9,458,000 milliseconds at 9.458 items/milliseconds
Discarded 1,000,000 strings in 158 minutes at 6,329.114 strings/minutes
```

Note that as shown in the above example output a `ThroughputSink` only reports throughput after inputs have been
forwarded onto the destination sink for processing.  Therefore, the outermost sink in a pipeline (which in the above
example is the sink from the innermost try with resources block) will be the one that reports metrics last.

# Metrics

A `ThroughputSink` can optionally collect metrics if the `metricsLabel` argument is passed into its
constructor as seen in the earlier example where it was given as `my_metric`.  The following metrics are collected:

- `items.received` - A counter indicating total items received.
- `items.processed` - A counter indicating total items processed.
- `items.processing_rate` - A gauge indicating overall processing rate in terms of the configured time
  reporting unit.

For each metric the actual observations will be labelled with an `items.type` label that has the metric label value you
passed into the constructor.  So in our earlier example each of these metrics would have observations with
`items.type=my_metric` collected.  This allows for multiple `ThroughputSink` or `ThroughputTracker` instances within an
application to collect different levels of throughput metric.