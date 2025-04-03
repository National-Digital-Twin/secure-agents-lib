#
# Copyright (C) Telicent Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# syntax=docker/dockerfile:1.7

# Application Base Image
FROM eclipse-temurin:21-jre-jammy AS app-base

RUN apt-get update && \
    apt-get install -y --no-install-recommends tini && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /app/ /app/lib/ /app/agents/ /opt/ianode/sbom/

RUN useradd -Mg root ianode-service
USER ianode-service

ARG PROJECT_VERSION
COPY cli/cli-common.sh cli/cli-entrypoint.sh /app/

ENV PROJECT_VERSION=${PROJECT_VERSION} \
    TINI_SUBREAPER=true

# CLI Base Image
FROM app-base AS cli-base

ARG PROJECT_VERSION
COPY cli/cli-debug/target/cli-debug-${PROJECT_VERSION}-bom.json \
     /opt/ianode/sbom/
COPY cli/cli-debug/target/cli-debug-${PROJECT_VERSION}.jar \
     cli/cli-debug/lib/* \
     /app/lib/
COPY cli/cli-debug/agents/* \
     /app/agents/

ENV OTEL_METRICS_EXPORTER=none \
    OTEL_TRACES_EXPORTER=none

# Debug Tools
FROM cli-base AS secure-agent-debug-tools

COPY cli/cli-debug/debug.sh /app/

ENV CLI_SCRIPT=/app/debug.sh
ENTRYPOINT [ "/usr/bin/tini", "--", "/app/cli-entrypoint.sh" ]
