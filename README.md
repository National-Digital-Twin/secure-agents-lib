# README

**Repository:** `secure-agents-lib`   
**Description:** `This repository provides a lightweight functional API for defining pipelines as well as more admin/operations oriented APIs for invoking and running Secure Agents pipelines and API Servers.`

<!-- SPDX-License-Identifier: Apache-2.0 AND OGL-UK-3.0 -->

## Overview
This repository contributes to the development of **secure, scalable, and interoperable data-sharing infrastructure**. It supports NDTP’s mission to enable **trusted, federated, and decentralised** data-sharing across organisations.

This repository is one of several open-source components that underpin NDTP’s **Integration Architecture (IA)**—a framework designed to allow organisations to manage and exchange data securely while maintaining control over their own information. The IA is actively deployed and tested across multiple sectors, ensuring its adaptability and alignment with real-world needs.

For a complete overview of the Integration Architecture (IA) project, please see the [Integration Architecture Documentation](https://github.com/National-Digital-Twin/integration-architecture-documentation).

## Prerequisites
Before using this repository, ensure you have the following dependencies installed:
- **Required Tooling:**
    - Java 17
    - Github PAT token set to allow retrieval of maven packages from Github Packages
    - Docker (if running via docker)
- **Pipeline Requirements:**
    - Cloud platform credentials
- **Supported Kubernetes Versions:** N/A
- **System Requirements:**
    - Java 17
    - Docker
    - Kafka (or connectivity to) - if applicable

## Quick Start
This repository does not really provide runnable code, rather it provides libraries that are used as building blocks to create runnable code in other repositories.

There are however some debugging tools found in the cli/cli-debug module that can be run via the included cli/cli-debug/debug.sh script.
See the [`debug.sh`](docs/cli/debug.md) documentation for more details.

This is a Java project built with Maven, please see BUILD for detailed build and development instructions. Provided you meet the basic requirements the following should work:

### 1. Download and Build
```sh  
git clone https://github.com/National-Digital-Twin/secure-agents-lib.git
cd [secure-agents-lib]  
```
### 2. Run Build Version
```sh  
mvn clean install --version  
```
## Features
The following features are currently available:

- `cli` - Provides a CLI for exercising this code base and running the Projectors.
  - `cli-api` - Provides the CLI API for implementing new commands.
  - `cli-debug` - Provides commands for various tools that do not constitute direct functionality, e.g. dumping the RDF from a Kafka topic, but which are useful for debugging. This can be invoked via the debug script in this module.

- `configurator` - Provides a lightweight configuration API for obtaining application configuration.

- `event-sources` - Provides Event Sources.

  - `event-sources-lib` - Provides an API for representing and accessing Event Sources.

  - `event-source-kafka` - Provides a Kafka backed implementation of the Event Source API.

- `jaxrs-base-server` - Provides a base JAX-RS server template to build server applications from.

- `live-reporter` - Provides the ability to report heartbeat status to IANode Live.

- `observability-lib` - Provides utilities around integrating Open Telemetry metrics into Secure Agents.

- `projector-driver` - Provides the ability to connect together an Event Source and a Projector.

- `projectors-lib` - Provides an API for defining Projectors and the processing of their output(s) via Sinks.

Usage of this repository is primarily by declaring dependencies on one/more of the
library modules provided and then using their APIs for your own Secure Agents development.  Please see the
[Documentation](docs/index.md) for introductions to the various libraries and APIs provided.

## Testing Guide

### Running Unit Tests
Navigate to the root of the project and run `mvn test` to run the tests for the repository.

## API Documentation
Documentation detailing the relevant configuration and endpoints is provided [here](./docs/cli/index.md ).

## Public Funding Acknowledgment
This repository has been developed with public funding as part of the National Digital Twin Programme (NDTP), a UK Government initiative. NDTP, alongside its partners, has invested in this work to advance open, secure, and reusable digital twin technologies for any organisation, whether from the public or private sector, irrespective of size.

## License
This repository contains both source code and documentation, which are covered by different licenses:
- **Code:** Originally developed by Telicent UK Ltd, now maintained by National Digital Twin Programme. Licensed under the [Apache License 2.0](LICENSE.md).
- **Documentation:** Licensed under the [Open Government Licence (OGL) v3.0](OGL_LICENSE.md).

By contributing to this repository, you agree that your contributions will be licensed under these terms.

See [LICENSE.md](LICENSE.md), [OGL_LICENSE.md](OGL_LICENSE.md), and [NOTICE.md](NOTICE.md) for details.

## Security and Responsible Disclosure
We take security seriously. If you believe you have found a security vulnerability in this repository, please follow our responsible disclosure process outlined in [SECURITY.md](SECURITY.md).

## Contributing
We welcome contributions that align with the Programme’s objectives. Please read our [Contributing](CONTRIBUTING.md) guidelines before submitting pull requests.

## Acknowledgements
This repository has benefited from collaboration with various organisations. For a list of acknowledgments, see [ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md).

## Support and Contact
For questions or support, check our Issues or contact the NDTP team on ndtp@businessandtrade.gov.uk.

**Maintained by the National Digital Twin Programme (NDTP).**

© Crown Copyright 2025. This work has been developed by the National Digital Twin Programme and is legally attributed to the Department for Business and Trade (UK) as the
governing entity.  
Licensed under the Open Government Licence v3.0.
