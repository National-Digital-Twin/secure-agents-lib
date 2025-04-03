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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicy;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.OffsetStore;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract builder for Kafka event sources
 *
 * @param <TKey>    Event Key type
 * @param <TValue>  Event Value type
 * @param <TSource> Event Source type
 */
@SuppressWarnings("unchecked")
public abstract class AbstractKafkaEventSourceBuilder<TKey, TValue, TSource extends KafkaEventSource<TKey, TValue>, TBuilder extends AbstractKafkaEventSourceBuilder<TKey, TValue, TSource, TBuilder>> {

    static final Logger LOGGER = LoggerFactory.getLogger(AbstractKafkaEventSourceBuilder.class);

    String bootstrapServers, groupId, keyDeserializerClass, valueDeserializerClass;
    final Set<String> topics = new LinkedHashSet<>();
    int maxPollRecords = 100;
    Duration lagReportInterval = Duration.ofMinutes(1);
    KafkaReadPolicy<TKey, TValue> readPolicy = KafkaReadPolicies.fromEarliest();
    boolean autoCommit = true;
    OffsetStore externalOffsetStore = null;
    Properties properties = new Properties();

    /**
     * Sets the bootstrap servers
     *
     * @param servers Servers
     * @return Builder
     */
    public TBuilder bootstrapServers(String servers) {
        this.bootstrapServers = servers;
        return (TBuilder) this;
    }

    /**
     * Sets the bootstrap servers
     *
     * @param servers Servers
     * @return Builder
     */
    public TBuilder bootstrapServers(String... servers) {
        return bootstrapServers(StringUtils.join(servers, ","));
    }

    /**
     * Sets a single topic to be read
     *
     * @param topic Topic
     * @return Builder
     */
    public TBuilder topic(String topic) {
        if(!this.topics.isEmpty()){
            LOGGER.info(String.format("Topics '%s' are being replaced by '%s'", topics.toString(), topic));
            this.topics.clear();
        }
        this.topics.add(topic);
        return (TBuilder) this;
    }

    /**
     * Sets the topic(s) to be read
     *
     * @param topics Topic(s)
     * @return Builder
     */
    public TBuilder topics(Collection<String> topics) {
        CollectionUtils.addAll(this.topics, topics);
        return (TBuilder) this;
    }


    /**
     * Sets the topic(s) to be read
     *
     * @param topics Topic(s)
     * @return Builder
     */
    public TBuilder topics(String... topics) {
        CollectionUtils.addAll(this.topics, topics);
        return (TBuilder) this;
    }

    /**
     * Sets the Kafka consumer group ID to use
     *
     * @param groupId Consumer Group ID
     * @return Builder
     */
    public TBuilder consumerGroup(String groupId) {
        this.groupId = groupId;
        return (TBuilder) this;
    }

    /**
     * Sets the maximum number of records to fetch from Kafka on one
     * {@link uk.gov.dbt.ndtp.secure.agent.sources.EventSource#poll(Duration)} call
     *
     * @param max Maximum poll records
     * @return Builder
     */
    public TBuilder maxPollRecords(int max) {
        this.maxPollRecords = max;
        return (TBuilder) this;
    }

    /**
     * Sets how frequently the topic lag should be calculated and reported
     * <p>
     * Defaults to 1 minute if not set
     * </p>
     *
     * @param interval Interval
     * @return Builder
     */
    public TBuilder lagReportInterval(Duration interval) {
        this.lagReportInterval = interval;
        return (TBuilder) this;
    }

    /**
     * Sets the key deserializer class
     *
     * @param keyDeserializerClass Key deserializer class
     * @return Builder
     */
    public TBuilder keyDeserializer(String keyDeserializerClass) {
        this.keyDeserializerClass = keyDeserializerClass;
        return (TBuilder) this;
    }

    /**
     * Sets the value deserializer class
     *
     * @param valueDeserializerClass Value deserializer class
     * @return Builder
     */
    public TBuilder valueDeserializer(String valueDeserializerClass) {
        this.valueDeserializerClass = valueDeserializerClass;
        return (TBuilder) this;
    }

    /**
     * Sets the key deserializer
     *
     * @param cls Class
     * @return Builder
     */
    public TBuilder keyDeserializer(Class<?> cls) {
        Objects.requireNonNull(cls, "Class cannot be null");
        return keyDeserializer(cls.getCanonicalName());
    }

    /**
     * Sets the value deserializer
     *
     * @param cls Class
     * @return Builder
     */
    public TBuilder valueDeserializer(Class<?> cls) {
        Objects.requireNonNull(cls, "Class cannot be null");
        return valueDeserializer(cls.getCanonicalName());
    }

