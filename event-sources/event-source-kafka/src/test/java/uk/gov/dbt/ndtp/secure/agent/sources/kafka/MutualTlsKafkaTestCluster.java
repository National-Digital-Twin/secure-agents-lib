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

import java.io.File;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.testcontainers.utility.DockerImageName;

/**
 * Creates a Kafka test cluster that uses mTLS for authentication
 * <p>
 * This assumes that a directory called {@code test-certs/} exists in the test environment and it contains the following
 * files:
 * </p>
 * <ul>
 *     <li>{@code broker-truststore} and {@code client-truststore} which contain the CA Root certificate used to sign
 *     the broker and client certificates</li>
 *     <li>{@code broker-keystore} containing the brokers certificate which has been signed by the CA Root certificate
 *     </li>
 *     <li>{@code client-keystore} containing the clients certificate which has been signed by the CA Root certificate
 *     </li>
 * </ul>
 * <p>
 * It also assumes that all these files are secured with the password {@code squirrel}
 * </p>
 * <p>
 * Note that a helper {@code generateCerts.sh} script is provided with this module that can be used to generate these
 * files on demand assuming you have {@code openssl} installed.
 * </p>
 */
public class MutualTlsKafkaTestCluster extends KafkaTestCluster<MutualTlsKafkaContainer> {

    private static final String DEFAULT_PASSWORD = "squirrel";

    /**
     * Creates a new Cluster
     */
    public MutualTlsKafkaTestCluster() {
        // Empty Constructor

    }

    @SuppressWarnings("deprecated")
    protected MutualTlsKafkaContainer createKafkaContainer() {
        //@formatter:off
        return new MutualTlsKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
                .withFileSystemBind(new File("test-certs/").getAbsolutePath(), "/etc/kafka/secrets/");
        //@formatter:on
    }

    protected void addAdminClientProperties(Properties properties) {
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.getBootstrapServers());
        // As we don't know the hostnames for test containers disable hostname verification
        properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        // Enable SSL as the security protocol and then supply the keystore and trust store necessary for this to function
        addSslProperties(properties);
    }

    private static void addSslProperties(Properties properties) {
        properties.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name);
        properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
                       new File("test-certs/client-keystore").getAbsolutePath());
        properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, DEFAULT_PASSWORD);
        properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, DEFAULT_PASSWORD);
        properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
                       new File("test-certs/client-truststore").getAbsolutePath());
        properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, DEFAULT_PASSWORD);
    }

    @Override
    public Properties getClientProperties() {
        Properties properties = new Properties();
        // As we don't know the hostnames for test containers disable hostname verification
        properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        // Add generic SSL properties
        addSslProperties(properties);
        return properties;
    }

    @Override
    public String getBootstrapServers() {
        return String.format("SSL://%s:%s", this.kafka.getHost(), this.kafka.getMappedPort(this.kafka.getPort()));
    }
}
