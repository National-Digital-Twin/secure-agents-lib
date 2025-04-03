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

import java.util.Collection;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;

/**
 * Represents a policy indicating how a {@link KafkaEventSource} should behave in
 * terms of which events it should read.  Intended to allow for easily configuring behaviours of pipelines.
 * <p>
 * A read policy also acts as a {@link ConsumerRebalanceListener} allowing it to seek to the appropriate points in topic
 * partitions when using a policy that uses the Kafka automatic partition assignment strategy (via
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer#subscribe(Collection, ConsumerRebalanceListener)}.
 * </p>
 */
public interface KafkaReadPolicy<TKey, TValue> extends ConsumerRebalanceListener {

    /**
     * Indicates whether this read policy is based upon Kafka's Consumer Group subscription model or not
     * <p>
     * There are two ways to read events from a Kafka topic:
     * </p>
     * <ol>
     * <li>Consumer Group Subscription i.e. automatic partition assignment handled by the Kafka brokers (server side)</li>
     * <li>Partition Assignment i.e. manual partition assignment handled by the Kafka consumers (client side)</li>
     * </ol>
     * <p>
     * It can be useful to know what model a read policy is using because it does have an impact on how an event source
     * is going to behave.  With the automatic partition assignment there can be quite long hangs while the client waits
     * for the server to assign it partitions.  Whereas with manual partition assignment the only real delay is waiting
     * for the server to tell us which partitions are available.
     * </p>
     *
     * @return True if using Consumer Group subscription, false otherwise
     */
    boolean isSubscriptionBased();

    /**
     * Provides the policy the opportunity to modify the Kafka {@link org.apache.kafka.clients.consumer.ConsumerConfig}
     * properties that are being declared on the {@link Properties} object used to instantiate a consumer.
     *
     * @param props Properties
     */
    void prepareConsumerConfiguration(Properties props);

    /**
     * Sets the consumer that will be used with this read policy
     * <p>
     * <strong>MUST</strong> only be set once, throws an {@link IllegalStateException} if this is called multiple
     * times.
     * </p>
     *
     * @param consumer Consumer
     * @throws IllegalStateException Thrown if this is called multiple times for this instance
     */
    void setConsumer(Consumer<TKey, TValue> consumer);

    /**
     * Configures the consumer to start receiving events
     *
     * @param topic Topic to start receiving events on
     */
    void startEvents(String topic);

    /**
     * Requests that the read positions for the given topic (if any) be logged
     *
     * @param topic Topic
     */
    void logReadPositions(String topic);

    /**
     * Gets the current lag for the given topic
     *
     * @param topic Topic
     * @return Current lag, may be {@code null} if currently unknown i.e. not currently reading events from and topic
     */
    Long currentLag(String topic);

    /**
     * Configures the currently configured consumer to stop receiving events
     *
     * @param topic Topic to stop receiving events on
     */
    void stopEvents(String topic);
}
