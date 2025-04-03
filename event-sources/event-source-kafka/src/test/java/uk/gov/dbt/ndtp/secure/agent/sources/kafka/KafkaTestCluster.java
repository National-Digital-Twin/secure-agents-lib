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
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;

/**
 * Convenience wrapper around a {@link KafkaContainer} instance for use in testing
 */
public abstract class KafkaTestCluster<T extends GenericContainer<?>> {

    public static final String DEFAULT_TOPIC = "tests";

    protected T kafka;
    protected AdminClient adminClient;

    /**
     * Creates a new test cluster
     */
    public KafkaTestCluster() {

    }

    /**
     * Creates the actual Kafka container that forms the test cluster
     *
     * @return Kafka container
     */
    protected abstract T createKafkaContainer();

    /**
     * Gets the default set of additional Kafka Client properties necessary to connect to the Kafka cluster
     *
     * @return Client Properties
     */
    public Properties getClientProperties() {
        return new Properties();
    }

    /**
     * Gets the bootstrap server for connecting to the Kafka test cluster
     *
     * @return Bootstrap servers
     */
    public abstract String getBootstrapServers();

    /**
     * Gets a Kafka Admin Client for the cluster
     *
     * @return Admin Client
     */
    public AdminClient getAdminClient() {
        return this.adminClient;
    }

    /**
     * Adds properties necessary to create an Admin Client.
     * <p>
     * The default implementation just sets the Bootstrap servers.
     * </p>
     *
     * @param properties Admin client properties
     */
    protected void addAdminClientProperties(Properties properties) {
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.getBootstrapServers());
    }

    /**
     * Setups and starts the test cluster
     */
    public void setup() {
        if (this.kafka == null) {
            this.kafka = this.createKafkaContainer();
        }

        try {
            Utils.logStdOut("Starting Kafka Test Cluster...");
            long start = System.currentTimeMillis();
            this.kafka.start();
            Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - start);
            Utils.logStdOut("Kafka Test Cluster ready in %,d seconds!", elapsed.toSeconds());
        } catch (ContainerLaunchException e) {
            System.err.println("Failed to launch Kafka container, see logs below:");
            System.err.println();
            System.err.println(this.kafka.getLogs());
            System.err.println();

            throw e;
        }
        Properties properties = new Properties();
        addAdminClientProperties(properties);
        this.adminClient = KafkaAdminClient.create(properties);
        this.adminClient.createTopics(List.of(new NewTopic(DEFAULT_TOPIC, 1, (short) 1)));
    }

    /**
     * Resets the test topic
     */
    public void resetTestTopic() {
        resetTopic(DEFAULT_TOPIC);
    }

    /**
     * Resets the given topic i.e. deletes it then recreates it
     *
     * @param topic Topic
     */
    public void resetTopic(String topic) {
        // If the admin client is null then the test cluster never came up so nothing to reset
        if (this.adminClient != null) {
            deleteTopic(topic);
            this.adminClient.createTopics(List.of(new NewTopic(topic, 1, (short) 1)));
        }
    }

    /**
     * Deletes the given topic
     *
     * @param topic Topic
     */
    public void deleteTopic(String topic) {
        this.adminClient.deleteTopics(List.of(topic));
    }

    /**
     * Tears down the test cluster
     */
    public void teardown() {
        if (this.kafka != null) {
            Utils.logStdOut("Stopping Kafka Test Cluster...");
            long start = System.currentTimeMillis();
            this.adminClient.close();
            this.kafka.stop();
            Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - start);
            Utils.logStdOut("Kafka Test Cluster stopped in %,d seconds!", elapsed.toSeconds());
        }
        this.adminClient = null;
    }
}
