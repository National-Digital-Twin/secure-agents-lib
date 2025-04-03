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

/**
 * Represents a source of events
 *
 * @param <TKey>   Event key type
 * @param <TValue> Event value type
 */
public interface EventSource<TKey, TValue> {

    /**
     * Gets whether the source has events immediately available i.e. will calling {@link #poll(Duration)} return
     * immediately without blocking?
     * <p>
     * If the source returns {@code true} then it guarantees that the next call to {@link #poll(Duration)} will return
     * an event.
     * </p>
     *
     * @return True if events are immediately available, false otherwise
     */
    boolean availableImmediately();

    /**
     * Gets whether the source is exhausted i.e. there are no further events available, nor will they ever be.
     * <p>
     * If an event source is unbounded, i.e. there may always be new events available in the future, then this method
     * <strong>MUST</strong> always return {@code false} <em>unless</em> the event source has been closed.
     * </p>
     * <p>
     * If a caller needs to distinguish between true source exhaustion and source closure then they can check the
     * closure status via the {@link #isClosed()} method.
     * </p>
     *
     * @return True if events are exhausted or the source is closed, false otherwise
     */
    boolean isExhausted();

    /**
     * Closes the event source
     * <p>
     * After this has been called any attempt to read further events from this source <strong>MUST</strong> fail.
     * </p>
     */
    void close();

    /**
     * Indicates whether the event source has been closed
     *
     * @return True if closed, false otherwise
     */
    boolean isClosed();

    /**
     * Polls for the next event from the source, potentially blocking until the next event is available.
     * <p>
     * If the next event cannot be provided within the given timeout then a {@code null} should be returned.
     * </p>
     * <p>
     * Callers can consult the {@link #availableImmediately()} and {@link #isExhausted()} methods to determine whether
     * this method is likely to block, and/or succeed at all.  If {@link #availableImmediately()} returns {@code true}
     * then they should expect this method to return the next event immediately.  Conversely if {@link #isExhausted()}
     * returns {@code true} then this method should always return {@code null} or throw {@link IllegalStateException} if
     * the source is already closed.
     * </p>
     *
     * @param timeout Maximum time to wait before returning {@code null}
     * @return Next event, or {@code null} if no event is currently available
     * @throws IllegalStateException Thrown if this is called after the event source has been closed
     * @throws EventSourceException  Thrown if the event source encounters an unrecoverable error.
     *                               <p>
     *                               This <strong>MUST NOT</strong> be used for situations where they simply aren't any
     *                               events available currently.  In that case the implementation <strong>MUST</strong>
     *                               return {@code null} to indicate this.
     *                               </p>
     */
    Event<TKey, TValue> poll(Duration timeout);

    /**
     * Reports the total remaining events at the time of calling (if known).
     * <p>
     * May report {@code null} if the event source is unable to report this at the time of calling.
     * </p>
     * <p>
     * Callers <strong>MUST</strong> assume that this number may change over time, both as the calling process reads
     * events and as the event source is populated with further events.
     * </p>
     *
     * @return Total remaining events, or {@code null} if currently unknown
     */
    Long remaining();

    /**
     * Tells the event source that the provided events have been processed.
     * <p>
     * This is intended to help implementations ensure that every event is processed and that an application restart
     * does not result in some events getting missed.  What the source does when this method is invoked will vary by
     * concrete implementation.
     * </p>
     * <p>
     * The type signature of the {@code processedEvents} parameter is intentionally a raw type because events may have
     * had their type signature changed in the course of processing them so their final type signature may not relate to
     * their original type signature which would prevent callers invoking this method.
     * </p>
     *
     * @param processedEvents A collection of events that have been processed.
     */
    @SuppressWarnings("rawtypes")
    void processed(Collection<Event> processedEvents);
}
