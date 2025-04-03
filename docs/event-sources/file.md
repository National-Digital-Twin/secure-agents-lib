# File Based Event Source
**Repository:** `secure-agents-lib`  
**Description:** `File Based Event Source which is a file-based event source that replays events from files`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The `event-source-file` module provides the `FileEventSource` which is a file-based event source that replays events
from files.  These can either be hand-crafted files or these can be captured from another event source using a
`EventCapturingSink`.  This is primarily intended for use in testing and debugging scenarios where you want/need to
remove the extra complexity of Kafka.

This event source works by taking in a source directory, scanning that for event files and sorting those into order.
This operation happens **ONCE** when the source is created, i.e. this source will not actively see new event files that
get added to the directory.  As events are polled from the source the next file is read in as an `Event` and returned.
Once the detected files are exhausted the source reports itself as exhausted.  Each concrete implementation specifies
its own logic as to which files within the source directory are considered to be events, usually by filtering upon file
extension and name.

These event sources are designed to re-use the Kafka `Serializer` and `Deserializer` implementations from the
[Kafka](kafka.md) event source.  This ensures that while there are some format specific differences in the actual
on-disk serialization of events the actual code paths for serialization and deserialization of event keys and values are
identical to using a real source.

The `FileEventFormatProvider` interface provides a means to define a supported format and create instances of the
various implementation classes used to implement file-based event sources.  Providers may be retrieved via the
`FileEventFormats.get()` method, currently we support three formats:

| Format Name | Headers | Keys | Values | Notes |
|-------------|---------|------|--------|-------|
| `yaml`      | Yes     | Yes  | Yes    | The preferred format, allows for full preservation and round tripping of events. |
| `text`      | Yes     | No   | Yes    | Simpler format that does not include keys. |
| `rdf`       | `Content-Type` inferred from file extension | No | Yes | Intended only to allow raw RDF files to be directly loaded. |

