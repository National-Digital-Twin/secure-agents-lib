# CLI Health Probe Server
**Repository:** `secure-agents-lib`  
**Description:** `CLI Health Probe Server module provides a minimalist Health Probe Server`
<!-- SPDX-License-Identifier: OGL-UK-3.0 -->

The `cli-probe-server` module provides a minimalist Health Probe Server based upon our [JAX-RS Base
Server](../jaxrs-base-server/index.md).  This is intended for incorporation into CLI applications that wouldn't normally
have a HTTP interface and allows them to easily expose liveness and readiness probes for consumption by container
runtimes and other monitoring.

# HTTP API

The Health Probe Server provides a very simple HTTP API with just two endpoints:

## `/version-info`

The `/version-info` endpoint is intended for use by liveness probes, it always responds with a HTTP 200 OK and returns a
JSON payload containing version information for the application so doubles as a way to check deployed application
versions.  Example output is as follows:

```json

```

## `/healthz`

The `/healthz` endpoint is intended for use by readiness probes, it responds with either an HTTP 200 OK if the
application is ready, or a 503 Service Unavailable if the application is unready.  It is the responsibility of the
application using the health probe server to provide a useful readiness supplier function that returns meaningful
readiness information.

The response in either case is a JSON payload like the following:

```json

```

# Usage 

A Health Probe server requires 3 pieces of information to run:

1. A display name for the server used in logging
2. A port upon which the server is exposed
3. A readiness supplier that can calculate the applications current readiness, used by the server in responding to 

# Dependency

The `cli-probe-server` module provides the health probe server machinery, it can be depended on from Maven like so:

```xml
<dependency>
    <groupId>uk.gov.dbt.ndtp.secure-agents</groupId>
    <artifactId>cli-api</artifactId>
    <version>VERSION</version>
</dependency>
```

Where `VERSION` is the desired version, see the top level [README](../../README.md) in this repository for that
information.

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.

Licensed under the Open Government Licence v3.0.