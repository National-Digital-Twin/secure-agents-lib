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
package uk.gov.dbt.ndtp.secure.agent.cli.options;

import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.AllowedRawValues;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaSecurity;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options related to provided additional Kafka configuration, especially around authentication
 */
public class KafkaConfigurationOptions {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConfigurationOptions.class);

    /**
     * Environment variable used to specify Kafka username
     */
    public static final String KAFKA_USERNAME = "KAFKA_USER";
    /**
     * Environment variable used to specify Kafka password
     */
    public static final String KAFKA_PASSWORD = "KAFKA_PASSWORD";

    /**
     * Environment variable used to specify Kafka Configuration properties file
     */
    public static final String KAFKA_CONFIG_FILE_PATH = "KAFKA_CONFIG_FILE_PATH";

    /**
     * Alternative environment variable used to specify Kafka Configuration properties file
     */
    public static final String KAFKA_PROPERTIES = "KAFKA_PROPERTIES";

    private static final String LOGIN_PLAIN = "PLAIN";
    private static final String LOGIN_SCRAM_SHA_256 = "SCRAM-SHA-256";
    private static final String LOGIN_SCRAM_SHA_512 = "SCRAM-SHA-512";

    @Option(name = {
            "--kafka-user", "--kafka-username"
    }, title = "KafkaUser", description = "Specifies the username used to connect to Kafka.  May also be specified via the KAFKA_USER environment variable.")
    private String username = Configurator.get(KAFKA_USERNAME);

    @Option(name = "--kafka-password", title = "KafkaPassword", description = "Specifies the password used to connect to Kafka.  Generally it is better to use the KAFKA_PASSWORD environment variable to supply this instead of supplying it directly at the command line.")
    private String password = Configurator.get(KAFKA_PASSWORD);

    @Option(name = "--kafka-login-type", title = "LoginType", description = "Specifies the Kafka Login Type to use in conjunction with the --kafka-user and --kafka-password arguments for SASL authentication, if you use an alternative Kafka authentication mechanism, or a variant of SASL not listed as supported here, then use --kafka-properties to supply a suitably configured properties file instead.")
    @AllowedRawValues(allowedValues = { LOGIN_PLAIN, LOGIN_SCRAM_SHA_256, LOGIN_SCRAM_SHA_512 })
    private String loginType = LOGIN_PLAIN;

    @Option(name = "--kafka-properties", title = "KafkaPropertiesFile", description = "Specifies a properties file containing Kafka configuration properties to use with Kafka.")
    @com.github.rvesse.airline.annotations.restrictions.File(mustExist = true)
    private File propertiesFile = Configurator.get(new String[] { KAFKA_CONFIG_FILE_PATH, KAFKA_PROPERTIES }, File::new, null);

    @Option(name = "--kafka-property", title = "KafkaProperty", arity = 2, description = "Specifies a Kafka configuration property to use with Kafka.  These are loaded prior to any properties from a file specified via the --kafka-properties option.")
    private List<String> extraConfiguration = new ArrayList<>();

    private Properties properties = null;

    /**
     * Gets the additional configuration properties to pass to Kafka
     *
     * @return Configuration properties
     */
    public synchronized Properties getAdditionalProperties() {
        // Use cached properties if present
        if (properties != null) {
            return this.properties;
        }
        Properties properties = new Properties();

        // If a Username and Password are provided then configure Kafka properties for login based on those
        if (StringUtils.isNotBlank(this.username) && StringUtils.isNotBlank(this.password)) {
            properties.put(SaslConfigs.SASL_MECHANISM, this.loginType);
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                           Objects.equals(this.loginType, LOGIN_PLAIN) ? SecurityProtocol.SASL_PLAINTEXT.name :
                           SecurityProtocol.SASL_SSL.name);
            properties.put(SaslConfigs.SASL_JAAS_CONFIG, Objects.equals(this.loginType, LOGIN_PLAIN) ?
                                                         KafkaSecurity.plainLogin(this.username, this.password) :
                                                         KafkaSecurity.scramLogin(this.username, this.password));
            LOGGER.info("Configured Kafka properties for SASL {} authentication", this.loginType);
        }

        // Load in any command line provided properties
        for (int i = 0; i <= this.extraConfiguration.size() - 2; i += 2) {
            properties.put(this.extraConfiguration.get(i), this.extraConfiguration.get(i + 1));
        }

        // Load in the properties file (if specified)
        if (this.propertiesFile != null) {
            try (FileInputStream input = new FileInputStream(this.propertiesFile)) {
                properties.load(input);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to read user supplied Kafka properties file %s",
                                                         this.propertiesFile.getAbsolutePath()));
            }
        }

        LOGGER.info("Gathered/generated {} Kafka properties based on supplied options", properties.size());
        // Cache properties for later reuse because some option modules and/or commands may call this method multiple
        // times and this avoids repeating the property loading unnecessary
        this.properties = properties;
        return properties;
    }
}
