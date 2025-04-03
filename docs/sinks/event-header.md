# Event Header Sink

The `EventHeaderSink` is a forwarding sink that takes in events and optionally adds some headers to the event prior to
outputting it.

Note that this sink is provided by the [`event-sources-lib` module](../event-sources/index.md#sinks) not the
`projectors-lib` module so requires a dependency on the `event-sources-lib` module to be available.

## Behaviours

- Forwarding
- Transforming: Yes
- Batching: No

## Parameters

It requires one/more header generator functions that take in the input event and decide if, and how, to add
a header.  These functions may return `null` if they do not wish to add a header to a given event.  The Builder API for
this sink offers a number of convenience methods for simple cases such that the user does not need to define the
functions themselves e.g.

- `fixedHeader("Name", "Value")` configures a generator function that adds a header with the given `Name` and `Value` to
  every event.
- `fixedHeaderIfMissing("Name", "Value")` configures a generator function that adds a header with the given `Name` and
  `Value` to events that don't yet have a `Name` header present.
- `addStandardHeaders("My-App")` configures several generator functions that populate the IANode standard
  `Exec-Patch`, `Request-Id` and `Input-Request-Id` headers.
    - You can also add these individually via the `addExecPath("My-App")`, `addRequestId()` and `addInputRequestId()`
      methods on the builder.
- `addDataSourceHeaders("Source", "text/csv")` configures generator functions that add the IANode standard
  `Data-Source-Name` and `Data-Source-Type` headers if they aren't already present on the event. 
- `addContentType("application/turtle")` configures a generator function that adds the IANode standard
  `Content-Type` header with the given value.

This sink also needs a destination sink to which it forwards the events it receives after adding any additional headers
as provided by the defined header generator functions.

## Example Usage

In this example we add all the IANode standard headers, set our `Content-Type` to `application/turtle` and add a
couple of custom headers, one with a fixed value and one with a generated value:

```java
Sink<Event<Integer, String>> destination = prepareDestination();

try (EventHeaderSink<Integer, String> sink 
        = EventHeaderSink.<Integer, String>create()
                   // IANode standard headers
                   .addStandardHeaders("example")
                   .addDataSourceHeaders("Cool Data Source", "text/csv")
                   .contentType("application/turtle")
                   // A fixed header Custom header
                   .fixedHeader("Custom", "example")
                   // A custom header generated based on the input event
                   .headerGenerator(e -> new Header("Square", Math.pow(e.key(), 2)))
                   .destination(destination)
                   .build()) {
    for (Event<Integer, String> input : someDataSource()) {
        sink.send(input);
    }
}
```
