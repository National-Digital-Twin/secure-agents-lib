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
package uk.gov.dbt.ndtp.secure.agent.projectors.driver;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.function.Supplier;
import uk.gov.dbt.ndtp.secure.agent.projectors.Projector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;

/**
 * A builder for {@link ProjectorDriver} instances
 *
 * @param <TKey>    Key type
 * @param <TValue>  Value type
 * @param <TOutput> Output type
 */
public class ProjectorDriverBuilder<TKey, TValue, TOutput> {

    private EventSource<TKey, TValue> source;
    private Duration pollTimeout = Duration.ofSeconds(30);
    private Projector<Event<TKey, TValue>, TOutput> projector;
    private Supplier<Sink<TOutput>> sinkSupplier;
    private long limit = -1, maxStalls = 0, reportBatchSize = 10_000L;

    /**
     * Specifies the event source for the projector driver
     *
     * @param source Event source
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> source(EventSource<TKey, TValue> source) {
        this.source = source;
        return this;
    }

    /**
     * Specifies the poll timeout used when polling for events from the event source
     *
     * @param duration Duration
     * @param unit     Temporal Unit
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> pollTimeout(long duration, TemporalUnit unit) {
        return pollTimeout(Duration.of(duration, unit));
    }

    /**
     * Specifies the poll timeout used when polling for events from the event source
     *
     * @param pollTimeout Duration
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> pollTimeout(Duration pollTimeout) {
        this.pollTimeout = pollTimeout;
        return this;
    }

    /**
     * Specifies the projector used to project the incoming events from the configured event source to the configured
     * destination sink
     *
     * @param projector Projector
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> projector(Projector<Event<TKey, TValue>, TOutput> projector) {
        this.projector = projector;
        return this;
    }

    /**
     * Specifies the destination sink to which the output of the projection is sent
     *
     * @param sink Destination sink
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> destination(Sink<TOutput> sink) {
        return destination(() -> sink);
    }

    /**
     * Specifies the destination sink to which the output of the projection is sent
     *
     * @param sinkBuilder   Destination sink builder
     * @param <TOutputSink> Output sink type
     * @return Builder
     */
    public <TOutputSink extends Sink<TOutput>> ProjectorDriverBuilder<TKey, TValue, TOutput> destinationBuilder(
            SinkBuilder<TOutput, TOutputSink> sinkBuilder) {
        return destination(sinkBuilder::build);
    }

    /**
     * Specifies the destination sink to which the output of the projection is sent
     *
     * @param sinkSupplier Destination sink supplier
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> destination(Supplier<Sink<TOutput>> sinkSupplier) {
        this.sinkSupplier = sinkSupplier;
        return this;
    }

    /**
     * Specifies that the driver will not impose any limit on the number of projected events i.e. it will run
     * indefinitely unless {@link #maxStalls(long)} has been configured
     *
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> unlimited() {
        return limit(-1);
    }

    /**
     * Specifies the maximum number of events that the driver will project before exiting
     *
     * @param limit Limit
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> limit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Specifies that the driver will not impose any limit on the number of stalls (see {@link #maxStalls(long)} for
     * definition of a stall) i.e. the driver will run indefinitely unless a {@link #limit(long)} has been configured
     *
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> unlimitedStalls() {
        return maxStalls(0);
    }

    /**
     * Specifies the maximum number of permitted consecutive stalls, a stall is when the driver polls the event source
     * and receives no new events within the configured poll timeout.
     * <p>
     * As a practical example if you have a 30 second poll timeout and 5 maximum stalls then if the event source
     * produces no new events for a 2.5 minute period (30 seconds times 5) the driver would abort itself.
     * </p>
     *
     * @param maxStalls Maximum permitted stalls
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> maxStalls(long maxStalls) {
        this.maxStalls = maxStalls;
        return this;
    }

    /**
     * Specifies the reporting batch size i.e. how often the driver will report progress.  This batch size is expressed
     * in terms of the number of events read from the event source, so a batch size of {@code 5000} would mean progress
     * is reported after every 5000 events.
     *
     * @param reportBatchSize Reporting batch size
     * @return Builder
     */
    public ProjectorDriverBuilder<TKey, TValue, TOutput> reportBatchSize(long reportBatchSize) {
        this.reportBatchSize = reportBatchSize;
        return this;
    }

    /**
     * Builds a new projector driver
     *
     * @return Projector Driver
     */
    public ProjectorDriver<TKey, TValue, TOutput> build() {
        return new ProjectorDriver<>(source, pollTimeout, projector, sinkSupplier, limit, maxStalls,
                                     reportBatchSize);
    }
}
