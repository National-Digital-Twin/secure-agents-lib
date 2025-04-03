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
import java.util.Properties;
import java.util.Set;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicy;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.DatasetGraphDeserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.OffsetStore;

/**
 * A Kafka event source that reads RDF Datasets from a Kafka topic
 * <p>
 * Integration Architecture Node  will be exchanging RDF as datasets to allow security labels to travel with the data in a separate
 * graph.  NQuads (the default RDF Dataset serialization assumed) is a superset of NTriples so any pre-existing events
 * in NTriples format can be read by this source.
 * </p>
 */
public class KafkaDatasetGraphSource<TKey> extends KafkaEventSource<TKey, DatasetGraph> {

    /**
     * Creates a new builder for building a Kafka Event source where the values are RDF Datasets
     * <p>
     * See {@link KafkaEventSource#create()} for creating a more generic source.
     * </p>
     *
     * @param <TKey> Event key type
     * @return Builder
     */
    public static <TKey> Builder<TKey> createGraph() {
        return new Builder<>();
    }

    /**
     * Creates a new event source backed by a Kafka topic
     *
     * @param bootstrapServers     Kafka Bootstrap servers
     * @param topics               Kafka topic(s) to subscribe to
     * @param groupId              Kafka Consumer Group ID
     * @param keyDeserializerClass Key Deserializer class
     * @param maxPollRecords       Maximum events to retrieve and buffer in one Kafka
     *                             {@link KafkaConsumer#poll(Duration)} request.
     * @param policy               Kafka read policy to use
     * @param autoCommit           Whether the event source will automatically commit Kafka positions
     * @param offsetStore          An external offset store to commit offsets to in addition to committing them to
     *                             Kafka
     * @param lagReportInterval    Lag reporting interval
     * @param properties           Kafka Consumer Configuration properties, these may be overridden by explicit
     *                             configuration parameters passed to this constructor
     */
    KafkaDatasetGraphSource(String bootstrapServers, Set<String> topics, String groupId, String keyDeserializerClass,
                            int maxPollRecords, KafkaReadPolicy<TKey, DatasetGraph> policy, boolean autoCommit,
                            OffsetStore offsetStore, Duration lagReportInterval, Properties properties) {
        super(bootstrapServers, topics, groupId, keyDeserializerClass,
              DatasetGraphDeserializer.class.getCanonicalName(), maxPollRecords, policy, autoCommit, offsetStore,
              lagReportInterval, properties);
    }

    /**
     * A Builder for Dataset Graph event sources
     *
     * @param <TKey> Event key type
     */
    public static class Builder<TKey>
            extends AbstractKafkaEventSourceBuilder<TKey, DatasetGraph, KafkaDatasetGraphSource<TKey>, Builder<TKey>> {

        /**
         * Creates a new builder
         */
        Builder() {
            this.valueDeserializerClass = DatasetGraphDeserializer.class.getCanonicalName();
        }

        @Override
        public KafkaDatasetGraphSource<TKey> build() {
            return new KafkaDatasetGraphSource<>(this.bootstrapServers, this.topics, this.groupId,
                                                 this.keyDeserializerClass, this.maxPollRecords, this.readPolicy,
                                                 this.autoCommit, this.externalOffsetStore, this.lagReportInterval,
                                                 this.properties);
        }
    }
}
