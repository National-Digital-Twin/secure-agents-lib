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
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;

/**
 * A Kafka read policy that reads events from specific offsets onwards from all assigned partitions (which are assigned
 * automatically via Kafka Consumer groups).
 *
 * @param <TKey>   Key Type
 * @param <TValue> Value Type
 */
public class AutoFromOffset<TKey, TValue> extends AbstractAutoSeekingPolicy<TKey, TValue> {

    private final Map<TopicPartition, Long> desiredOffsets;
    private final long defaultOffset;

    /**
     * Creates a new policy that defaults to reading all partitions from offset zero
     */
    public AutoFromOffset() {
        this(null, 0);
    }

    /**
     * Creates a new policy that reads from the given offsets
     *
     * @param desiredOffsets Desired starting offsets for specific partitions
     * @param defaultOffset  Default offset to use for any partition not explicitly specified
     */
    public AutoFromOffset(Map<TopicPartition, Long> desiredOffsets, long defaultOffset) {
        this.desiredOffsets = desiredOffsets != null ? new HashMap<>(desiredOffsets) : Collections.emptyMap();
        if (defaultOffset < 0) {
            throw new IllegalArgumentException("defaultOffset must be >= 0");
        }
        this.defaultOffset = defaultOffset;
    }

    @Override
    protected void seekInternal(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            long offset = selectOffset(partition);
            this.consumer.seek(partition, offset);
        }
    }

    /**
     * Selects the offset to read from for the given partition
     *
     * @param partition Partition
     * @return Offset to read from
     */
    private long selectOffset(TopicPartition partition) {
        return desiredOffsets.getOrDefault(partition, defaultOffset);
    }
}
