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

import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.automatic.AutoFromBeginning;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.automatic.AutoFromEarliest;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.automatic.AutoFromEnd;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.automatic.AutoFromLatest;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.automatic.AutoFromOffset;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.manual.ManualFromBeginning;

/**
 * Provides access to predefined {@link KafkaReadPolicy} instances to control how you want a
 * {@link KafkaEventSource} to read from Kafka topic(s)
 */
public class KafkaReadPolicies {

    /**
     * Private constructor to prevent instantiation
     */
    private KafkaReadPolicies() {
    }

    /**
     * Read all events from the beginning, possibly reading events multiple times
     *
     * @param <TKey>   Key Type
     * @param <TValue> Value Type
     * @return Read Policy
     */
    public static <TKey, TValue> KafkaReadPolicy<TKey, TValue> fromBeginning() {
        return new AutoFromBeginning<>();
    }

    /**
     * Read all events from the beginning, possibly reading events multiple times
     *
     * @param <TKey>   Key Type
     * @param <TValue> Value Type
     * @return Read Policy
     */
    public static <TKey, TValue> KafkaReadPolicy<TKey, TValue> manualFromBeginning() {
        return new ManualFromBeginning<>();
    }

    /**
     * Read all events from the beginning, possibly ignoring pre-existing events
     *
     * @param <TKey>   Key Type
     * @param <TValue> Value Type
     * @return Read Policy
     */
    public static <TKey, TValue> KafkaReadPolicy<TKey, TValue> fromEnd() {
        return new AutoFromEnd<>();
    }

    /**
     * Read all events starting from the earliest unread
     * <p>
     * If the topic has not previously been read by the Consumer Group then this is equivalent to
     * {@link #fromBeginning()} since it starts from the earliest available event.  If the topic has previously been
     * read by the Consumer Group then reading resumes from the most recent event read.
     * </p>
     *
     * @param <TKey>   Key Type
     * @param <TValue> Value Type
     * @return Read Policy
     */
    public static <TKey, TValue> KafkaReadPolicy<TKey, TValue> fromEarliest() {
        return new AutoFromEarliest<>();
    }

    /**
     * Reads events starting from the latest
     * <p>
     * If the topic has not previously been read by the Consumer Group then starts from the latest offset i.e. only
     * reads new events.  If the topic has previously been read by the Consumer Group then reading resumes from the most
     * recent event read.
     * </p>
     *
     * @param <TKey>   Key Type
     * @param <TValue> Value Type
     * @return Read Policy
     */
    public static <TKey, TValue> KafkaReadPolicy<TKey, TValue> fromLatest() {
        return new AutoFromLatest<>();
    }

    /**
     * Reads events starting from specific offsets
     *
     * @param offsets       Offsets
     * @param defaultOffset Default offset to use for any partition whose desired offset is not explicitly specified
     * @param <TKey>        Key Type
     * @param <TValue>      Value Type
     * @return Read Policy
     */
    public static <TKey, TValue> KafkaReadPolicy<TKey, TValue> fromOffsets(Map<TopicPartition, Long> offsets,
                                                                           long defaultOffset) {
        return new AutoFromOffset<>(offsets, defaultOffset);
    }
}
