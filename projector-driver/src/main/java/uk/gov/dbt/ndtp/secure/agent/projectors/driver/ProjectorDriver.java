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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.jena.atlas.logging.FmtLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.observability.AttributeNames;
import uk.gov.dbt.ndtp.secure.agent.observability.IANodeMetrics;
import uk.gov.dbt.ndtp.secure.agent.projectors.Library;
import uk.gov.dbt.ndtp.secure.agent.projectors.Projector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.utils.ThroughputTracker;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;

/**
 * A projector driver connects an event source up to a projector and an output sink.
 * <p>
 * This basically wraps up a bunch of useful logic around polling for events from an {@link EventSource} and pushing the
 * events through a {@link Projector}.  It includes automated management of the polling loop alongside throughput
 * monitoring and reporting.
 * </p>
 *
 * @param <TKey>    Event key type
 * @param <TValue>  Event value type
 * @param <TOutput> Output type
 */
public class ProjectorDriver<TKey, TValue, TOutput> implements Runnable {

    /**
     * Creates a new builder for a {@link ProjectorDriver} instance
     *
     * @param <TKey>    Key type
     * @param <TValue>  Value type
     * @param <TOutput> Output type
     * @return Builder
     */
    public static <TKey, TValue, TOutput> ProjectorDriverBuilder<TKey, TValue, TOutput> create() {
        return new ProjectorDriverBuilder<>();
    }


