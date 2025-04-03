// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.

/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */
package uk.gov.dbt.ndtp.secure.agent.sources.kafka;

import java.time.Duration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Convenience wrapper around a {@link KafkaContainer} instance for use in testing, this implementation creates a
 * completely insecure cluster.  See {@link SecureKafkaTestCluster} or {@link MutualTlsKafkaTestCluster} for secure
 * clusters.
 */
public class BasicKafkaTestCluster extends KafkaTestCluster<KafkaContainer> {

    public static final String DEFAULT_TOPIC = "tests";

    /**
     * Creates a new test cluster
     */
    public BasicKafkaTestCluster() {
        // Empty Constructor
    }

    /**
     * Creates the actual Kafka container that forms the test cluster
     *
     * @return Kafka container
     */
    @SuppressWarnings("resource")
    protected KafkaContainer createKafkaContainer() {
        //@formatter:off
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
                    .withStartupTimeout(Duration.ofSeconds(180));
        //@formatter:on
    }

    @Override
    public String getBootstrapServers() {
        return this.kafka != null ? this.kafka.getBootstrapServers() : null;
    }
}
