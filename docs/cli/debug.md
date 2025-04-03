# `debug` CLI
**Repository:** `secure-agents-lib`  
**Description:** `Debug CLI module to build actual usable commands`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The `cli-debug` module contains some useful debugging commands that demonstrate the usage of the [`cli-api`](index.md)
module to build actual usable commands. You can invoke this via the [`./cli/cli-debug/debug`](../../cli/cli-debug/debug)
script to see the available commands:

```bash
$ ./cli/cli-debug/debug.sh
usage: debug.sh <command> [ <args> ]

Commands are:
    capture         Captures the contents of a topic to a directory without any interpretation of the the event contents.
    dump            Dumps the contents of a topic to the console assuming values can be treated as strings and ignoring keys
    fake-reporter   Creates a fake application that reports status heartbeats to IANode Live
    help            Displays help for a command, group, or the entire CLI.
    rdf-dump        Dumps the RDF from a Knowledge topic to standard output
    replay          Replays a previously obtained event capture back onto a Kafka topic without any interpretation of its contents.

See 'debug.sh help <command>' for more information on a specific command.
```

The following commands are available:

1. `help` which just displays help about the other commands.
2. [`dump`](#dump) which dumps the values from a Kafka topic as strings.
3. [`rdf-dump`](#rdf-dump) which prints out RDF from a Kafka topic.
4. [`capture`](#capture) which captures the contents of a Kafka topic to a directory of files.
5. [`replay`](#replay) which replays a capture back onto a Kafka topic.

## `dump`

The `dump` command can be used to view the contents of a topic as strings.  This assumes that the values in the 
topic can be usefully deserialized into strings.  If the values are some other format then this deserialization will 
just produce junk data.

```bash
$ ./cli/cli-debug/debug.sh dump --boostrap-server localhost:9092 --topic some-topic --limit 10
```

In this example we dump the next 10 events from topic `some-topic` to the console as strings.  Note that the events 
will be intermingled with logging output from the command, all logging goes to standard error so can be redirected 
elsewhere via normal shell redirects e.g. `2>log.txt`

## `rdf-dump`

The `rdf-dump` command can be used to view the RDF on a given topic:

```bash
$ ./cli/cli-debug/debug.sh rdf-dump --bootstrap-server localhost:9092 --topic ies-knowledge \
  --read-policy BEGINNING --limit 1
```

In this example we dump 1 event from `BEGINNING` of the `ies-knowlege` topic on the Kafka cluster at
`localhost:9092`. Note that the RDF will be intermingled with logging output from the command, all logging goes to
standard error so can be redirected elsewhere via normal shell redirects e.g. `2>log.txt`

The default output format is Turtle, but can be customised via the `--output-language <lang>` option where `<lang>`
is either a Jena recognised name or MIME type identifying the desired RDF format e.g.

```bash
$ ./cli/cli-debug/debug.sh rdf-dump --bootstrap-server localhost:9092 --topic ies-knowledge \
  --read-policy BEGINNING --limit 1 --output-language application/rdf+xml 2>/dev/null
```

Would output the same RDF graph as before but serialized as RDF/XML instead. This time with logging output thrown away
entirely.

## `capture`

The `capture` command takes a copy of the contents of a Kafka topic as a directory of files on disk with various
[formats](../event-sources/file.md#supported-formats) supported.  This is intended to help developers to capture the
state of a Kafka topic so that other developers can reproduce their setup easily:

```bash
$ ./cli/cli-debug/debug.sh capture --bootstrap-servers localhost:9092 --topic knowledge --max-stalls 1 \
    --poll-timeout 5  --read-policy BEGINNING --capture-directory /capture --capture-format yaml
```

In the above example we capture the contents of the `knowledge` topic to the directory `/capture` in our
[YAML](../event-sources/file.md#yaml) format.

**WARNING:** The `capture` command **DOES NOT** check whether the capture directory is empty, if the directory already
contains a previous event capture then running the command against then existing events captures in that directory will
be overwritten.

## `replay`

The `replay` commands takes a previously created event capture (created via the [`capture`](#capture) command) and
replays it back onto a Kafka topic.  This is intended to help developers quickly reproduce another developers setup:

```bash
$ ./cli/cli-debug/debug.sh replay --bootstrap-servers localhost:9092 --topic knowledge \
    --source-directory /capture --source-format yaml
```

In the above example we replay a capture from the `/capture` directory in our [YAML](../event-sources/file.md#yaml)
format back onto the `knowledge` topic of our Kafka cluster.

# Running via Docker

The `debug` CLI is packaged into a Docker image `docker.com/ianode/secure-agent-debug-tools`
so that developers don't have to build and run the `debug` CLI themselves.  This image makes the `debug` CLI its
entrypoint so any arguments passed to the `docker run` command will be passed to the `debug` CLI.  Thus, we could run the
`rdf-dump` command from the image like so:

```bash
$ docker run -i --rm docker.com/ianode/secure-agent-debug-tools rdf-dump \
       --bootstrap-servers host.docker.internal:9092 --topic knowledge \
       --max-stalls 1 --poll-timeout 5 \
       --read-policy BEGINNING >example.ttl
```

**NB:** Since this is running in a container we need to refer to our local Kafka cluster via `host.docker.internal:9092`
rather than `localhost:9092`.  You **MUST** also ensure that the `advertised.listeners` in your Kafka
`server.properties` does not explicitly pin the listener to `localhost`.

## Docker Convenience Scripts

As the full command invocations for running from Docker can be quite unwieldy we provide several convenience scripts in
this repository that wrap running the CLI with most of the options pre-populated for you.  All these scripts assume a
Kafka cluster at `host.docker.internal:9092` and a topic of `knowledge`, you can override these defaults by setting the
`BOOTSTRAP_SERVERS` and `TOPIC` environment variables.

### `capture-topic`

The `capture-topic` script wraps the [`capture`](#capture) command and requires just a capture directory, and optionally
a capture format to run:

```bash
$ ./capture-topic.sh /capture
```

Would capture the `knowledge` topic to the `/capture` directory.

Alternatively to capture in a specific [format](../event-sources/file.md#supported-formats):

```bash
$ ./capture-topic.sh /another-capture text
```

Would capture the `knowledge` topic to the `/another-capture` directory in our
[Plaintext](../event-sources/file.md#plain-text) format.

### `replay-topic`

The `replay-topic.sh` script wraps the [`replay`](#replay) command and requires just a capture directory, and optionally a capture format to run:

```bash
$ ./replay-topic.sh /another-capture text
```

Would replay the capture from `/another-capture` that is in our [Plaintext](../event-sources/file.md#plain-text) format
back to the `knowledge` topic.

### `rdf-dump-topic`

The `rdf-dump-topic.sh` scripts wraps the [`rdf-dump`](#rdf-dump) command and requires just an output file:

```bash
$ ./rdf-dump-topic.sh output.ttl
```

Would dump the contents of the `knowledge` topic to the file `output.ttl`.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.