    /**
     * Default items name used in throughput tracking output of the driver
     */
    public static final String DEFAULT_ITEMS_NAME = "Events";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectorDriver.class);

    private static final String ITEM_TYPE_EVENTS = "events";

    private final EventSource<TKey, TValue> source;
    private final Duration pollTimeout;
    private final Projector<Event<TKey, TValue>, TOutput> projector;
    private final Supplier<Sink<TOutput>> sinkSupplier;
    private final long limit, maxStalls;
    private long consecutiveStallsCount;
    private final ThroughputTracker tracker;
    private volatile boolean shouldRun = true;
    private final Attributes metricAttributes;
    private final LongCounter stalls;

    /**
     * Creates a new driver
     *
     * @param source             Event source from which to read events
     * @param pollTimeout        Maximum time to wait for an {@link EventSource#poll(Duration)} operation to succeed
     * @param projector          Projector to project the events with
     * @param outputSinkSupplier A supplier that can provide a sink to which projected events will be output
     * @param limit              The maximum number of events to project before stopping, negative values are
     *                           interpreted as no limit
     * @param maxStalls          The maximum number of consecutive stalls, i.e. occasions where the event source fails
     *                           to return any new events, after which projection should be aborted.
     * @param reportBatchSize    Reporting batch size i.e. how often the driver should report throughput statistics
     */
    @SuppressWarnings("resource")
    ProjectorDriver(EventSource<TKey, TValue> source, Duration pollTimeout,
                    Projector<Event<TKey, TValue>, TOutput> projector, Supplier<Sink<TOutput>> outputSinkSupplier,
                    long limit, long maxStalls, long reportBatchSize) {
        Objects.requireNonNull(source, "Event Source cannot be null");
        Objects.requireNonNull(projector, "Projector cannot be null");
        Objects.requireNonNull(outputSinkSupplier, "Sink Supplier cannot be null");
        Objects.requireNonNull(pollTimeout, "Poll Timeout cannot be null");

        this.source = source;
        this.pollTimeout = pollTimeout;
        this.projector = projector;
        this.sinkSupplier = outputSinkSupplier;
        this.limit = limit;
        this.maxStalls = maxStalls;

        this.metricAttributes = Attributes.of(AttributeKey.stringKey(AttributeNames.ITEMS_TYPE), ITEM_TYPE_EVENTS,
                                              AttributeKey.stringKey(AttributeNames.INSTANCE_ID),
                                              UUID.randomUUID().toString());
        Meter meter = IANodeMetrics.getMeter(Library.NAME);
        this.stalls = meter.counterBuilder(DriverMetricNames.STALLS_TOTAL)
                           .setDescription(DriverMetricNames.STALLS_TOTAL_DESCRIPTION)
                           .build();
        ObservableLongGauge consecutiveStalls = meter.gaugeBuilder(DriverMetricNames.STALLS_CONSECUTIVE)
                                                     .setDescription(DriverMetricNames.STALLS_CONSECUTIVE_DESCRIPTION)
                                                     .ofLongs()
                                                     .buildWithCallback(
                                                             measure -> measure.record(getConsecutiveStalls(),
                                                                                       this.metricAttributes));

        this.tracker = ThroughputTracker.create()
                                        .logger(LOGGER)
                                        .reportBatchSize(reportBatchSize)
                                        .inSeconds()
                                        .action("Projected")
                                        .itemsName(DEFAULT_ITEMS_NAME)
                                        .metricsLabel(ITEM_TYPE_EVENTS)
                                        .build();
    }

    private long getConsecutiveStalls() {
        return this.consecutiveStallsCount;
    }

   @Override
    public void run() {
        try {
            Thread.currentThread().setName("ProjectorDriver");
        } catch (Throwable e) {
            // Ignore if unable to set thread name
        }

        try (Sink<TOutput> sink = this.sinkSupplier.get()) {
            this.tracker.start();

            while (this.shouldRun) {
                checkSourceClosed();

                if (checkEventLimitReached() || checkSourceExhausted()) {
                    break;
                }

                Event<TKey, TValue> event = this.source.poll(this.pollTimeout);
                handleEvent(event, sink);
            }
        } finally {
            this.tracker.reportThroughput();
            this.shouldRun = false;
            closeSource();
        }
    }

    private void checkSourceClosed() {
        if (this.source.isClosed()) {
            LOGGER.warn("Event Source has been closed outside of our control, aborting projection");
            throw new IllegalStateException("Event Source closed externally");
        }
    }

    private boolean checkEventLimitReached() {
        if (this.limit >= 0 && this.tracker.processedCount() >= this.limit) {
            FmtLog.info(LOGGER, "Reached configured event limit of %,d events", this.limit);
            this.shouldRun = false;
            return true;
        }
        return false;
    }

    private boolean checkSourceExhausted() {
        if (this.source.isExhausted()) {
            LOGGER.info("Event Source indicates all events have been exhausted, ending projection");
            this.shouldRun = false;
            return true;
        }
        return false;
    }

    private void handleEvent(Event<TKey, TValue> event, Sink<TOutput> sink) {
        boolean expectToBlock = !this.source.availableImmediately();

        if (event == null) {
            handleEventTimeout(expectToBlock);
        } else {
            this.consecutiveStallsCount = 0;
            this.tracker.itemReceived();
            this.projector.project(event, sink);
            this.tracker.itemProcessed();
        }
    }

    private void handleEventTimeout(boolean expectToBlock) {
        LOGGER.debug("Timed out waiting for Event Source to return more events, waited {}", this.pollTimeout);
        this.stalls.add(1, this.metricAttributes);
        this.consecutiveStallsCount++;

        if (!expectToBlock) {
            LOGGER.warn("Event Source incorrectly indicated that events were available but failed to return them, aborting projection");
            this.shouldRun = false;
            return;
        }

        if (this.maxStalls > 0 && this.consecutiveStallsCount >= this.maxStalls) {
            LOGGER.info("Event Source is stalled, no new events have been received on the last {} polls, aborting projection", this.maxStalls);
            this.shouldRun = false;
            return;
        }

        checkRemainingEvents();
    }

    private void checkRemainingEvents() {
        Long remaining = this.source.remaining();
        if (remaining != null && this.consecutiveStallsCount == 1) {
            if (remaining == 0L) {
                LOGGER.info("Event Source reports it currently has 0 events remaining i.e. all events have been processed");
            } else {
                FmtLog.info(LOGGER, "Event Source reports it only has %,d events remaining", remaining);
            }

            double overallRate = this.tracker.getOverallRate();
            if (overallRate > remaining) {
                FmtLog.warn(LOGGER, "Overall processing rate (%.3f events/seconds) is greater than remaining events (%,d).  Application performance is being reduced by a slower upstream producer writing to %s", overallRate, remaining, this.source.toString());
            }
        }
    }

    private void closeSource() {
        if (!this.source.isClosed()) this.source.close();
    }

    /**
     * Cancels the projection
     * <p>
     * Since a {@link ProjectorDriver} is a {@link Runnable} task that will be running on a thread this method is used
     * from another thread to signal that projection should be cancelled.  Note that cancellation may not take effect
     * immediately depending on what the driver is doing at the time.
     * </p>
     */
    public void cancel() {
        this.shouldRun = false;
    }
}
