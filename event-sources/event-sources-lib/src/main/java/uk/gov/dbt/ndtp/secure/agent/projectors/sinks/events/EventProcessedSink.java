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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;

/**
 * A terminal sink that merely calls the {@link EventSource#processed(Collection)}
 * method once events reach it, either per event or when a batch threshold for events is reached
 *
 * @param <TKey>   Event Key type
 * @param <TValue> Event Value type
 */
public class EventProcessedSink<TKey, TValue> implements Sink<Event<TKey, TValue>> {

    /**
     * Batch size used to request that no batching be performed
     */
    public static final int NO_BATCHING = 1;
    @SuppressWarnings("rawtypes")
    private final Map<EventSource, List<Event<TKey, TValue>>> events = new HashMap<>();
    private final int batchSize;

    /**
     * Creates an event processed sink
     *
     * @param batchSize Batch size
     */
    EventProcessedSink(int batchSize) {
        if (batchSize <= 0) throw new IllegalArgumentException("batchSize must be >= 1");
        this.batchSize = batchSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void send(Event<TKey, TValue> event) {
        // If the event has a source then report it as processed, this may not happen immediately if a batch size for
        // reporting has been configured
        if (event.source() != null) {
            if (this.batchSize == NO_BATCHING) {
                event.source().processed(List.of(event));
            } else {
                // Add to a batch, potentially reporting that batch if the batch size has been reached
                List<Event<TKey, TValue>> batch =
                        this.events.computeIfAbsent(event.source(), k -> new ArrayList<>());
                batch.add(event);
                if (batch.size() >= this.batchSize) {
                    event.source().processed(batch);
                    batch.clear();
                }
            }
        }
    }

    /**
     * Gets the number of incomplete batches i.e. batches which have not been reported as processed to their respective
     * event sources
     *
     * @return Incomplete batches
     */
    public long incompleteBatches() {
        return this.events.values().stream().filter(b -> !b.isEmpty()).count();
    }

    /**
     * Gets the total number of batched events i.e. events which have not been reported as processed to their respective
     * event sources
     *
     * @return Total batched events
     */
    public long batchedEvents() {
        return this.events.values().stream().map(List::size).reduce(Integer::sum).orElse(0);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void close() {
        // Nothing to do if batching was disabled
        if (this.batchSize == NO_BATCHING) {
            return;
        }

        // Upon close report any incomplete batches as processed
        for (Map.Entry<EventSource, List<Event<TKey, TValue>>> entry : this.events.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                entry.getKey().processed(entry.getValue());
                entry.getValue().clear();
            }
        }
    }

    /**
     * Creates a builder for an event processed sink
     *
     * @param <TKey>   Event key type
     * @param <TValue> Event value type
     * @return Builder
     */
    public static <TKey, TValue> Builder<TKey, TValue> create() {
        return new Builder<>();
    }

    /**
     * Builder for event processed sinks
     *
     * @param <TKey>   Event Key type
     * @param <TValue> Event Value type
     */
    public static class Builder<TKey, TValue>
            implements SinkBuilder<Event<TKey, TValue>, EventProcessedSink<TKey, TValue>> {

        private int batchSize = NO_BATCHING;

        /**
         * Specifies that the resulting sink should not batch up its reporting of events processed
         *
         * @return Builder
         */
        public Builder<TKey, TValue> noBatching() {
            return batchSize(NO_BATCHING);
        }

        /**
         * Specifies that the resulting sink should batch up its reporting of events processed into batches of the given
         * size
         *
         * @param batchSize Batch size
         * @return Builder
         */
        public Builder<TKey, TValue> batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        @Override
        public EventProcessedSink<TKey, TValue> build() {
            return new EventProcessedSink<>(this.batchSize);
        }
    }
}
