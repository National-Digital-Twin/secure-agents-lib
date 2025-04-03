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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.kafka.common.TopicPartition;

/**
 * An automatic read policy that seeks within partitions when they are assigned to it guaranteeing that the seek happens
 * only once for each partition
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public abstract class AbstractAutoSeekingPolicy<TKey, TValue> extends AbstractAutoReadPolicy<TKey, TValue> {
    private final Set<TopicPartition> seekedPartitions = new HashSet<>();

    @Override
    protected final void seek(Collection<TopicPartition> partitions) {
        synchronized (this.seekedPartitions) {
            // Due to consumer group re-balances we could get reassigned the same partitions during the course of our
            // lifetime in which case we do not want to repeatedly seek to the desired position within the topic as that
            // would cause us to redo work everytime a re-balance happens
            List<TopicPartition> needsSeek = new ArrayList<>();
            for (TopicPartition partition : partitions) {
                if (!seekedPartitions.contains(partition)) {
                    needsSeek.add(partition);
                }
            }
            if (!needsSeek.isEmpty()) {
                seekInternal(needsSeek);
                seekedPartitions.addAll(needsSeek);
            }
        }
    }

    /**
     * Performs the actual seek operation on the given partitions
     *
     * @param partitions Partitions
     */
    protected abstract void seekInternal(Collection<TopicPartition> partitions);
}
