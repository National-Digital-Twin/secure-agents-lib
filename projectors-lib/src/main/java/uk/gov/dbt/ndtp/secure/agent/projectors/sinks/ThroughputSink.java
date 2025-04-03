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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.AbstractForwardingSinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.projectors.utils.ThroughputTracker;
import uk.gov.dbt.ndtp.secure.agent.projectors.utils.ThroughputTrackerBuilder;

/**
 * A sink that reports throughput metrics
 *
 * @param <T> Input type
 */
public class ThroughputSink<T> extends AbstractTransformingSink<T, T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThroughputSink.class);

    private final ThroughputTracker tracker;

    /**
     * Creates a new throughput sink with the given destination sink
     *
     * @param destination     Destination sink
     * @param reportBatchSize Configures how often the throughput is reported, this is expressed in batch size i.e.
     *                        after this many items are received by this sink the throughput will be reported
     */
    ThroughputSink(Sink<T> destination, long reportBatchSize) {
        this(destination, reportBatchSize, TimeUnit.MILLISECONDS, ThroughputTracker.DEFAULT_ACTION,
             ThroughputTracker.DEFAULT_ITEMS_NAME, null);
    }

    /**
     * Creates a new throughput sink with the given destination sink
     *
     * @param destination     Destination sink
     * @param reportBatchSize Configures how often the throughput is reported, this is expressed in batch size i.e.
     *                        after this many items are received by this sink the throughput will be reported.
     * @param reportTimeUnit  Time Unit in which throughput rate should be reported
     * @param action          What action will be referred to in the logging
     * @param itemsName       How items will be referred to in the logging
     */
    ThroughputSink(Sink<T> destination, long reportBatchSize, TimeUnit reportTimeUnit, String action,
                   String itemsName, String metricsLabel) {
        this(destination, ThroughputTracker.create()
                                           .logger(LOGGER)
                                           .reportBatchSize(reportBatchSize)
                                           .reportTimeUnit(reportTimeUnit)
                                           .action(action)
                                           .itemsName(itemsName)
                                           .metricsLabel(metricsLabel)
                                           .build());
    }

    /**
     * Creates a new throughput sink with the given destination sink
     *
     * @param destination Destination sink
     * @param tracker     Throughput tracker
     */
    ThroughputSink(Sink<T> destination, ThroughputTracker tracker) {
        super(destination);
        Objects.requireNonNull(tracker, "tracker cannot be null");
        this.tracker = tracker;
    }

    @Override
    public void send(T item) throws SinkException {
        this.tracker.itemReceived();

        if (!this.shouldForward(item)) {
            return;
        }
        this.forward(this.transform(item));

        this.tracker.itemProcessed();
    }

    @Override
    protected T transform(T t) {
        return t;
    }

    @Override
    public void close() {
        super.close();

        // Report throughput when closed
        this.tracker.reportThroughput();

        // Reset counters on close
        this.tracker.reset();
    }

    /**
     * Gets the total count of items received by this sink
     * <p>
     * This may differ from {@link #processedCount()} as derived implementations of this sink may override
     * {@link #shouldForward(Object)} such that not all received items are forwarded for further processing.
     * </p>
     *
     * @return Count of received items
     */
    public long receivedCount() {
        return this.tracker.receivedCount();
    }


    /**
     * Gets the total count of items processed by this sink i.e. items that were received and forwarded onto the
     * destination sink for further processing
     *
     * @return Count of processed items
     */
    public long processedCount() {
        return this.tracker.processedCount();
    }

    /**
     * Gets when this sink first saw an item as milliseconds since the epoch
     *
     * @return First item time
     */
    public long getFirstTime() {
        return this.tracker.getFirstTime();
    }

    /**
     * Gets when this sink last saw an item as milliseconds since the epoch
     *
     * @return Last item time
     */
    public long getLastTime() {
        return this.tracker.getLastTime();
    }

    /**
     * Creates a new throughput tracking sink builder
     *
     * @param <TItem> Item type
     * @return Throughput tracking sink builder
     */
    public static <TItem> Builder<TItem> create() {
        return new Builder<>();
    }

    /**
     * A builder for throughput tracking sinks
     *
     * @param <TItem> Item type
     */
    public static class Builder<TItem>
            extends AbstractForwardingSinkBuilder<TItem, TItem, ThroughputSink<TItem>, Builder<TItem>> {

        private ThroughputTrackerBuilder trackerBuilder = ThroughputTracker.create().logger(ThroughputTracker.class);

        /**
         * Applies a function to build the throughput tracker for this sink
         *
         * @param f Builder function
         * @return Builder
         */
        public Builder<TItem> tracker(Function<ThroughputTrackerBuilder, ThroughputTrackerBuilder> f) {
            this.trackerBuilder = f.apply(this.trackerBuilder);
            return this;
        }

        /**
         * Builds a throughput tracking sink
         *
         * @return Throughput tracking sink
         */
        @Override
        public ThroughputSink<TItem> build() {
            return new ThroughputSink<>(this.getDestination(), this.trackerBuilder.build());
        }
    }
}