A more detailed description of each format is given [later](#supported-formats) in this document.

## Behaviours

- Bounded
- Unbuffered
- Configurable Read Policy: No

## Parameters

It requires a source directory (which must exist) and the deserializer classes for the key and value types stored in the
event files.

## Example Usage

You should not use the `FileEventSource` or its derived implementations directly, rather you should use the
aforementioned `FileEventFormats` registry and the `FileEventFormatProvider` interface:


```java
FileEventFormatProvider format = FileEventFormats.get(YamlFormat.EVENT_FORMAT_NAME);
EventSource<Integer, String> source 
    = format.createSource(new IntegerDeserializer(), 
                          new StringDeserializer(), 
                          new File("some-dir"));
```

It requires a directory from which events will be read as well as the deserializers for the key and
value types.

The YAML format looks for files with a `.yaml` extension in the source directory that have a numeric portion in the
filename e.g. `event-12345.yaml`.  Detected files are sorted based upon the numeric portion of the filename, in the
event that two files have the same numeric portion then they are lexicographically sorted based on their absolute paths.

Alternatively if you have a single file you wish to use as input you can call the `createSingleFileSource()` method
instead:

```java
FileEventFormatProvider format = FileEventFormats.get(YamlFormat.EVENT_FORMAT_NAME);
EventSource<Integer, String> source 
    = format.createSingleFileSource(new IntegerDeserializer(), 
                                    new StringDeserializer(), 
                                    new File("some-file.yaml"));
```

A single file source, as the name suggests, will only yield a single event from the given file.  Additionally, any
restrictions that a format usually applies to file naming do not apply, so in the above example we did not need to have
a numeric portion in the filename.

## Supported Formats

As mentioned earlier several different file formats are supported, here we describe the behaviour of each of them in
more detail.

### YAML

The YAML format is our preferred format for file-based event sources.  The format allows for full preservation and
round-tripping of events.  An example event is given below:

```yaml
---
headers:
- key: "Content-Type"
  value: "application/n-quads"
- key: "Security-Label"
  value: "clearance=TS"
key: "AAAAAQ=="
value: "PGh0dHA6Ly9zdWJqZWN0PiA8aHR0cDovL3ByZWRpY2F0ZT4gPGh0dHA6Ly9vYmplY3Q+IC4KPGh0dHA6Ly9vdGhlcj4gPGh0dHA6Ly9wcmVkaWNhdGU+ICJ2YWx1ZSIgLgo="
```

The `headers` field provides a list of key-value pairs representing the headers of the event.  Note that this is a list,
not a map, since the same header key may appear multiple times in the headers, potentially with a different value each
time.

The `key` and `value` fields provide Base 64 encoded strings that encode the `byte[]` sequences generated by the Kafka
`Serializer`'s for the events key and value type.

As noted earlier a YAML event source expects all the files to be named with a `.yaml` extension and have a numeric
portion in their filename.

### Plain Text

The plain text format is a simpler format that does not preserve event keys but still fully preserves headers and
values.  The same YAML event shown above can be represented in plain text as follows:

```
Content-Type: application/n-quads
Security-Label: clearance=TS

<http://other> <http://predicate> "value" .
<http://subject> <http://predicate> <http://object> .
```

This format has a series of header lines written in single line HTTP style i.e. `<key>: <value>` followed by a blank
line to signify the end of the headers.  The remainder of the file then contains the value, this will be the `byte[]`
sequence generated by the Kafka `Serializer` for the event value type.  Note that this may mean that the value portion
of the file is not human-readable depending on the type and serializer in use.

A plain text event source expects all the files to be named with a `.txt` extension and have a numeric portion in their
filename.

### RDF

The RDF format is the simplest format in that it is intended to allow the loading of existing well-formed RDF files
without needing to transform them in any way.  This also makes it the most limited format as you cannot supply headers
nor a key for the events.

This format simply takes in any files that have a recognised RDF file extension, using that extension to infer
`Content-Type` header for the event which our RDF Kafka serializers and deserializers use to parse the event value
correctly.

The same event seen in our other formats could be provided as a simple NQuads file e.g. `example1.nq`:

```
<http://other> <http://predicate> "value" .
<http://subject> <http://predicate> <http://object> .
```

When used as a directory source the usual behaviours apply in that each RDF filename must contain a numeric portion in
order to sort the input files into the desired input event order.  However, the RDF files themselves may be in a mixture
of RDF formats provided each file has a recognised file extension.

### Adding additional formats

As already noted earlier a file event format is provided by a `FileEventFormatProvider`, this is a `ServiceLoader`
discovered API whose registered implementations are accessed via the `FileEventFormats` static registry class.  If you
want to add a new event file format you need to implement this interface and all its methods, and create a suitable
entry in a new/existing `META-INF/services` file for this interface.

## Capturing Events for Replay

You can capture events to files for later replay using the `EventCapturingSink`, like all sinks this is built via a
builder pattern:

```java
EventCapturingSink<Integer, String> sink
    = EventCapturingSink.<Integer, String>create()
                        .directory(new File("capture-dir"))
                        .prefix("events-")
                        .extension(".yaml")
                        .padding(5)
                        .writeYaml(y -> y.keySerializer(new IntegerSerializer())
                                         .valueSerializer(new StringSerializer()))
                        .discard()
                        .build();
```

Would capture events to `capture-dir` naming the files `events-<num>.yaml` where `<num>` is padded to a minimum of 5
characters using leading `0`s.

The resulting `Sink` instance can then be inserted into your processing pipeline at the point where you want to capture
events.

## Generating Events Manually

If you want to generate some events manually for later reuse as test data you can do this by using the
`FileEventFormatProvider` you retrieved earlier to get a `FileEventWriter` instance e.g.

```java
FileEventWriter<Integer, String> writer 
    = format.createWriter(new IntegerSerializer(), new StringSerializer());

// Generate events as desired
writer.write(new SimpleEvent(Collections.emptyList(), 5678, "example event value"), new File("example1.yaml"));
```
© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.