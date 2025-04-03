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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.automatic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.AbstractReadPolicy;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.manual.AbstractManualReadPolicy;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Kafka read policies that rely upon Kafka's Consumer Groups subscription model.
 * <p>
 * In this model consumers within each Consumer Group are automatically assigned a fair share of partitions within the
 * topics they {@link KafkaConsumer#subscribe(Collection, ConsumerRebalanceListener)} to.  Thus the policy is informed
 * when topic partitions are assigned to it, or revoked from it, and behaves accordingly.  Therefore any policy derived
 * from this base class <strong>MAY</strong> only provide a subset of the events in a topic, depending on whether there
 * are multiple active consumers associated with the Consumer Group.
 * </p>
 * <p>
 * This differs from policies derived from {@link AbstractManualReadPolicy} which guarantee to read all partitions for a
 * topic.  If you <strong>MUST</strong> read all events from all partitions then do not use a subscription based
 * policy.
 * </p>
 *
 * @param <TKey>   Key Type
 * @param <TValue> Value Type
 */
public abstract class AbstractAutoReadPolicy<TKey, TValue> extends AbstractReadPolicy<TKey, TValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAutoReadPolicy.class);

    @Override
    public boolean isSubscriptionBased() {
        return true;
    }

    @Override
    public void startEvents(String topic) {
        if (!consumer.subscription().contains(topic)) {
            if (consumer.subscription().isEmpty()) {
                consumer.subscribe(Collections.singletonList(topic), this);
            } else {
                Set<String> newTopics = new LinkedHashSet<>(consumer.subscription());
                newTopics.add(topic);
                consumer.subscribe(newTopics, this);
            }

            // NB - Because Kafka consumer re-balancing is an automatic process sometimes our first attempt to read
            //      events from the topic can hang until such time as the Kafka brokers trigger a re-balance and inform
            //      us of our assigned partitions i.e. it's a server side process that can block us
            //      Therefore, when we are explicitly subscribing to new topics we ask the server for a re-balance to
            //      avoid this potential hang.
            consumer.enforceRebalance();

            LOGGER.info("Subscribed to Kafka topic {} using Group ID {}", topic, consumer.groupMetadata().groupId());
        } else {
            LOGGER.debug("Already subscribed to Kafka topic {}, caller may be using the API incorrectly", topic);
        }
    }

    @Override
    public void logReadPositions(String topic) {
        this.logPartitionPositions(this.getRelevantPartitions(consumer.assignment(), topic), LOGGER);
    }

    @Override
    public Long currentLag(String topic) {
        return this.calculateLag(this.getRelevantPartitions(consumer.assignment(), topic));
    }

    @Override
    public void stopEvents(String topic) {
        if (consumer.subscription().contains(topic)) {
            // Want to log our positions within these topic partitions on stop as this will help inform users how much
            // of the partitions has been successfully read
            this.logPartitionPositions(this.getRelevantPartitions(consumer.assignment(), topic), LOGGER);

            if (consumer.subscription().size() == 1) {
                consumer.unsubscribe();
            } else {
                Set<String> newTopics = new HashSet<>(consumer.subscription());
                newTopics.remove(topic);
                consumer.subscribe(newTopics, this);
            }
        } else {
            LOGGER.debug("Not subscribed to Kafka topic {}, caller may be using the API incorrectly", topic);
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        Set<String> affectedTopics = getAffectedTopics(partitions);
        LOGGER.info("Revoked {} partitions for Kafka topic(s) {}", partitions.size(),
                    StringUtils.join(affectedTopics, ", "));
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        Set<String> affectedTopics = getAffectedTopics(partitions);
        LOGGER.info("Assigned {} partitions for Kafka topic {}", partitions.size(),
                    StringUtils.join(affectedTopics, ", "));

        seek(partitions);

        logPartitionPositions(partitions, LOGGER);
    }

}
