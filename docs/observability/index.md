# Observability Lib

The Observability Lib module provides helper utilities around using [Open Telemetry][OTEL] to add metrics and traces to your
applications.  It is also used by many of our other modules to help in creating and tracking their own metrics.

The `IANodeMetrics` class contains static utilities that act as the entry point for doing various metric related
operations which are detailed in the following sections.

## Accessing the Open Telemetry instance

The `get()` and `set()` methods allow for accessing an `OpenTelemetry` instance, if one is not explicitly configured
then this returns the result of `GlobalOpenTelemetry.get()`.  Generally you will not need to configure an instance
yourself, except for in [testing contexts](#testing-open-telemetry-metrics).

If you are using the [Open Telemetry Java Agent](#using-the-java-agent) then you will not need to set this yourself as
this will already be set for you as part of the Agent auto-configuration.

## Obtaining an Open Telemetry Meter

An Open Telemetry `Meter` instance can be obtained via the `IANodeMetrics.getMeter()` method, there are two overloads
of this provided:

- `getMeter(String name)`
- `getMeter(String name, String version)`

A `Meter` instance has some attributes associated with it identifying the library that is generating the metrics.  These
above overloads allow specifying either just the library name, or the library name and version.  If the version is not
specified then we attempt to auto-detect its [version information](#detecting-version-information).

Once you have a `Meter` instance you can use it to create the actual Open Telemetry [metric instances][1] via the
various methods on the `Meter`.

## Timing Tasks

If you are collecting timing metrics for tasks the `IANodeMetrics.time()` method can be used to time a `Runnable` or
`Callable<T>`.  This method takes a `DoubleHistogram` and an `Attributes` instance to use in recording the timing along
with the actual task to run e.g.

```java
IANodeMetrics.time(myHistogram, myAttributes, () -> doSomeLongTask());
```

Timings are recorded in fractional seconds.  If you want to time your tasks using a different unit then you will need to
provide your own logic for that.

## Detecting Version Information

The module also includes a mechanism for detecting version information for libraries.  This relies upon each library
putting a `/<library-name>.version` file in the root of their class path.  This is a Java properties file that **MUST**
contain at least a `version` property containing the version string for the library.

For example:

```
#Created by build system. Do not modify
#Wed Nov 23 14:14:06 GMT 2022
name=IANode Secure Agents - Observability Lib API
version=0.9.0-SNAPSHOT
revision=69ab517ed4e270c37f0dbf054abfe917e3d202f9
```

You can obtain the version for a library via `LibraryVersion.get("my-library")` assuming it has a valid `/my-library.version` as described above. If this cannot be detected successfully
then `unknown` will be returned instead.

You can also call `LibraryVersion.getProperties("my-library")` to get the `Properties` detected for a library.  This
gives you the full contents of the `/my-library.version` file which you can use in whatever way you see fit.  For
example the [`jaxrs-base-server`](../jaxrs-base-server/index.md) module includes a `/version-info` endpoint in your
application that allows querying this information.

All the modules in these libraries provide this version information and can be looked up based upon their Maven
Artifact IDs assuming that those libraries are present on your applications classpath.

You can generate this file for your own applications using the [Maven Build Number Plugin][2] like so:

```xml
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>buildnumber-maven-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>create-metadata</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <doCheck>true</doCheck>
                    <doUpdate>true</doUpdate>
                    <outputName>${project.artifactId}.version</outputName>
                    <addOutputDirectoryToResources>true</addOutputDirectoryToResources>
                </configuration>
            </plugin>
```

Note that the `LibraryVersion` class caches the detected version information, if you want to see the list of libraries
that have been cached you can call `LibraryVersion.cachedLibraries()` to get a `Set<String>`.

## Testing Open Telemetry Metrics

When developing applications that expose their own Open Telemetry metrics it can be useful to test that metrics are
actually being collected as desired.  The `projectors-lib` module has a `TestUtils` class in its `tests` classifier
artifact that can be used to help with this.  To use this you will first need to add additional [test
dependencies](#test-dependencies) to your Maven project.

The `TestUtils` class has a `enableMetricsCapture()` and `disableMetricsCapture()` methods for enabling and disabling
metrics capture.  Typically, you would call these from the setup and teardown methods of your tests i.e. methods
annotated with `@BeforeMethod` and `@AfterMethod` or equivalent e.g.

```java
@BeforeMethod
public void setupMetrics() {
    TestUtils.enableMetricsCapture();
}

@AfterMethod
public void teardownMetrics() {
    TestUtils.disableMetricsCapture();
}
```

The `getReportedMetric()` method allows for obtaining the collected value for a given metric which you can then compare
against the expected value to ensure your metrics code is behaving as expected.  There are several overloads depending
on whether the reported metric is labelled with additional attributes.

- `TestUtils.getReportedMetric("my.metric")` - Gets an unlabelled metric.
- `TestUtils.getReportedMetric("my.metric", "my.attribute", "value")` - Gets a metric labelled with a single attribute.
- `TestUtils.getReportedMetric("my.metric", myAttributes)` - Gets a metric labelled with multiple attributes.

If there is no such metric found, or you forgot to enable metrics capture, then these methods throw an
`IllegalStateException`, otherwise the `Double` value of the reported metric is retrieved.

# Using the Java Agent

The easiest way to actually export metrics from a Java application is to use the [Java Agent][3] to provide automatic
instrumentation of your application and exports it via Open Telemetry Protocol (OTLP).  You can then run an Open Telemetry collector on the default port with a OTLP receiver to collect those exported metrics.

At a minimum you need to ensure that when your application is started an appropriate `-javaagent` option is added
pointing to the Open Telemetry agent i.e. `-javaagent:path/to/opentelemetry-javaagent.jar`.

Additionally, it may be helpful to provide some basic configuration of the agent, this can be done either via [JVM
properties][4] or [Environment Variables][5].  The two most relevant configuration items are as follows:

| JVM Property | Environment Variable | Description |
|--------------|----------------------|-------------|
| `otel.service.name` | `OTEL_SERVICE_NAME`  | Sets the name of your application, this will apply a `service.name` label to all metrics generated by your application. |
| `otel.metric.export.interval` | `OTEL_METRIC_EXPORT_INTERVAL` | Sets how often metrics are exported in milliseconds, defaults to `60000`. |

# Dependency

This API is provided by the `observability-lib` module which can be depended on from Maven like so:

```xml

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>observability</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information.

Note that many of the other modules already depend on this module so an explicit dependency may be unnecessary.

## Test Dependencies

If you are wanting to [test metrics](#testing-open-telemetry-metrics) then you will need the following additional `test`
scoped dependencies:

```xml
<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>projectors-lib</artifactId>
    <version>${project.version}</version>
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>observability-lib</artifactId>
    <version>${project.version}</version>
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <scope>test</scope>
</dependency>
```        

[OTEL]: https://opentelemetry.io
[1]: https://opentelemetry.io/docs/instrumentation/java/manual/#metrics
[2]: https://www.mojohaus.org/buildnumber-maven-plugin/create-metadata-mojo.html
[3]: https://opentelemetry.io/docs/instrumentation/java/automatic/
[4]: https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/
[5]: https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/sdk-environment-variables.md
