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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;

/**
 * An event source that uses an internal buffer
 * <p>
 * The buffered events are stored as an intermediate type to allow derived implementations to store additional
 * information alongside the actual event.  For example the {@link KafkaEventSource} stores the actual
 * {@link org.apache.kafka.clients.consumer.ConsumerRecord} instances since it needs this information to be able to
 * reliably track committed offsets.
 * </p>
 *
 * @param <TIntermediate> Intermediate event type
 * @param <TKey>          Event key type
 * @param <TValue>        Event value type
 */
public abstract class AbstractBufferedEventSource<TIntermediate, TKey, TValue> implements EventSource<TKey, TValue> {

    /**
     * Tracks whether the event source has been closed
     */
    protected volatile boolean closed = false;
    /**
     * The buffer of intermediate events
     */
    protected final Queue<TIntermediate> events = new LinkedList<>();

    /**
     * Creates a new buffered event source
     */
    public AbstractBufferedEventSource() {
    }

    @Override
    public final boolean availableImmediately() {
        return !this.closed && !this.events.isEmpty();
    }

    @Override
    public boolean isExhausted() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public final boolean isClosed() {
        return this.closed;
    }

    /**
     * Decodes from the internal event representation used when buffering events to the actual {@link Event} instance
     * <p>
     * The implementation must return {@code null} if it receives a {@code null} input since {@code null} is used to
     * represent no event available.
     * </p>
     *
     * @param internalEvent Internal event representation
     * @return Decoded event
     */
    protected abstract Event<TKey, TValue> decodeEvent(TIntermediate internalEvent);

    @Override
    public final Event<TKey, TValue> poll(Duration timeout) {
        if (this.closed) {
            throw new IllegalStateException("Event source has been closed");
        }

        // If we have some events buffered continue returning them
        if (!events.isEmpty()) {
            return this.decodeEvent(events.poll());
        }

        // The buffer has now been exhausted, allow the derived implementation chance to do any state management it
        // needs and then ask it to refill the buffer
        bufferExhausted();
        tryFillBuffer(timeout);

        // Return the next buffered event, or null if no events available
        return this.decodeEvent(events.poll());
    }

    /**
     * Try to refill the buffer with the next available events.  If no new events are available within the timeout leave
     * the buffer unmodified.
     *
     * @param timeout Timeout
     */
    protected abstract void tryFillBuffer(Duration timeout);

    /**
     * Gives derived implementations a chance to carry out any state management they might need to track the fact that
     * all the previously buffered events have now been processed.
     */
    protected void bufferExhausted() {
        // No-op
    }
}
