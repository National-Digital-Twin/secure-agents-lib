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
import java.util.Properties;
import java.util.Set;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicy;

public class MockKafkaEventSource<TKey, TValue> extends KafkaEventSource<TKey, TValue> {

    private MockConsumer<TKey, TValue> mock;

    /**
     * Creates a new event source backed by a Kafka topic
     *
     * @param bootstrapServers       Kafka Bootstrap servers
     * @param topics                 Kafka topic(s) to subscribe to
     * @param groupId                Kafka Consumer Group ID
     * @param keyDeserializerClass   Key deserializer class
     * @param valueDeserializerClass Value deserializer class
     * @param maxPollRecords         Maximum events to retrieve and buffer in one Kafka
     *                               {@link KafkaConsumer#poll(Duration)} request.
     * @param autoCommit             Whether the event source will automatically commit Kafka positions
     * @param policy                 Kafka Read Policy to control what events to read from the configured topic
     */
    public MockKafkaEventSource(String bootstrapServers, Set<String> topics, String groupId,
                                String keyDeserializerClass, String valueDeserializerClass, int maxPollRecords,
                                KafkaReadPolicy policy, boolean autoCommit, Collection<Event<TKey, TValue>> events) {
        super(bootstrapServers, topics, groupId, keyDeserializerClass, valueDeserializerClass, maxPollRecords,
              new MockReadPolicy(policy, events), autoCommit, null, Duration.ofMinutes(1), null);
    }

    @Override
    protected Consumer<TKey, TValue> createConsumer(Properties props) {
        this.mock = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        return this.mock;
    }

    /**
     * Gets the mock consumer, used to modify mock behaviour during tests
     *
     * @return Mock consumer
     */
    MockConsumer<TKey, TValue> getMockConsumer() {
        return this.mock;
    }

    @Override
    protected AdminClient createAdminClient(Properties props) {
        return null;
    }
}
