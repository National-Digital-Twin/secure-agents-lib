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
import uk.gov.dbt.ndtp.secure.agent.payloads.RdfPayload;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicy;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.RdfPayloadDeserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.OffsetStore;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * A Kafka event source that reads RDF Payloads from a Kafka topic.
 * <p>
 * An RDF payload is either an additive payload containing an RDF Dataset (i.e.
 * {@link org.apache.jena.sparql.core.DatasetGraph} or a mutative payload containing an RDF Patch (i.e.
 * {@link org.apache.jena.rdfpatch.RDFPatch}.  This event source uses the {@link RdfPayloadDeserializer} which
 * intelligently deserializes the event value based upon the {@code Content-Type} header in the Kafka event.  For events
 * with no such header they are assumed to be additive payloads serialized using NQuads.
 * </p>
 *
 * @param <TKey> Key type
 */
public class KafkaRdfPayloadSource<TKey> extends KafkaEventSource<TKey, RdfPayload> {

    /**
     * Creates a new builder for building a Kafka Event source where the values are RDF Datasets
     * <p>
     * See {@link KafkaEventSource#create()} for creating a more generic source.
     * </p>
     *
     * @param <TKey> Event key type
     * @return Builder
     */
    public static <TKey> Builder<TKey> createRdfPayload() {
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
     * @param properties           Kafka Consumer Properties, these may be overwritten by explicit configuration passed
     *                             as other parameters
     */
    KafkaRdfPayloadSource(String bootstrapServers, Set<String> topics, String groupId, String keyDeserializerClass,
                          int maxPollRecords, KafkaReadPolicy<TKey, RdfPayload> policy, boolean autoCommit,
                          OffsetStore offsetStore, Duration lagReportInterval, Properties properties) {
        super(bootstrapServers, topics, groupId, keyDeserializerClass, RdfPayloadDeserializer.class.getCanonicalName(),
              maxPollRecords, policy, autoCommit, offsetStore, lagReportInterval, properties);
    }

    /**
     * A Builder for Dataset Graph event sources
     *
     * @param <TKey> Event key type
     */
    public static class Builder<TKey>
            extends AbstractKafkaEventSourceBuilder<TKey, RdfPayload, KafkaRdfPayloadSource<TKey>, Builder<TKey>> {

        /**
         * Creates a new builder
         */
        Builder() {
            this.valueDeserializerClass = RdfPayloadDeserializer.class.getCanonicalName();
        }

        @Override
        public KafkaRdfPayloadSource<TKey> build() {
            return new KafkaRdfPayloadSource<>(this.bootstrapServers, this.topics, this.groupId,
                                               this.keyDeserializerClass, this.maxPollRecords, this.readPolicy,
                                               this.autoCommit, this.externalOffsetStore, this.lagReportInterval,
                                               this.properties);
        }
    }
}
