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
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class MutualTlsKafkaContainer extends GenericContainer<MutualTlsKafkaContainer> {

    private static final AtomicInteger PORT = new AtomicInteger(39093);

    private final int port;

    /**
     * Creates a new Secure Kafka Container
     *
     * @param imageName Image name, assumed to be some tag of the Confluent Kafka as the configuration relies upon logic
     *                  in that images scripts
     */
    public MutualTlsKafkaContainer(DockerImageName imageName) {
        super(imageName);
        this.port = PORT.getAndIncrement();
        this.addFixedExposedPort(this.port, this.port);
        this.withEnv("CLUSTER_ID", "MkU3OEVBNTcwNTJENDM2Qk")
            .withEnv("KAFKA_NODE_ID", "1")
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "SSL:SSL,CONTROLLER:SSL")
            .withEnv("KAFKA_LISTENERS", "SSL://:" + this.port + ",CONTROLLER://:19093")
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "SSL://localhost:" + this.port)
            .withEnv("KAFKA_JMX_HOSTNAME", "localhost")
            .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@:19093")
            .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "SSL")
            .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
            .withEnv("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", " ")
            .withEnv("KAFKA_SSL_TRUSTSTORE_FILENAME", "broker-truststore")
            .withEnv("KAFKA_SSL_TRUSTSTORE_CREDENTIALS", "credentials")
            .withEnv("KAFKA_SSL_KEYSTORE_FILENAME", "broker-keystore")
            .withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", "credentials")
            .withEnv("KAFKA_SSL_KEY_CREDENTIALS", "credentials")
            .withEnv("KAFKA_SSL_CLIENT_AUTH", "required")
            .withStartupTimeout(Duration.ofSeconds(180))
            .waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1))
            .withExposedPorts(this.port);
    }

    /**
     * Gets the port this container has been configured to listen on
     *
     * @return Port number
     */
    public int getPort() {
        return this.port;
    }

}
