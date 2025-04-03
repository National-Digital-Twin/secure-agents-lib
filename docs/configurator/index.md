# Configurator API
**Repository:** `secure-agents-lib`  
**Description:** `Configurator API which is a lightweight abstraction for obtaining application configuration`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The `configurator` module provides the `Configurator` API which is a lightweight abstraction for obtaining application
configuration.  By default, this API only retrieves configuration provided by environment variables, but it may be
configured to obtain from alternative/additional sources as needed.

## Obtaining Configuration

Configuration values are obtained by calling one of the `Configurator.get()` methods.  At its simplest you do the
following:

```java
String value = Configurator.get("SOME_KEY");
```

If there are multiple possible keys for the same configuration you provide an array of keys, the keys are tried in the
order provided with the first valid (non-empty) value being returned:

```java
String value = Configurator.get(new String[] { "SOME_KEY", "ALTERNATIVE_KEY" });
```

You can add a default fallback value like so, if there is no value set for the given configuration then your provided
default value is returned instead:

```java
String value = Configurator.get(new String[] { "SOME_KEY", "ALTERNATIVE_KEY" }, "default value");
```

If you want to obtain a strongly typed value then you can also provide a `Function<String, T>` to convert from the raw
string value into a target type.  For example to treat a key as an `Integer`:

```java
Integer value = Configurator.get("SOME_KEY", Integer::parseInt, 1);
```

Note that in this scenario you **MUST** also provide a fallback value to use if none of the available configuration
values can be successfully parsed by your provided parser function.

## Configuration Sources

The `Configurator` API is backed by one/more configured `ConfigurationSource` instances, this API provides a simple
`get(String)` method that retrieves the raw configuration value for a given configuration key.

You can add a new source via the `Configurator.addSource()` method.  When you add a new source it does not replace
existing sources but is added as the primary source.  Thus, the order in which you add the sources, is significant.  For example if you did the following:

```java
Configurator.addSource(SystemPropertiesSource.INSTANCE);
```

Then you are configuring the [`SystemPropertiesSource`](#systempropertiessource) to act as the primary source, so if you
then called `Configurator.get("SOME_KEY")` it would look up configuration first in the System Properties, and then in the
Environment Variables if it wasn't found in the System Properties.

If you **ONLY** wish to use a single source there are a couple of ways to achieve that.  Either you can call
`Configurator.setUseAllSources(false);` to make only the most recently configured source be used.  Or alternatively
instead of calling `addSource()` use `setSingleSource()` with the desired source.

Finally, if you ever wish to reset back to the default configuration of just the `EnvironmentSource` you can do so by
calling `Configurator.reset()`.

## Available Sources

The following `ConfigurationSource` implementations are provided:

- [`NullSource`](#nullsource)
- [`EnvironmentSource`](#environmentsource)
- [`PropertiesSource`](#propertiessource)
- [`SystemPropertiesSource`](#systempropertiessource)

### `NullSource`

The `NullSource` is a singleton available via `NullSource.INSTANCE`.  It's implementation returns `null` for every
configuration key, this is useful if you need to simulate a completely blank configuration for testing irrespective of
what may be set in the developers runtime environment.

### `EnvironmentSource`

The `EnvironmentSource` is the default source used by `Configurator`, it is a singleton available via
`EnvironmentSource.INSTANCE`.

It automatically converts any key provided into environment variable format, e.g., if you provided `some.key` as the key
that would be converted into `SOME_KEY`.

### `PropertiesSource`

The `PropertiesSource` is a source backed by a Java `Properties` object, this allows you to supply an arbitrary set of
configuration values which is very useful for constructing unit and integration tests.

It automatically converts any key provided into system properties format, e.g., if you provided `SOME_KEY` as the key
that would be converted into `some.key`.

### `SystemPropertiesSource`

The `SystemPropertiesSource` is a source backed by the System Properties provided via `System.getProperties()`, it is a
singleton available via `SystemPropertiesSource.INSTANCE`.  This always reflects the current state of the System
Properties.

It automatically converts any key provided into system properties format, e.g., if you provided `SOME_KEY` as the key
that would be converted into `some.key`.

# Dependency

This API is provided by the `configurator` module which can be depended on from Maven like so:

```xml
<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>configurator</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information.

Note that some of the other modules already depend on this module so an explicit dependency may be unnecessary.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.