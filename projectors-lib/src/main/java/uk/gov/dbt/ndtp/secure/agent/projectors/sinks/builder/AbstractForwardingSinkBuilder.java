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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder;

import java.util.function.Function;
import uk.gov.dbt.ndtp.secure.agent.projectors.RejectSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.CleanupSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.CollectorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.FilterSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.JacksonJsonSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.Sinks;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.SuppressDuplicatesSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.SuppressUnmodifiedSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.ThroughputSink;

/**
 * An abstract sink builder for a forwarding, and potentially transforming sink.
 * <p>
 * Includes various convenience methods to forward the sink output onwards to common destinations.
 * </p>
 *
 * @param <TInput>  Input item type
 * @param <TOutput> Output item type
 * @param <TSink>   Sink type that will be built
 */
public abstract class AbstractForwardingSinkBuilder<TInput, TOutput, TSink extends Sink<TInput>, TBuilder extends AbstractForwardingSinkBuilder<TInput, TOutput, TSink, TBuilder>>
        implements SinkBuilder<TInput, TSink> {

    private Sink<TOutput> destination;

    /**
     * Returns the builder instance
     *
     * @return Builder
     */
    @SuppressWarnings("unchecked")
    protected TBuilder self() {
        return (TBuilder) this;
    }

    /**
     * Sets the destination sink for this sink
     *
     * @param builder       Sink builder for the destination
     * @param <TOutputSink> Output sink type
     * @return Builder
     */
    public <TOutputSink extends Sink<TOutput>> TBuilder destination(SinkBuilder<TOutput, TOutputSink> builder) {
        return this.destination(builder.build());
    }

    /**
     * Sets the destination sink for this sink
     *
     * @param destination Destination Sink builder
     * @return Builder
     */
    public TBuilder destination(Sink<TOutput> destination) {
        if (this.destination != null) {
            throw new IllegalStateException("Destination has already been set");
        }
        this.destination = destination;
        return self();
    }

    /**
     * Sets the destination for this sink to be a collecting sink
     *
     * @return Builder
     */
    public TBuilder collect() {
        return this.destination(CollectorSink.of());
    }

    /**
     * Sets the destination for this sink to be a null (discarding) sink
     *
     * @return Builder
     */
    public TBuilder discard() {
        return this.destination(NullSink.of());
    }

    /**
     * Sets the destination for this sink to be a filtering sink
     *
     * @param f Builder function that can be used to build the filter sink
     * @return Builder
     */
    public TBuilder filter(Function<FilterSink.Builder<TOutput>, SinkBuilder<TOutput, FilterSink<TOutput>>> f) {
        return this.destination(f.apply(Sinks.filter()).build());
    }

    /**
     * Sets the destination for this sink to be a rejecting sink
     *
     * @param f Builder function that can be used to build the rejecting sink
     * @return Builder
     */
    public TBuilder reject(Function<RejectSink.Builder<TOutput>, SinkBuilder<TOutput, RejectSink<TOutput>>> f) {
        return this.destination(f.apply(Sinks.reject()).build());
    }

    /**
     * Sets the destination for this sink to be a cleanup sink
     *
     * @param f Builder function that can be used to build the cleanup sink
     * @return Builder
     */
    public TBuilder cleanup(Function<CleanupSink.Builder<TOutput>, SinkBuilder<TOutput, CleanupSink<TOutput>>> f) {
        return this.destination(f.apply(Sinks.cleanup()).build());
    }

    /**
     * Sets the destination for this sink to be a JSON sink
     *
     * @param f Builder function that can be used to build the JSON sink
     * @return Builder
     */
    public TBuilder toJson(Function<JacksonJsonSink.Builder<TOutput>, JacksonJsonSink.Builder<TOutput>> f) {
        return this.destination(f.apply(Sinks.toJson()).build());
    }

    /**
     * Sets the destination for this sink to be a duplicate suppressing sink
     *
     * @param f Builder function that can be used to build the duplicate suppressing sink
     * @return Builder
     */
    public TBuilder suppressDuplicates(
            Function<SuppressDuplicatesSink.Builder<TOutput>, SinkBuilder<TOutput, SuppressDuplicatesSink<TOutput>>> f) {
        return this.destination(f.apply(Sinks.suppressDuplicates()).build());
    }

    /**
     * Sets the destination for this sink to be an unmodified suppressing sink
     *
     * @param f        Builder function that can be used to build the unmodified suppressing sink
     * @param <TKey>   Key type
     * @param <TValue> Value type
     * @return Builder
     */
    public <TKey, TValue> TBuilder suppressUnmodified(
            Function<SuppressUnmodifiedSink.Builder<TOutput, TKey, TValue>, SinkBuilder<TOutput, SuppressUnmodifiedSink<TOutput, TKey, TValue>>> f) {
        return this.destination(f.apply(Sinks.suppressUnmodified()).build());
    }

    /**
     * Sets the destination for this sink to be a throughput tracking sink
     *
     * @param f Builder function that can be used to build the throughput tracking sink
     * @return Builder
     */
    public TBuilder throughput(
            Function<ThroughputSink.Builder<TOutput>, SinkBuilder<TOutput, ThroughputSink<TOutput>>> f) {
        return this.destination(f.apply(Sinks.throughput()).build());
    }

    /**
     * Gets the destination, possibly {@code null} if none configured.
     * <p>
     * If there is no destination configured then this acts internally as if {@link #discard()} had been called i.e. the
     * outputs will go to a null (discarding) sink.
     * </p>
     *
     * @return Destination
     */
    protected final Sink<TOutput> getDestination() {
        if (this.destination != null) {
            return this.destination;
        } else {
            return null;
        }
    }

}
