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
package uk.gov.dbt.ndtp.secure.agent.sources.memory;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;

/**
 * An in-memory event source intended primarily for testing
 *
 * @param <TKey>   Event key type
 * @param <TValue> Event value type
 */
public class InMemoryEventSource<TKey, TValue> implements EventSource<TKey, TValue> {

    private final Queue<Event<TKey, TValue>> events = new LinkedList<>();
    private boolean closed = false;

    /**
     * Creates a new in-memory event source
     *
     * @param events Events that the source will provide
     */
    public InMemoryEventSource(Collection<Event<TKey, TValue>> events) {
        Objects.requireNonNull(events, "Events cannot be null");
        CollectionUtils.addAll(this.events, events);
    }

    @Override
    public boolean availableImmediately() {
        return !this.closed && !this.events.isEmpty();
    }

    @Override
    public boolean isExhausted() {
        return !availableImmediately();
    }

    @Override
    public void close() {
        this.closed = true;
        this.events.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public Event<TKey, TValue> poll(Duration timeout) {
        checkNotClosed();

        if (!this.events.isEmpty()) {
            return this.events.poll();
        }

        return null;
    }

    private void checkNotClosed() {
        if (this.closed) {
            throw new IllegalStateException("Event Source has been closed");
        }
    }

    @Override
    public Long remaining() {
        return (long) this.events.size();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void processed(Collection<Event> processedEvents) {
        checkNotClosed();
        // No-op
    }
}
