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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A variation upon {@link KafkaTestCluster} that enables Kafka's user authentication features with SASL PLAINTEXT
 * authentication
 */
public class SecureKafkaTestCluster extends BasicKafkaTestCluster {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "admin-secret";
    public static final String DEFAULT_CLIENT_USERNAME = "client";
    public static final String DEFAULT_CLIENT_PASSWORD = "client-secret";
    protected final String adminUsername, adminPassword;
    protected final Map<String, String> additionalUsers;

    /**
     * Creates a new Secure Kafka cluster with a default admin and non-admin user
     * <p>
     * See {@link #DEFAULT_ADMIN_USERNAME} and {@link #DEFAULT_ADMIN_PASSWORD} for the default admin user credentials.
     * </p>
     * <p>
     * See {@link #DEFAULT_CLIENT_USERNAME} and {@link #DEFAULT_CLIENT_PASSWORD} for the default client user
     * credentials.
     * </p>
     */
    public SecureKafkaTestCluster() {
        this(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD, Map.of(DEFAULT_CLIENT_USERNAME, DEFAULT_CLIENT_PASSWORD));
    }

    /**
     * Creates a new Secure Kafka cluster with the configured users
     *
     * @param adminUsername   Admin username
     * @param adminPassword   Admin password
     * @param additionalUsers Additional users
     */
    public SecureKafkaTestCluster(String adminUsername, String adminPassword, Map<String, String> additionalUsers) {
        if (StringUtils.isBlank(adminUsername)) {
            throw new IllegalArgumentException("Admin username cannot be null/blank");
        }
        if (StringUtils.isBlank(adminPassword)) {
            throw new IllegalArgumentException("Admin password cannot be null/blank");
        }
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.additionalUsers = MapUtils.isEmpty(additionalUsers) ? new HashMap<>() : new HashMap<>(additionalUsers);
    }

    /**
     * Gets the configured admin username
     *
     * @return Username
     */
    public String getAdminUsername() {
        return this.adminUsername;
    }

    /**
     * Gets the configured admin password
     *
     * @return Admin password
     */
    public String getAdminPassword() {
        return this.adminPassword;
    }

    @Override
    public Properties getClientProperties() {
        return this.getClientProperties(this.adminUsername, this.adminPassword);
    }

    /**
     * Gets the configured additional users
     *
     * @return Additional users
     */
    public Map<String, String> getAdditionalUsers() {
        return Collections.unmodifiableMap(this.additionalUsers);
    }

    @Override
    @SuppressWarnings("resource")
    protected KafkaContainer createKafkaContainer() {
        // Heavily inspired by https://github.com/michelin/ns4kafka/blob/master/src/test/java/com/michelin/ns4kafka/integration/AbstractIntegrationTest.java
        //@formatter:off
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
                .withKraft()
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
                .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG", buildBrokerJaasConfig())
                .withEnv("KAFKA_SASL_JAAS_CONFIG", KafkaSecurity.plainLogin(this.adminUsername, this.adminPassword))
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", buildBrokerJaasConfig())
                .withEnv("KAFKA_SUPER_USERS", "User:" + this.adminUsername)
                .withStartupTimeout(Duration.ofSeconds(180));
        //@formatter:on
    }

    @NotNull
    protected String buildBrokerJaasConfig() {
        StringBuilder builder = new StringBuilder();
        builder.append("org.apache.kafka.common.security.plain.PlainLoginModule required ")
               .append("username=\"")
               .append(this.adminUsername)
               .append("\" password=\"")
               .append(this.adminPassword)
               .append("\" ")
               .append("user_")
               .append(this.adminUsername)
               .append("=\"")
               .append(this.adminPassword)
               .append("\" ");
        for (Map.Entry<String, String> user : this.additionalUsers.entrySet()) {
            builder.append("user_").append(user.getKey()).append("=\"").append(user.getValue()).append("\" ");
        }
        builder.append(";");
        return builder.toString();
    }

    @Override
    protected void addAdminClientProperties(Properties properties) {
        super.addAdminClientProperties(properties);
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name);
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, KafkaSecurity.plainLogin(this.adminUsername, this.adminPassword));
    }

    public Properties getClientProperties(String username, String password) {
        Properties properties = new Properties();
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name);
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, KafkaSecurity.plainLogin(username, password));
        return properties;
    }
}
