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

# Application base image
FROM eclipse-temurin:21-jre-jammy

# Install Tini
RUN apt-get update && \
    apt-get install -y --no-install-recommends tini && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set-up application directories
WORKDIR /app
RUN mkdir -p /app/ /app/lib/

# Copy application files
ARG PROJECT_VERSION
COPY cli-common.sh cli-entrypoint.sh /app/
COPY cli-debug/target/cli-debug-${PROJECT_VERSION}.jar \
     cli-debug/lib/* \
     /app/lib/
COPY fake-platform.sh cli-debug/debug.sh /app/

ENV PROJECT_VERSION=${PROJECT_VERSION} \
    OTEL_METRICS_EXPORTER=none \
    OTEL_TRACES_EXPORTER=none \
    CLI_SCRIPT=/app/fake-platform.sh

ENTRYPOINT [ "/usr/bin/tini", "--", "/app/cli-entrypoint.sh" ]