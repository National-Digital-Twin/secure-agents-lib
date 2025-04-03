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

import java.util.ArrayList;
import java.util.List;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;

/**
 * A sink that simply collects items into a list, primarily intended for testing
 *
 * @param <T> Input type
 */
public class CollectorSink<T> implements Sink<T> {

    private final List<T> collection = new ArrayList<>();

    CollectorSink() {

    }

    @Override
    public void send(T item) throws SinkException {
        collection.add(item);
    }

    /**
     * Gets the items collected by this sink
     *
     * @return Items
     */
    public List<T> get() {
        return this.collection;
    }

    @Override
    public void close() {
        // Throw out the collected items when we are closed
        this.collection.clear();
    }

    /**
     * Creates a new collector sink instance
     *
     * @param <TItem> Item type
     * @return Collector sink
     */
    public static <TItem> CollectorSink<TItem> of() {
        return CollectorSink.<TItem>create().build();
    }

    /**
     * Creates a new collecting sink builder
     *
     * @param <TItem> Item type
     * @return Collecting sink builder
     */
    public static <TItem> Builder<TItem> create() {
        return new Builder<>();
    }

    /**
     * A builder for collecting sinks
     *
     * @param <TItem> Item type
     */
    public static class Builder<TItem> implements SinkBuilder<TItem, CollectorSink<TItem>> {

        /**
         * Builds a collecting sink
         *
         * @return Collecting sink
         */
        @Override
        public CollectorSink<TItem> build() {
            return new CollectorSink<>();
        }
    }
}
