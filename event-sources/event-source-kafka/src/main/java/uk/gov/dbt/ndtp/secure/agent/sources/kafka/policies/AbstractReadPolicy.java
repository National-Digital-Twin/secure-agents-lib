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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;

/**
 * Abstract base class for read policies
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public abstract class AbstractReadPolicy<TKey, TValue> implements KafkaReadPolicy<TKey, TValue> {
    /**
     * The Kafka consumer that is being used
     */
    protected Consumer<TKey, TValue> consumer = null;

    /**
     * We create a basic cache to control the amount of repeated status messages logged that add no value.
     */
    protected static final Cache<String, Boolean> LOGGING_CACHE =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .initialCapacity(1)
                    .maximumSize(1)
                    .build();


    @Override
    public void prepareConsumerConfiguration(Properties props) {
        // Nothing to do
    }

    @Override
    public final void setConsumer(Consumer<TKey, TValue> consumer) {
        if (this.consumer != null) {
            throw new IllegalStateException("Cannot set the consumer multiple times");
        }
        this.consumer = consumer;
    }

    /**
     * Gets the set of unique topics affected by a partition re-balance operation
     *
     * @param partitions Partitions
     * @return Set of affected topics
     */
    protected final Set<String> getAffectedTopics(Collection<TopicPartition> partitions) {
        return partitions.stream().map(TopicPartition::topic).collect(Collectors.toSet());
    }

    /**
     * Gets the set of relevant partitions for a given topic
     *
     * @param partitions Partitions to filter
     * @param topic      Topic
     * @return Relevant partitions
     */
    protected final Set<TopicPartition> getRelevantPartitions(Collection<TopicPartition> partitions, String topic) {
        return partitions.stream()
                         .filter(p -> Objects.equals(p.topic(), topic))
                         .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Seeks to the events in the topic(s) that this read policy wishes to read
     *
     * @param partitions Topic Partitions
     */
    protected void seek(Collection<TopicPartition> partitions) {
        // No-op by default

        // The read policy may already have set some seek settings via the Consumer Config which render this operation
        // unnecessary, but for some read policies they may wish to explicitly seek within partitions.
    }

    /**
     * Logs the positions and lag for the given partitions
     *
     * @param partitions Partitions
     * @param logger     Logger to log to
     */
    protected void logPartitionPositions(Collection<TopicPartition> partitions, Logger logger) {
        if (CollectionUtils.isEmpty(partitions)) {
            return;
        }

        // We want to accurately log the position and lag for each partition
        // Since currentLag() is computed only from local metadata we have to force Kafka to look up the end offsets
        // and positions for each partition prior to asking for the current lag
        this.consumer.endOffsets(partitions);
        partitions.forEach(p -> {
            long position = this.consumer.position(p);
            OptionalLong currentLag = this.consumer.currentLag(p);
            String knownLag = currentLag.isPresent() ? String.format("%,d", currentLag.getAsLong()) : "unknown";
            String key = String.format("%s-%d-%s", p , position , knownLag);
            if(LOGGING_CACHE.getIfPresent(key) == null) {
                FmtLog.info(logger, "Kafka Partition %s is at position %,d with a current lag of %s", p,
                        position, knownLag);
                LOGGING_CACHE.put(key, Boolean.TRUE);
            }
        });
    }

    /**
     * Calculates the total lag for the given partitions, or {@code null} if it cannot be calculated
     *
     * @param partitions Partitions
     * @return Total lag
     */
    protected Long calculateLag(Collection<TopicPartition> partitions) {
        if (CollectionUtils.isEmpty(partitions)) {
            return null;
        }

        this.consumer.endOffsets(partitions);
        AtomicBoolean anyUnknown = new AtomicBoolean(false);
        AtomicLong totalLag = new AtomicLong();
        partitions.forEach(p -> {
            this.consumer.position(p);
            OptionalLong currentLag = this.consumer.currentLag(p);
            if (currentLag.isPresent()) {
                totalLag.addAndGet(currentLag.getAsLong());
            } else {
                anyUnknown.set(true);
            }
        });

        if (anyUnknown.get()) {
            return null;
        } else {
            return totalLag.get();
        }
    }
}
