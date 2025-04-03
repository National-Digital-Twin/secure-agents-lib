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

import java.util.function.Supplier;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;

/**
 * A Sink that throws errors when sent items, intended purely for unit testing
 *
 * @param <T> Input type
 */
public class ErrorSink<T> implements Sink<T> {
    private final Supplier<SinkException> errorSupplier;

    /**
     * Creates a new error sink with a default error message of {@code Failed}
     */
    public ErrorSink() {
        this(() -> new SinkException("Failed"));
    }

    /**
     * Creates a new error sink
     *
     * @param errorSupplier Error supplier
     */
    public ErrorSink(Supplier<SinkException> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    public void send(T item) {
        throw this.errorSupplier.get();
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
