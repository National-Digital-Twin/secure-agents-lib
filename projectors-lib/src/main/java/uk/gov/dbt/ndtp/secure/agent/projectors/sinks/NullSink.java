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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks;

import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;

/**
 * A sink that simply throws away all items it receives
 * <p>
 * It does however keep count of how many items it throws away, primarily so that we can use it in tests to verify that
 * our pipelines are behaving as expected.
 * </p>
 *
 * @param <T> Input type
 */
public class NullSink<T> implements Sink<T> {
    private long counter = 0;

    NullSink() {

    }

    @Override
    public void send(T item) {
        // We're throwing away all our inputs
        // However maintain a counter of how many items we're received, this is done primarily so code coverage can
        // actually detect we've covered this class with tests as with empty method bodies this can't be detected
        this.counter++;
    }

    @Override
    public void close() {
        // Reset the counter
        this.counter = 0;
    }

    /**
     * Gets how many items this sink has discarded
     *
     * @return Items discarded
     */
    public long count() {
        return this.counter;
    }

    /**
     * Creates a new null (discarding) sink
     *
     * @param <TItem> Item type
     * @return Null sink
     */
    public static <TItem> NullSink<TItem> of() {
        return NullSink.<TItem>create().build();
    }

    /**
     * Creates new null (discarding) sink builder
     *
     * @param <TItem> Item type
     * @return Null sink builder
     */
    public static <TItem> Builder<TItem> create() {
        return new Builder<>();
    }

    /**
     * A builder for null (discarding) sinks
     *
     * @param <TItem> Item type
     */
    public static class Builder<TItem> implements SinkBuilder<TItem, NullSink<TItem>> {

        /**
         * Builds a new null (discarding) sink
         *
         * @return Null (discarding) sink
         */
        @Override
        public NullSink<TItem> build() {
            return new NullSink<>();
        }
    }
}
