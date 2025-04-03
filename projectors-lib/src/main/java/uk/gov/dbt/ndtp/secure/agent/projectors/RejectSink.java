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
package uk.gov.dbt.ndtp.secure.agent.projectors;

import java.util.function.Function;
import java.util.function.Predicate;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.FilterSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.AbstractFilteringSinkBuilder;

/**
 * A sink that filters items, throwing an error for items that fail to match the filter
 * <p>
 * If you prefer to just drop filtered items silently use the {@link FilterSink} instead.
 * </p>
 *
 * @param <T> Item type
 */
public class RejectSink<T> extends FilterSink<T> {

    private final Function<T, String> errorMessageGenerator;

    RejectSink(Sink<T> destination, Predicate<T> filter, Function<T, String> errorMessageGenerator,
               String metricsLabel) {
        super(destination, filter, metricsLabel);
        this.errorMessageGenerator = errorMessageGenerator != null ? errorMessageGenerator : t -> "Rejected";
    }

    /**
     * Creates a new rejecting filter sink builder
     * <p>
     * As opposed to the normal {@link FilterSink} that can be built via the normal {@link #create()} method which
     * simply does not forward items that don't match the configured filter, the {@link RejectSink} built via this
     * method throws errors for items that don't match the filter.
     * </p>
     *
     * @param <TItem> Item type
     * @return Rejecting sink builder
     */
    public static <TItem> Builder<TItem> createRejecting() {
        return new Builder<>();
    }

    @Override
    protected boolean shouldForward(T item) throws SinkException {
        // Any filtered items are explicitly rejected with an exception
        if (!super.shouldForward(item)) {
            throw new SinkException(this.errorMessageGenerator.apply(item));
        }
        return true;
    }

    /**
     * A builder for rejecting sinks
     *
     * @param <TItem> Item type
     */
    public static class Builder<TItem>
            extends AbstractFilteringSinkBuilder<TItem, RejectSink<TItem>, Builder<TItem>> {

        private Function<TItem, String> errorMessageGenerator;

        /**
         * Configures an error message generator that can be used to customise the error message for the exception
         * thrown when an item does not meet the configured {@link #filter(Function)} function.
         *
         * @param errorMessageGenerator Error message generator function
         * @return Builder
         */
        public Builder<TItem> errorMessageGenerator(Function<TItem, String> errorMessageGenerator) {
            this.errorMessageGenerator = errorMessageGenerator;
            return this;
        }

        @Override
        protected RejectSink<TItem> buildInternal(Predicate<TItem> filter, String metricsLabel) {
            return new RejectSink<>(this.getDestination(), filter, this.errorMessageGenerator, metricsLabel);
        }
    }
}
