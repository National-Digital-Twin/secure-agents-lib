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

import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.AbstractTransformingSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.AbstractForwardingSinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;

/**
 * A sink that outputs just the event key
 *
 * @param <TKey>   Event key type
 * @param <TValue> Event value type
 */
public class EventKeySink<TKey, TValue> extends AbstractTransformingSink<Event<TKey, TValue>, TKey> {
    /**
     * Creates a new sink with an optional forwarding destination
     * <p>
     * If no forwarding destination is provided then the {@link NullSink} is used.
     * </p>
     *
     * @param destination Forwarding destination
     */
    public EventKeySink(Sink<TKey> destination) {
        super(destination);
    }

    @Override
    protected TKey transform(Event<TKey, TValue> event) {
        return event.key();
    }

    /**
     * Creates a new event key sink builder
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     * @return Event key sink builder
     */
    public static <TKey, TValue> Builder<TKey, TValue> create() {
        return new Builder<>();
    }

    /**
     * A builder for event key sinks
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     */
    public static class Builder<TKey, TValue>
            extends
            AbstractForwardingSinkBuilder<Event<TKey, TValue>, TKey, EventKeySink<TKey, TValue>, Builder<TKey, TValue>> {

        /**
         * Builds an event key sink
         *
         * @return Event key sink
         */
        @Override
        public EventKeySink<TKey, TValue> build() {
            return new EventKeySink<>(this.getDestination());
        }
    }
}