    /**
     * Sets the read policy to {@link KafkaReadPolicies#fromBeginning()}
     *
     * @return Builder
     */
    public TBuilder fromBeginning() {
        return readPolicy(KafkaReadPolicies.fromBeginning());
    }

    /**
     * Sets the read policy to {@link KafkaReadPolicies#fromEarliest()}
     *
     * @return Builder
     */
    public TBuilder fromEarliest() {
        return readPolicy(KafkaReadPolicies.fromEarliest());
    }

    /**
     * Sets the read policy to {@link KafkaReadPolicies#fromLatest()}
     *
     * @return Builder
     */
    public TBuilder fromLatest() {
        return readPolicy(KafkaReadPolicies.fromLatest());
    }

    /**
     * Sets the read policy to {@link KafkaReadPolicies#fromEnd()}
     *
     * @return Builder
     */
    public TBuilder fromEnd() {
        return readPolicy(KafkaReadPolicies.fromEnd());
    }

    /**
     * Sets the read policy to the given policy
     *
     * @param policy Read policy
     * @return Builder
     */
    public TBuilder readPolicy(KafkaReadPolicy<TKey, TValue> policy) {
        this.readPolicy = policy;
        return (TBuilder) this;
    }

    /**
     * Enables auto-commit behaviour, this means the event source will consider an event as read and update Kafka
     * offsets accordingly as soon as it has been read from this event source.
     *
     * @return Builder
     */
    public TBuilder autoCommit() {
        return autoCommit(true);
    }

    /**
     * Enables/Disables auto-commit behaviour.  This controls whether the event source will consider an event as read
     * and update Kafka offsets accordingly as soon as it has been read from the event source.  When disabled offsets
     * are only updated when the {@link uk.gov.dbt.ndtp.secure.agent.sources.EventSource#processed(Collection)} method is
     * calling on the resulting Kafka event source.
     *
     * @param enabled Whether auto-commit is enabled
     * @return Builder
     */
    public TBuilder autoCommit(boolean enabled) {
        this.autoCommit = enabled;
        return (TBuilder) this;
    }

    /**
     * Disables auto-commit behaviour.  A source configured like this will <strong>ONLY</strong> commit offsets back to
     * Kafka when its {@link uk.gov.dbt.ndtp.secure.agent.sources.EventSource#processed(Collection)} method is called.
     *
     * @return Builder
     */
    public TBuilder commitOnProcessed() {
        return autoCommit(false);
    }

    /**
     * Sets an external offset store to be used to store Kafka offsets in addition to Kafka's own consumer group offset
     * storage
     *
     * @param store Offset store
     * @return Builder
     */
    public TBuilder externalOffsetStore(OffsetStore store) {
        this.externalOffsetStore = store;
        return (TBuilder) this;
    }

    /**
     * Sets a Kafka Consumer configuration property that will be used to configure the underlying
     * {@link org.apache.kafka.clients.consumer.KafkaConsumer}.  Note that some properties are always overridden by the
     * other source configuration provided to this builder.
     *
     * @param name  Name
     * @param value Value
     * @return Builder
     */
    public TBuilder consumerConfig(String name, Object value) {
        this.properties.put(name, value);
        return (TBuilder) this;
    }

    /**
     * Sets multiple Kafka Consumer configuration properties that will be used to configure the underlying
     * {@link org.apache.kafka.clients.consumer.KafkaConsumer}. Note that some properties are always overridden by the
     * other source configuration provided to this builder.
     *
     * @param properties Consumer configuration properties
     * @return Builder
     */
    public TBuilder consumerConfig(Properties properties) {
        this.properties.putAll(properties);
        return (TBuilder) this;
    }

    /**
     * Configures the consumer to perform a plain SASL login to the Kafka cluster using the provided credentials
     * <p>
     * If an alternative authentication mechanism is needed use {@link #consumerConfig(Properties)} to supply the
     * necessary configuration properties with suitable values.
     * </p>
     *
     * @param username Username
     * @param password Password
     * @return Builder
     */
    public TBuilder plainLogin(String username, String password) {
        this.properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        this.properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name);
        this.properties.put(SaslConfigs.SASL_JAAS_CONFIG, KafkaSecurity.plainLogin(username, password));
        return (TBuilder) this;
    }

    /**
     * Builds the event source
     *
     * @return Event source
     * @throws NullPointerException     A required argument has not been configured on this builder
     * @throws IllegalArgumentException An illegal value has been configured on this builder
     */
    public abstract TSource build();
}
