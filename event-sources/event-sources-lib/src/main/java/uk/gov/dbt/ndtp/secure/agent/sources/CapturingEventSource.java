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
package uk.gov.dbt.ndtp.secure.agent.sources;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;

/**
 * An event source decorator that captures the events to some additional sink prior to returning them
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class CapturingEventSource<TKey, TValue> implements EventSource<TKey, TValue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapturingEventSource.class);

    private final EventSource<TKey, TValue> underlying;
    private final Sink<Event<TKey, TValue>> capture;

    /**
     * Creates a new capturing event source
     *
     * @param source  Underlying event source
     * @param capture Capture sink
     */
    public CapturingEventSource(EventSource<TKey, TValue> source, Sink<Event<TKey, TValue>> capture) {
        Objects.requireNonNull(source, "Event source cannot be null");
        Objects.requireNonNull(capture, "Capture sink cannot be null");
        this.underlying = source;
        this.capture = capture;
    }

    @Override
    public boolean availableImmediately() {
        return this.underlying.availableImmediately();
    }

    @Override
    public boolean isExhausted() {
        return this.underlying.isExhausted();
    }

    @Override
    public void close() {
        this.underlying.close();
    }

    @Override
    public boolean isClosed() {
        return this.underlying.isClosed();
    }

    @Override
    public Event<TKey, TValue> poll(Duration timeout) {
        Event<TKey, TValue> next = this.underlying.poll(timeout);
        if (next != null) {
            try {
                this.capture.send(next);
            } catch (SinkException e) {
                LOGGER.warn("Failed to capture event: {}", e.getMessage());
            }
        }
        return next;
    }

    @Override
    public Long remaining() {
        return this.underlying.remaining();
    }

    @Override
    public void processed(Collection<Event> processedEvents) {
        this.underlying.processed(processedEvents);
    }
}
