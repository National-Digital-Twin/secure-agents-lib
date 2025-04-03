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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.common.TopicPartition;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicy;

public class MockReadPolicy<TKey, TValue> implements KafkaReadPolicy<TKey, TValue> {

    private final Collection<Event<TKey, TValue>> events;
    private final KafkaReadPolicy<TKey, TValue> policy;
    private MockConsumer<TKey, TValue> mock;

    public MockReadPolicy(KafkaReadPolicy<TKey, TValue> policy, Collection<Event<TKey, TValue>> events) {
        this.policy = policy;
        this.events = events;
    }

    @Override
    public boolean isSubscriptionBased() {
        return this.policy.isSubscriptionBased();
    }

    @Override
    public void prepareConsumerConfiguration(Properties props) {
        this.policy.prepareConsumerConfiguration(props);
    }

    @Override
    public void setConsumer(Consumer<TKey, TValue> consumer) {
        this.policy.setConsumer(consumer);
        if (consumer instanceof MockConsumer<TKey, TValue> mockConsumer) {
            this.mock = mockConsumer;
        }
    }

    @Override
    public void startEvents(String topic) {
        // Create the mock partition we will be assigned and set its offsets
        TopicPartition partition = new TopicPartition(topic, 0);
        this.mock.updateBeginningOffsets(Map.of(partition, 0L));
        this.mock.updateEndOffsets(Map.of(partition, (long) events.size()));

        this.policy.startEvents(topic);

        if (this.policy.isSubscriptionBased()) {
            this.mock.rebalance(Collections.singletonList(partition));
        } else {
            this.mock.assign(Collections.singletonList(partition));
        }

        int offset = 0;
        for (Event<TKey, TValue> event : events) {
            this.mock.addRecord(
                    new ConsumerRecord<>(partition.topic(), partition.partition(), offset, event.key(), event.value()));
            offset++;
        }
    }

    @Override
    public void logReadPositions(String topic) {
        this.policy.logReadPositions(topic);
    }

    @Override
    public Long currentLag(String topic) {
        return this.policy.currentLag(topic);
    }

    @Override
    public void stopEvents(String topic) {
        this.policy.stopEvents(topic);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        this.policy.onPartitionsRevoked(partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        this.policy.onPartitionsAssigned(partitions);


    }
}
