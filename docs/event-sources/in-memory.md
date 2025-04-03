# In-Memory Event Source
**Repository:** `secure-agents-lib`  
**Description:** `In-Memory Event Source is an event source backed by a simple Queue`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The `InMemoryEventSource` is an event source backed by a simple `Queue`.  The events are populated when the source is
constructed and this is primarily intended for use in unit testing and debugging of data processing pipelines.

An in-memory event source is bounded, i.e. it represents a finite stream of events, thus it can be exhausted by polling
all the events it is populated with.

## Behaviours

- Bounded
- Buffered
- Configurable Read Policy: No

## Parameters

This source simply takes in a `Collection<Event<TKey, TValue>>` representing the events it should provide.

## Example Usage

In this example we create a source from a predefined list of events generating event keys as an integer sequence, the
contents of the event source are then printed out:

```java
List<String> events = Arrays.asList("a", "b", "c", "d", "e", "f");
AtomicInteger id = new AtomicInteger(0);
EventSource<Event<Integer, String>> source 
  = new InMemoryEventSource<>(events.stream().map(v -> new SimpleEvent(Collections.emptyList(), 
                                                                       id.incrementAndGet(), 
                                                                       v)));

while (true) {
  // Once exhausted stop reading
  if (source.isExhausted())
    break;

  Event<Integer, String> event = source.poll(Duration.ofSeconds(1));
  System.out.println(Integer.toString(event.key()) + ": " + event.value());
}
source.close();
```
© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.