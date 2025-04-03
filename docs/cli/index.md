# CLI
**Repository:** `secure-agents-lib`  
**Description:** `CLI modules provide machinery that aids in creating CLI entrypoints for Secure Agents pipelines`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The CLI modules provide machinery that aids in creating CLI entrypoints for Secure Agents pipelines. There is a
`cli-api` module that provides the actual API for this, and a `cli-debug` module which provides a `debug` CLI with a
couple of example commands available.

# API

The actual CLI parsing machinery is provided by the [Airline][1] library, the `cli-api` module basically handles the
dependency management for this and provides useful base classes.

All commands should be derived from the `SecureAgentCommand` class, this automatically adds the `-h/--help` option to
these commands, and defines an abstract `int run()` method that commands must implement. It also has some useful static
helper methods:

- `SecureAgentCommand.runAsSingleCommand(Class, String[])` - Can be called to automate creating a parser, invoking it and
  running the command. This is for CLIs with a single entrypoint i.e. a single command.
- `SecureAgentCommand.handleParseResult(ParseResult)` - Can be called with a `ParseResult` to automate either displaying
  help, parser errors or running the command that parsing selected. This is for use with CLIs with multiple entrypoints
  i.e. a CLI with multiple sub-commands (`git` being an obvious example developers will be familiar with).

# Usage

Assuming you have a command class that derives from `SecureAgentCommand`, and is suitably annotated with the Airline
[`@Command`][2], then you can add a simple `main()` method like so:

```java
import com.github.rvesse.airline.annotations.Command;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;

@Command(name = "example", description = "An example command")
public class Example extends SecureAgentCommand {

    // Any command options/arguments you need are defined via annotated fields here

    // A simple main method that just calls the relevant static method from SecureAgentCommand
    public static void main(String[] args) {
        SecureAgentCommand.runAsSingleCommand(Example.class, args);
    }

    @Override
    public int run() {
        // Actual command logic goes here

        // Return the desired exit code based on command success/failure
        return 0;
    }
}
```

This will generate a parser for your command, parse the users inputs and populate an instance of your command class, in
this case `Example`, before calling its `run()` method where your actual command logic happens. If the user has
requested help then that would be shown. Or if the user failed to provide options/arguments that your command declared
as required (or with other restrictions upon their values) then relevant error messages are shown instead.

Any command derived from `SecureAgentCommand` will automatically have the following options available:

