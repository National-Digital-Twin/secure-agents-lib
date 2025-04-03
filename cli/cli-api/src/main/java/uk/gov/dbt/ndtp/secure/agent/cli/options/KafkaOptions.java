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

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.AllowedEnumValues;
import com.github.rvesse.airline.annotations.restrictions.ranges.IntegerRange;
import com.github.rvesse.airline.annotations.restrictions.ranges.LongRange;
import com.github.rvesse.airline.model.CommandMetadata;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import uk.gov.dbt.ndtp.secure.agent.cli.restrictions.RequiredForSource;
import uk.gov.dbt.ndtp.secure.agent.cli.restrictions.SourceRequired;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options related to configuring a Kafka Event source
 */
public class KafkaOptions extends KafkaConfigurationOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOptions.class);

    /**
     * Environment variable used to specify Kafka bootstrap servers
     */
    public static final String BOOTSTRAP_SERVERS = "BOOTSTRAP_SERVERS";
    /**
     * Environment variable used to specify Kafka topic
     */
    public static final String TOPIC = "TOPIC";
    /**
     * Environment variable used to specify Kafka input topic, may be used interchangeably with {@link #TOPIC}
     */
    public static final String INPUT_TOPIC = "INPUT_TOPIC";
    /**
     * Environment variable used to specify Kafka output dead letter topic, where events with processing errors will be
     * written.
     */
    public static final String DLQ_TOPIC = "DLQ_TOPIC";
    /**
     * Environment variable used to specify Kafka output dead letter topic, where events with processing errors will be
     * written.
     *
     * @deprecated Use corrected constant {@link #DLQ_TOPIC}
     */
    @Deprecated(forRemoval = true)
    public static final String DQL_TOPIC = DLQ_TOPIC;
    /**
     * Environment variable used to specify Kafka consumer group
     */
    public static final String CONSUMER_GROUP = "CONSUMER_GROUP";

    /**
     * Default consumer group used if a more specific one is not set or automatically determined based upon the command
     * being run
     */
    public static final String DEFAULT_CONSUMER_GROUP = "secure-agent";

    @AirlineModule
    private CommandMetadata command;

    /**
     * Kafka bootstrap servers
     */
    @Option(name = {
            "--bootstrap-server",
            "--bootstrap-servers"
    }, title = "BootstrapServers", description = "Provides a comma separated list of bootstrap servers to use for creating the initial connection to Kafka.")
    @SourceRequired(name = "Kafka", unlessEnvironment = BOOTSTRAP_SERVERS)
    public String bootstrapServers = Configurator.get(BOOTSTRAP_SERVERS);

    /**
     * Kafka input topic
     */
    @Option(name = {
            "-t",
            "--topic"
    }, title = "KafkaTopic", description = "Provides the name of the Kafka topic(s) to read events from.  May be specified multiple times to supply multiple topics, when multiple topics are specified then they must all contain events that can be deserialized by this command otherwise errors will occur!")
    @RequiredForSource(sourceName = "Kafka", unlessEnvironment = { TOPIC, INPUT_TOPIC })
    public Set<String> topics = Configurator.get(new String[] { TOPIC, INPUT_TOPIC }, t -> {
        Set<String> ts = new LinkedHashSet<>();
        ts.add(t);
        return ts;
    }, new LinkedHashSet<>());

    /**
     * Kafka DLQ Topic
     */
    @Option(name = {
            "-dlq",
            "--dlq-topic"
    }, title = "KafkaDlqTopic", description = "Provides the name of a Kafka dead letter topic, where events with processing errors will be written.")
    public String dlqTopic = Configurator.get(DLQ_TOPIC);

    /**
     * Kafka consumer group
     */
    @Option(name = {
            "-g",
            "--group"
    }, title = "KafkaConsumerGroup", description = "Provides the name of the Kafka Consumer Group to use.  If not set defaults to the name of the command being invoked, or secure-agent if that is unknown.")
    private String group = null;

    /**
     * Lag reporting interval
     */
    @Option(name = "--lag-report-interval", title = "LagReportIntervalSeconds", description = "Specifies how often in seconds the lag for the Kafka topic partitions being read should be calculated and reported.  As calculating lag can be an expensive operation it should not be done too often.  Default is 30 seconds.")
    @LongRange(min = 5)
    private long lagReportInterval = 30;

    @Option(name = "--kafka-max-poll-records", title = "KafkaMaxPollRecords", description = "Specifies the maximum number of records to retrieve from Kafka in a single poll request.  Defaults to 1,000.")
    @IntegerRange(min = 100)
    private int maxPollRecords = 1000;

    /**
     * Gets the maximum number of records to poll from Kafka in a single request
     *
     * @return Maximum records to poll
     */
    public int getMaxPollRecords() {
        return this.maxPollRecords;
    }

    /**
     * Gets the consumer group to use to take advantage of Kafka's Consumer Group features.
     * <p>
     * This may be provided explicitly by the user, either via the command line {@code -g/--group} option or the
     * {@value #CONSUMER_GROUP} environment variable.  If not provided explicitly then a suitable default will be
     * automatically selected.  This default will be the name of the command being invoked, or if not used in the
     * context of a command then defaults to {@value #DEFAULT_CONSUMER_GROUP}.
     * </p>
     *
     * @return Consumer Group to use
     */
    public String getConsumerGroup() {
        if (this.group != null) {
            return this.group;
        }

        String envValue = Configurator.get(new String[] { CONSUMER_GROUP }, null);
        if (envValue != null) {
            return envValue;
        }

        if (this.command != null) {
            return this.command.getName();
        }

        return DEFAULT_CONSUMER_GROUP;
    }

    /**
     * Gets the configured lag reporting interval
     *
     * @return Lag reporting interval
     */
    public Duration getLagReportInterval() {
        return Duration.ofSeconds(this.lagReportInterval);
    }

    /**
     * Possible read policies
     */
    public enum ReadPolicy {
        /**
         * Read from beginning
         */
        BEGINNING,
        /**
         * Read from end
         */
        END,
        /**
         * Read from earliest
         */
        EARLIEST,
        /**
         * Read from latest
         */
        LATEST;

        /**
         * Converts into a Kafka Read Policy
         *
         * @param <TKey>   Key Type
         * @param <TValue> Value Type
         * @return Read policy
         */
        public <TKey, TValue> KafkaReadPolicy<TKey, TValue> toReadPolicy() {
            return switch (this) {
                case EARLIEST -> KafkaReadPolicies.fromEarliest();
                case LATEST -> KafkaReadPolicies.fromLatest();
                case END -> KafkaReadPolicies.fromEnd();
                default -> KafkaReadPolicies.fromBeginning();
            };
        }
    }

    /**
     * Kafka read policy
     */
    @Option(name = "--read-policy", title = "KafkaReadPolicy", description = "Specifies how events should be read from Kafka.  Defaults to EARLIEST i.e. automatically read from the earliest available events that have yet to be consumed by the specified Consumer Group.")
    @AllowedEnumValues(ReadPolicy.class)
    public ReadPolicy readPolicy = ReadPolicy.EARLIEST;
}
