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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.manual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.AbstractReadPolicy;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.automatic.AbstractAutoReadPolicy;

/**
 * Abstract base class for Kafka read policies that use the manual partition assignment strategy.
 * <p>
 * This means that when starting events for a topic the policy first obtains available partitions via
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer#partitionsFor(String)} and then calls
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer#assign(Collection)} to manually assign the relevant partitions
 * for reading.  This is as opposed to the policies derived from {@link AbstractAutoReadPolicy} where Kafka's Consumer
 * Group subscription model is used so the policy is assigned a fair-share of the available partitions.  Therefore any
 * policy derived from this base class will automatically assign itself all available partitions for a topic.
 * </p>
 * <p>Note that even with this policy Kafka's consumer group offset management is still implicitly used, unless a
 * policy specifically seeks to a specific offset within the partitions.
 * </p>
 *
 * @param <TKey>   Key Type
 * @param <TValue> Value Type
 */
public class AbstractManualReadPolicy<TKey, TValue> extends AbstractReadPolicy<TKey, TValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractManualReadPolicy.class);

    @Override
    public boolean isSubscriptionBased() {
        return false;
    }

    @Override
    public void startEvents(String topic) {
        Set<TopicPartition> allAssignments = new LinkedHashSet<>(this.consumer.assignment());
        Set<TopicPartition> currentAssignments = getRelevantPartitions(allAssignments, topic);

        List<PartitionInfo> available = this.consumer.partitionsFor(topic);
        List<TopicPartition> newPartitions = new ArrayList<>();
        for (PartitionInfo info : available) {
            TopicPartition partition = new TopicPartition(topic, info.partition());
            if (!currentAssignments.contains(partition)) {
                allAssignments.add(partition);
                newPartitions.add(partition);
            }
        }

        if (newPartitions.isEmpty()) {
            LOGGER.warn("Attempted to subscribe to Kafka topic {} but there are no partitions available for it", topic);
            return;
        }

        this.consumer.assign(new ArrayList<>(allAssignments));
        LOGGER.info("Subscribed to Kafka topic {} using manual partition assignment of {} partitions", topic,
                    newPartitions.size());
        seek(newPartitions);
        logPartitionPositions(newPartitions, LOGGER);
    }

    @Override
    public void logReadPositions(String topic) {
        Set<TopicPartition> allAssignments = new LinkedHashSet<>(this.consumer.assignment());
        Set<TopicPartition> currentAssignments = getRelevantPartitions(allAssignments, topic);
        this.logPartitionPositions(currentAssignments, LOGGER);
    }

    @Override
    public Long currentLag(String topic) {
        Set<TopicPartition> allAssignments = new LinkedHashSet<>(this.consumer.assignment());
        Set<TopicPartition> currentAssignments = getRelevantPartitions(allAssignments, topic);
        return this.calculateLag(currentAssignments);
    }

    @Override
    public void stopEvents(String topic) {
        Set<TopicPartition> allAssignments = new LinkedHashSet<>(this.consumer.assignment());
        Set<TopicPartition> currentAssignments = getRelevantPartitions(allAssignments, topic);

        // Want to log our positions within these topic partitions on stop as this will help inform users how much
        // of the partitions has been successfully read
        this.logPartitionPositions(currentAssignments, LOGGER);

        if (!currentAssignments.isEmpty()) {
            allAssignments.removeAll(currentAssignments);
            this.consumer.assign(new ArrayList<>(allAssignments));
        } else {
            LOGGER.debug("Not assigned any partitions for topic {}, caller may be using the API incorrectly", topic);
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // Should never be called when using manual partition assignment
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Should never be called when using manual partition assignment
    }
}
