# Design Overview
**Repository:** `secure-agents-lib`  
**Description:** `Outlines the use case, design requirements, and related work associated with the secure agent libraries`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

# Use Case
The secure-agent use case is relatively simple. A secure-agent reads events from an event source, converts those events
into an appropriate format for its underlying database and ingests the formatted data.

Thus, we can treat this as a linear data processing pipeline: Event Source -> Data Conversion -> Secure Agents
Database.

# Design Requirements and Goals
As with any good design we start from our requirements and goals. Given that this is a library for an internal audience
these are driven primarily by developers and application requirements, since Secure Agents ultimately expose data to
applications.

- Non-Functional Requirements
    - Ability to rapidly develop new Secure Agents
    - Minimize unnecessary code duplication between Secure Agents
- Functional Requirements
    - Read events from Kafka
        - Enable reading from alternative event sources in future
    - Automate Secure Agents projection pipelines
    - Must interoperate with other portions of Integration Architecture Node  that are not JVM based

## Non-Functional Requirements
Our requirements here are primarily driven by a desire to increase developer productivity. As a small start-up with
limited resources, both time and people, we want to enable our developers to be as productive as possible. Particularly
as we grow and bring in new developers we want to make it easy for them to be productive ASAP!

Providing shared libraries for common functionality and with a common design ethos reduces both cognitive overheads
for developers and provides a consistent experience whatever Secure Agents they might be working on.

## Functional Requirements
As a platform Integration Architecture Node  is built around Apache Kafka as an Event Stream so a key functional requirement is the
ability to consume events from that stream. Kafka is not the only option in this space, so we don't want to preclude
swapping out Kafka for alternatives in the future. Therefore, the API needs to abstract away from Kafka in so far as is
possible to enable this.

Similar to [IA Node maplib][1] we want to automate the common data processing patterns that we have in Integration Architecture Node  so
that developers can focus on the Secure Agents implementations rather than the machinery that runs them.

The final functional requirement is that we **MUST** be fully interoperable with other portions of Integration Architecture Node  that
are not JVM based. By this we mean that any Producer, Mapper, Projector etc. built with these libraries must be able to
be composed with other Integration Architecture Node  components running in Python, Go etc. In particular this means avoiding the use of any Java specific serialization of events, but in general avoiding anything that would prevent interoperability.

# Design Ethos
The libraries in this repository follow a pretty simple design ethos, they aim to be:

1. Simple
2. Extensible
3. Composable

So let's discuss what this ethos actually means in practise.

## Simple
All the APIs defined in these libraries are intended to be simple to understand and simple to use. That means they
should be as minimalist as possible i.e. define the minimum API contract necessary for each the intended use of each
API.

Additionally, their usage/intent should be obvious from the API e.g. by naming methods and parameters clearly. Wherever
the API itself cannot be self-explanatory it **MUST** be sufficiently well documented via Javadoc such that a developer
has the necessary documentation available to them.

Keeping an API simple makes it easier to build an Extensible and Composable API.

## Extensible
Having the API be extensible means two things:

1. It's easy to implement new instances of the API interfaces as needed
2. It's possible to build higher level APIs on top of the base APIs

By keeping interfaces simple it becomes very straightforward for developers to implement new instances of them, either
on a per-Secure Agents basis or to introduce new common functionality into these libraries when needed.

Plus by having simple APIs we can add implementation specific extensions to those APIs as necessary without needing to
worry about clashing with the base API.

## Composable
An API that is composable means that it is easy to compose the different APIs together to build higher level
functionality. Having simple APIs provide useful building blocks makes it very easy to connect these together into
complex applications.

As a prime example of this the [`EventSource`](event-sources/index.md), [`Sink`](sinks/index.md) and
[`Projector`](projection/index.md) APIs are all simple in of themselves, yet the
[`ProjectorDriver`](projection/driver.md) is able to compose these together to offer the API consumer a lot of
functionality with minimal effort on their part.

# Related Work
The concepts of this design, as outlined in the [Overview](index.md) document, are inspired by existing works.

Firstly our own [IA Node maplib][1] is a Python library that is used throughout the existing Producers, Mappers and
Projectors. It shares many of the same goals and concepts as these libraries, and in fact there has been some iterative
improvement of both `maplib` and these APIs driven by experiences of working with both. The main difference with these
libraries versus `maplib` is that we have more interfaces and classes because it's more Java-like to pass instances of
these around than relying on passing static function references. So this API is idiomatic Java in the same way that
`maplib` is idiomatic Python. Much of the early design and experimentation with this API was based upon mapping the
Python implementation into Java in a way that made sense for Java developers.

Secondly there is [Kafka Streams][2] which is a higher level DSL build on top of the low level Kafka APIs. It provides
similar capabilities to these libraries though considerably more generic. While it has a lot of nice features it also
locks us into the Kafka ecosystem much more closely and forces us to write components in Java since Streams is a JVM
only library. We have been actively researching Kafka Streams in relation to contract deliverables for some customers
and that research has identified that the majority of the value-add features of Streams are built on top of existing low
level Kafka capabilities. For example auto-scaling is based upon Consumer Groups (which both this API and `maplib`
already support). Even more complex features like multi-topic transactions are achieved by using the low level Producer
transaction API in combination with Consumer Group offset tracking. While this is not something currently supported in
either this API or `maplib` neither is precluded from enabling this capability in the future.

Additionally, while there are lots of existing data processing frameworks, e.g. Apache Spark and Apache Flink to name
just two, they are primarily more generic and tend to be focused on distributed processing. While being generic is not
in of itself a bad thing it does inherently increase the cognitive overhead of using it. In a challenging recruiting
environment adding yet another job requirement to the list only reduces our pool of potential candidates. Plus by its
very nature a larger framework implies more machinery and complexity that makes a developers job of developing and
testing data processing pipelines harder. As noted in the [Use Case](#use-case) a Secure Agents implementation is a linear
data processing pipeline. These frameworks typically allow for much richer data processing pipelines, typically some
form of [Directed Acyclic Graph (DAG)][3], that we simply don't need currently. Note that the Integration Architecture Node  platform as a whole is certainly representable as a DAG but the individual components, such as Secure Agents, within the platform don't themselves need the full power of a DAG based data processing framework to function.

Also, there is the key design assumption of Integration Architecture Node  to consider, that its topics are a continuous stream of changes that needs to be processed in-order. The distributed processing, of the kind offered by these frameworks, does not hold any obvious benefit for us because in distributing the workload you'd either have out-of-order processing of events, or sufficient coordination overheads to offset the benefits. It should be noted that Kafka Consumer Groups already gives us the capability to scale up to multiple instances of a Secure Agents if necessary. However, even then we would have work to do to ensure in-order processing of events across multiple instances of a Secure Agents.


[1]: https://github.com/National-Digital-Twin/map-lib

[2]: https://kafka.apache.org/documentation/streams/

[3]: https://hazelcast.com/glossary/directed-acyclic-graph/

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.