- `-h`/`--help` - Requests that help for a command be displayed.
- Several options related to reconfiguring application logging which are detailed under [Logging
  Options](#logging-options).
- Several options related to [Live Reporter](../live-reporter/index.md) which are detailed under [Live Reported
  Support](#live-reporter-support).

If you invoke your actual command via the static helper methods shown in the above example then these options are
automatically processed and acted upon as appropriate.

A number of abstract base classes are also provided for common patterns e.g. a command that applies a projector to an
event source:

- `AbstractProjectorCommand` - Abstract base class for commands that apply a projector
    - `AbstractKafkaProjectorCommand` - Abstract base class for commands that apply a projector and support Kafka as an
      event source
       - `AbstractKafkaRdfProjectionCommand` - Abstract base class for commands that apply a projector to a Kafka
         knowledge topic whose event values are RDF Datasets

Using these base classes will add a bunch of additional options to your commands relevant to those commands e.g.
`--bootstrap-server` for specifying Kafka bootstrap servers.

Note that it is also possible to write unit tests against your commands, see the later section on
[Testing](#testing-commands) for how to do this.

## Logging Options

The `SecureAgentCommand` class includes the `LoggingOptions` module which provides the following three options:

- `--quiet` - Reduces log verbosity to only `WARN` and `ERROR` messages.
- `--verbose` - Increases log verbosity to include `DEBUG` messages.
- `--trace` - Increases log verbosity to include `TRACE` messages.

If multiple of these options are supplied then the most verbose logging level would apply.

When these options are used they cause the root logger level to be adjusted as noted above.  Since **ONLY** the root
logger level is modified if your application wants to enforce a certain log level on a specific logger it can do that
via its Logback configuration file.  This will be honoured regardless of whether a user supplies a logging option
because only the root loggers level is modified.

## Kafka Connectivity Options

The `KafkaOptions` module provides all the options around connecting to Kafka:

| Option(s) | Purpose | Environment Variable                       | Default Value |
|-----------|---------|--------------------------------------------|---------------|
| `--bootstrap-server`/`--bootstrap-servers`| Specifies the Kafka Bootstrap servers | `BOOTSTRAP_SERVERS`                        | |
| `-t`/`--topic` | Specifies the Kafka topic(s) to read from | `TOPIC`/`INPUT_TOPIC`                      | |
| `--dlq`/`--dlq-topic` | Specifies the Kafka topic to use as the DLQ | `DLQ_TOPIC`                                | |
| `-g`/`--group` | Specifies the Kafka consumer group to use |                                            | Name of the CLI command |
| `--lag-report-interval` | Specifies how often (in seconds) that the lag on read topics is checked and reported in logs |                                            | `30` |
| `--kafka-max-poll-records` | Specifies the maximum number of records to poll from Kafka at once |                                            | `1000` |
| `--kafka-user`/`--kafka-username` | Specifies a username for authenticating to Kafka | `KAFKA_USER`                               | |
| `--kafka-password` | Specifies a password for authenticating to Kafka | `KAFKA_PASSWORD`                           | |
| `--kafka-login-type` | Specifies the login type to use for username and password authentication to Kafka |                                            | `PLAIN` |
| `--kafka-properties` | Specifies a properties file containing additional Kafka configuration properties, useful for more advanced authentication methods like mTLS | `KAFKA_CONFIG_FILE_PATH`/`KAFKA_PROPERTIES` | |
| `--kafka-property` | Specifies a single Kafka configuration property directly on the command line |                                            | |
| `--read-policy` | Specifies how to read from Kafka topics |                                            | `EARLIEST` |

If you are deriving from one of the abstract commands with `Kafka` in its name then this will be accessible via the
protected `kafka` field in your command class.  Command code can access the various Kafka configuration either directly
via public fields, or via public methods where additional computation is needed e.g.

```java
KafkaEventSource<Bytes,String> source
  = KafkaEventSource
        .<Bytes, String>create()
        .keyDeserializer(BytesDeserializer.class)
        .valueDeserializer(StringDeserializer.class)
        .bootstrapServers(this.kafka.bootstrapServers)
        .topics(this.kafka.topics)
        .consumerGroup(this.kafka.getConsumerGroup())
        .consumerConfig(this.kafka.getAdditionalProperties())
        .maxPollRecords(this.kafka.getMaxPollRecords())
        .readPolicy(this.kafka.readPolicy.toReadPolicy())
        .lagReportInterval(this.kafka.getLagReportInterval())
        .build();
```

Importantly you **MUST** ensure that you are passing in the additional properties from `getAdditionalProperties()` via
the appropriate `consumerConfig()`/`producerConfig()` method of our builders, or however you are using Kafka Client APIs
as this may contain complex configuration necessary for communicating with secure Kafka clusters.

### Kafka SASL Authentication

Commands built with this framework can easily communicate with a Kafka cluster using SASL authentication since the user
generally only needs to provide the `--kafka-user` and `--kafka-password` (or equivalent environment variables per the
earlier table) in order to authenticate the command to the cluster.  `KafkaOptions` takes care of populating the
`Properties` returned by `getAdditionalProperties()` with suitable configuration to enable that authentication to take
place.

Note that if the cluster is using one of the SASL-SCRAM authentication mechanisms that Kafka supports the user will need
to specify `--kafka-login-type SCRAM-SHA-256` or `--kafka-login-type SCRAM-SHA-512` as appropriate.  For SASL-SCRAM they
may also need additional SSL related configuration to ensure that it is provided a trust store that allows it to trust
the broker(s) SSL certificates and establish an encrypted channel over which authentication may occur.  The trust store
should contain at least the CA Root Certificate that signed the broker certificate, and ideally for maximum security the
certificates of the individual brokers.

**NB** If using a managed Kafka Cloud Service, e.g. Confluent Cloud, Amazon MSK etc, then your Kafka Brokers may already
be using certificates that are signed by CAs that the application already trusts and this configuration is unnecessary.

This may be provided either directly via the `--kafka-property` option e.g. `--kafka-property
ssl.truststore.location=/path/to/truststore --kafka-property ssl.truststore.password=<password>`, or since it will
involve sensitive values, may be provided indirectly via a properties file using `--kafka-properties client.properties`
e.g.

```
ssl.truststore.location=/path/to/client-truststore
ssl.truststore.password=<password>
```

In either case the `<password>` placeholders should be suitably replaced with the necessary password for the Trust
Store.

If a user prefers they can just provide all this configuration via a properties file e.g.

```
security.protocol=SASL_SCRAM
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="<username>" password="<password>"
```

For other SASL mechanisms, e.g. `OAUTHBEARER`, that require substantially more configuration then `--kafka-properties`
should be used to supply a properties file with the necessary configuration.  Please refer to the official [Kafka
Configuration](https://kafka.apache.org/documentation/#security_jaas_client) documentation for different variations that
are supported.

#### Testing SASL enabled clusters

Developers can spin up a SASL Plaintext secured Kafka cluster as described in the [Testing
Kafka](../event-sources/kafka.md#testing-secure-clusters) documentation.

### Kafka mTLS Authentication

Communicating with a Kafka cluster using mTLS authentication is somewhat more involved as the user needs to provide SSL
Trust and Key stores, and the credentials for those, in order to enable the mTLS authentication.  For this users should
supply a properties file via `--kafka-properties client.properties` with appropriate configuration e.g.

```
security.protocol=SSL
ssl.truststore.location=/path/to/client-truststore
ssl.truststore.password=<truststore-password>
ssl.keystore.location=/path/to/client-keystore
ssl.keystore.password=<keystore-password>
ssl.key.password=<key-password>
```

Where the relevant password placeholders are replaced with the appropriate passwords for the users environment.

**NB** If using a managed Kafka Cloud Service, e.g. Confluent Cloud, Amazon MSK etc, then your Kafka Brokers may already
be using certificates that are signed by CAs that the application already trusts and the `ssl.trustore` configuration is
unnecessary.

Note that this is not the only possible way to configure mTLS authentication for Kafka, please refer to the [Kafka
Configuration](https://kafka.apache.org/documentation/#security_configclients) documentation for the different
variations that are supported.

#### Testing mTLS enabled clusters

Developers can spin up a mTLS secured Kafka cluster as described in the [Testing
Kafka](../event-sources/kafka.md#mtls-authentication) documentation.

If the cluster is a test cluster, or deployed in an environment where the certificates don't/can't contain the hostnames
of the brokers and clients, then the following additional configuration will be necessary:

```
# Explicitly disable hostname verification
ssl.endpoint.identification.algorithm=
```

## Live Reporter support

The `cli-api` module integrates [Live Reporter](../live-reporter/index.md) support, meaning that commands are able to
generate heartbeats that are reported to IANode Live to help in visualizing the state of the Integration Architecture Node  Platform.

Several options are provided to all commands derived from `SecureAgentCommand` to allow end users to configure Live
Reporter as desired:

- `--live-reporter`/`--no-live-reporter` - Enables/Disables the live reporter functionality.
- `--live-reporter-topic <topic>` - Specifies the topic to which live heartbeats are sent, defaults to `provenance.live`
- `--live-report-interval/--live-reporter-interval <seconds>` - Specifies the heartbeat interval in seconds, defaults to
  15 seconds.
- `--live-bootstrap-server/--live-bootstrap-servers <bootstrap-servers>` - Specifies the Kafka cluster to which
  heartbeats are sent.

In order to actually configure the live reporter a command must override the `setupLiveReporter()` method and in that
implementation call `this.liveReporter.setupLiveReporter()` passing in suitable parameters for your application.
Assuming you are calling `SecureAgentCommand.runAsSingleCommand()` as suggested above then the `setupLiveReporter()`
method will be called for you so provided you override it appropriately then the reporter will be setup.  Conversely
provided you have called `this.liveReporter.setupLiveReporter()` the base implementation will also make a best effort to
terminate the live reporter appropriately when the application exits.

For an example implementation of this take a look at `AbstactKafkaProjectionCommand`.

## Health Probe Server support

The `cli-api` module can optionally provide a [Health Probe Server](health-probes.md) meaning that commands that
wouldn't normally have an HTTP interface can provide useful liveness and readiness probes with minimal additional coding.

To enable this support you need to add the `HealthProbeServerOptions` to your command class e.g.

```java
@AirlineModule
private HealthProbeServerOptions healthProbes = new HealthProbeServerOptions();
```

If your command class derives from `AbstractProjectorCommand` then it will have these options available by default and
there's no need to manually add them.  You merely need to override the `Supplier<HealthStatus> getHealthProbeSupplier()`
method to return a supplier function that can calculate the readiness of your application.

This options class provides a `setupHealthProbeServer()` method that is used to configure and start the health probe
server, it requires a display name, a health status supplier and optionally a list of library version information you
wish to expose via the liveness probe.  Again, if deriving from `AbstractProjectorCommand` then this is automatically
called for you.

By default, the health probe server runs on port `10101`. This can be configured by the user via the
`--health-probe-port` option.  The user can also choose to disable health probes entirely via the `--no-health-probes`
option.

There is also a corresponding `teardownHealthProbeServer()` method that should be used to terminate the server when you
are done with it, again commands derived from `AbstractProjectorCommand` will call this automatically.  Note that a
health probe server always registers a JVM Shutdown Hook so attempts to shut itself down when the JVM exits gracefully
so it isn't essential to explicitly call the teardown method.

# Dependency

The `cli-api` module provides the base command classes and machinery, it can be depended on from Maven like so:

```xml
<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>cli-api</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information.

# Implementations

## `debug` CLI

The `cli-debug` module provides an example CLI with developer debugging tools, see the [`debug`](debug.md) documentation
for more details.

## Other Implementations

Other implementations based on `cli-api` may be found in other repositories, for example the [Search Secure Agents][3]
repository provides an `elastic-index` command.

## Testing Commands

The `cli-api` module also includes the ability to write unit tests around command implementations by using the `tests`
dependency i.e.

```xml
<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>cli-api</artifactId>
    <version>VERSION</version>
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>
```

With this module as a test dependency you can use the `SecureAgentCommandTester` class to help in writing unit tests.
The key thing is to call the `setup()`, `resetTestState()` and `teardown()` methods in your test class e.g.

```java
public class TestExampleCommand {

    @BeforeClass
    public void setup() {
        // Enables command testing mode including output stream capture
        SecureAgentCommandTester.setup();
    }

    @AfterMethod
    public void testCleanup() {
        // Resets test state by discarding previously captured state
        SecureAgentCommandTester.resetTestState();
    }

    @AfterClass
    public void teardown() {
        // Disables command testing mode including restoring original output streams
        SecureAgentCommandTester.teardown();
    }

    @Test
    public void example_01() {
        // Invoke your commands main() method with any arguments you want to parse it to test
        ExampleCommand.main(new String[] { "--flag", "--option", "value"});

        // Get the parse result and make assertions about it...
        ParseResult<ExampleCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertTrue(result.wasSuccessful());

        // Get the exit status and make assertions about it...
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 2);

        String lastStdOut = SecureAgentCommandTester.getLastStdOut();
        // Make some assertions about standard output contents...
        String lastStdErr = SecureAgentCommandTester.getLastStdErr();
        // Make some assertions about standard error contents...
    }
```

Note that if your tests don't need any additional setup you can choose to derive your test class from
`AbstractCommandHelper` in this module and those setup and teardown methods will already be done for you.


[1]: https://github.com/rvesse/airline
[2]: http://rvesse.github.io/airline/guide/annotations/command.html
[3]: https://github.com/National-Digital-Twin/secure-agent-knowledge-search

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.
