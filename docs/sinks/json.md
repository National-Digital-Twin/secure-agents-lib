# JSON Serialization

The `JacksonJsonSink` is a terminal sink that serializes its inputs to JSON and outputs them to a configured
`OutputStream`, the JSON may optionally be pretty printed.

This sink takes an `Object` as the input meaning it can be used to serialize the outputs of any pipeline into JSON.

## Behaviours

- Terminal
- Transforming: Yes
- Batching: No

## Parameters

This sink takes an optional `OutputStream` and a boolean indicating whether to pretty print the outputs.

If these are not supplied then it defaults to Standard Output (i.e. `System.out`) and pretty printing defaults to
being disabled.

## Example Usage

In this example we pretty print the outputs to a file:

```java
try (FileOutputStream output = new FileOutputStream("example.json")) {
    try (JacksonJsonSink<SomeObject> sink 
            = JacksonJsonSink.<SomeObject>create()
                             .toStream(output)
                             .prettyPrint(true)
                             .build()) {
        for (SomeObject input : someDataSource()) {
            sink.send(input);
        }
    }
}
```

Would print a stream of JSON objects to `example.json`