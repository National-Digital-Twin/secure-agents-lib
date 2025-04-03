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
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import uk.gov.dbt.ndtp.secure.agent.payloads.RdfPayload;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicy;

public class MockKafkaRdfPayloadSource extends KafkaRdfPayloadSource<Integer> {

    /**
     * Creates a new event source backed by a Kafka topic
     *
     * @param bootstrapServers Kafka Bootstrap servers
     * @param topics            Kafka topic(s) to subscribe to
     * @param groupId          Kafka Consumer Group ID
     * @param maxPollRecords   Maximum events to retrieve and buffer in one Kafka
     *                         {@link org.apache.kafka.clients.consumer.KafkaConsumer#poll(Duration)} request.
     * @param autoCommit       Whether the event source will automatically commit Kafka positions
     * @param policy           Kafka Read Policy to control what events to read from the configured topic
     */
    public MockKafkaRdfPayloadSource(String bootstrapServers, Set<String> topics, String groupId, int maxPollRecords,
                                     KafkaReadPolicy<Integer, RdfPayload> policy, boolean autoCommit,
                                     Collection<Event<Integer, RdfPayload>> events) {
        super(bootstrapServers, topics, groupId, IntegerDeserializer.class.getCanonicalName(), maxPollRecords,
              new MockReadPolicy<>(policy, events), autoCommit, null, Duration.ofMinutes(1), null);
    }

    @Override
    protected Consumer<Integer, RdfPayload> createConsumer(Properties props) {
        MockConsumer<Integer, RdfPayload> mock = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        return mock;
    }

    @Override
    protected AdminClient createAdminClient(Properties props) {
        return null;
    }
}
