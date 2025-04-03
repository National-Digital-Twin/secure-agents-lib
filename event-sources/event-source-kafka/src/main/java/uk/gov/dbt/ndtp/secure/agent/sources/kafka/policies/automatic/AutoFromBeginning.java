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
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Kafka read policy that reads all assigned partitions (which are assigned automatically via Kafka Consumer Groups)
 * from the beginning.
 * <p>
 * Note that this does necessarily mean that all events in a topic are read because the consumer may only be assigned a
 * subset of partitions.
 * </p>
 * <p>
 * Note that use of this policy means that events in a topic <strong>MAY</strong> be read and processed multiple times.
 * </p>
 *
 * @param <TKey>   Key Type
 * @param <TValue> Value Type
 */
public class AutoFromBeginning<TKey, TValue> extends AbstractAutoSeekingPolicy<TKey, TValue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoFromBeginning.class);

    @Override
    protected void seekInternal(Collection<TopicPartition> partitions) {
        this.consumer.seekToBeginning(partitions);
    }
}
