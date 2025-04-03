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
package uk.gov.dbt.ndtp.secure.agent.projectors.utils;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.logging.FmtLog;
import org.slf4j.Logger;
import uk.gov.dbt.ndtp.secure.agent.observability.AttributeNames;
import uk.gov.dbt.ndtp.secure.agent.observability.MetricNames;
import uk.gov.dbt.ndtp.secure.agent.observability.IANodeMetrics;
import uk.gov.dbt.ndtp.secure.agent.projectors.Library;

/**
 * Utility class for tracking the throughput of various components
 */
public class ThroughputTracker {

    /**
     * Creates a new {@link ThroughputTracker} builder to use to build a new tracker
     *
     * @return Builder
     */
    public static ThroughputTrackerBuilder create() {
        return new ThroughputTrackerBuilder();
    }

    /**
     * Default action used for logging that reports throughput
     */
    public static final String DEFAULT_ACTION = "Processed";

    /**
     * Default items name used for logging that reports the throughput
     */
    public static final String DEFAULT_ITEMS_NAME = "items";
    /**
     * Error message issued when the tracker is used incorrectly
     */
    public static final String TRACKING_MISMATCH_ERROR = "Must call itemReceived prior to itemProcessed";

    private final Logger logger;
    private long processed = 0, received = 0;
    private long first = -1, last = -1;
    private final long reportBatchSize;
    private final TimeUnit reportTimeUnit;
    private final String action, itemsName;

    private final boolean metricsEnabled;
    private final LongCounter processedMetric, receivedMetric;
    private final Attributes metricAttributes;

    /**
     * Creates a new throughput tracker
     *
     * @param logger          Logger to which throughput should be reported
     * @param reportBatchSize Reporting batch size i.e. after how many items should throughput be reported
     * @param reportTimeUnit  Reporting time unit i.e. controls in what time unit elapsed time and rate are displayed
     * @param action          Reporting action i.e. how the throughput is referred to in the logging
     * @param itemsName       Reported items name i.e. how the items are referred to in the logging
     * @param metricsLabel    Label to use in reporting metrics, leave blank to disable metrics
     */
    @SuppressWarnings("resource")
    ThroughputTracker(Logger logger, long reportBatchSize, TimeUnit reportTimeUnit, String action, String itemsName,
                      String metricsLabel) {
        Objects.requireNonNull(logger, "Logger cannot be null");
        if (reportBatchSize < 0) {
            throw new IllegalArgumentException("Reporting interval must be >= 1");
        }
        Objects.requireNonNull(reportTimeUnit, "Reporting time unit cannot be null");

        switch (reportTimeUnit) {
            case MICROSECONDS, NANOSECONDS ->
                throw new IllegalArgumentException("Reporting time unit maximum precision is milliseconds");
            default -> {
                // No action needed for other time units
            }
        }

        this.logger = logger;
        this.reportBatchSize = reportBatchSize;
        this.reportTimeUnit = reportTimeUnit;
        this.action = StringUtils.isNotBlank(action) ? action : DEFAULT_ACTION;
        this.itemsName = StringUtils.isNotBlank(itemsName) ? itemsName : DEFAULT_ITEMS_NAME;

        // Get the instances of our metrics that we're going to update
        this.metricsEnabled = StringUtils.isNotBlank(metricsLabel);
        ObservableDoubleGauge rateMetric;
        if (this.metricsEnabled) {
            this.metricAttributes = Attributes.of(AttributeKey.stringKey(AttributeNames.ITEMS_TYPE), metricsLabel,
                                                  AttributeKey.stringKey(AttributeNames.INSTANCE_ID),
                                                  UUID.randomUUID().toString());
            Meter meter = IANodeMetrics.getMeter(Library.NAME);
            //@formatter:off
            this.receivedMetric = meter.counterBuilder(MetricNames.ITEMS_RECEIVED)
                                       .setDescription(MetricNames.ITEMS_RECEIVED_DESCRIPTION)
                                       .build();
            this.processedMetric = meter.counterBuilder(MetricNames.ITEMS_PROCESSED)
                                        .setDescription(MetricNames.ITEMS_PROCESSED_DESCRIPTION)
                                        .build();
            rateMetric = meter.gaugeBuilder(MetricNames.ITEMS_PROCESSING_RATE)
                                   .setDescription(MetricNames.ITEMS_PROCESSING_RATE_DESCRIPTION)
                                   .buildWithCallback(measurement -> measurement.record(getOverallRate(),
                                                                                        this.metricAttributes));
            //@formatter:on
        } else {
            this.metricAttributes = null;
            this.receivedMetric = null;
            this.processedMetric = null;
            rateMetric = null;
        }
    }

    /**
     * Should be called when you want to start tracking prior to an item being received, if the action of receiving the
     * item may itself take a long time.
     * <p>
     * In most cases it is sufficient to just start calling {@link #itemReceived()} as items are received since that
     * will start the tracking timer at the time of the first item received.  However in some cases receiving the first
     * item may itself take a long time, e.g. waiting for an event source to start producing events, whose time you want
     * to capture in your timings.  In those cases you call this method <strong>once</strong> prior to calling whatever
     * method receives your first item.
     * </p>
     * <p>
     * Calling this on an already started tracker is considered an error and will produce an
     * {@link IllegalStateException}.
     * </p>
     *
     * @throws IllegalStateException Thrown if this is called on an already started tracker.
     */
    public void start() {
        if (this.first == -1) {
            this.first = System.currentTimeMillis();
        } else {
            throw new IllegalStateException("start() called on an already started tracker");
        }
    }

    /**
     * Should be called when an item is received, but before it is processed, this allows the tracker to account for
     * total processing time of the entire operation being tracked
     */
    public void itemReceived() {
        if (this.first == -1) {
            this.start();
        }
        this.received++;
        if (this.metricsEnabled) {
            this.receivedMetric.add(1, this.metricAttributes);
        }
    }

    /**
     * Should be called when an item has been processed
     */
    public void itemProcessed() {
        if (this.processed >= this.received) {
            throw new IllegalStateException(TRACKING_MISMATCH_ERROR);
        }
        this.processed++;
        if (this.metricsEnabled) {
            this.processedMetric.add(1, this.metricAttributes);
        }
        this.last = System.currentTimeMillis();

        if (this.processed % this.reportBatchSize == 0) reportThroughput();
    }

    /**
     * Should be called when multiple items are processed e.g. via batch processing
     *
     * @param items Number of items that have been processed
     */
    public void itemsProcessed(int items) {
        if (items < 1) {
            throw new IllegalArgumentException("Items must be >= 1");
        }
        if (this.processed + items > this.received) {
            throw new IllegalStateException(TRACKING_MISMATCH_ERROR);
        }

        this.processed += items;
        if (this.metricsEnabled) {
            this.processedMetric.add(items, this.metricAttributes);
        }
        this.last = System.currentTimeMillis();

        if (this.processed % this.reportBatchSize == 0) reportThroughput();
    }

    /**
     * Reports the current throughput statistics
     */
    public void reportThroughput() {
        // If not started, or we've been reset and cleared our counters, we can't report throughput
        if (this.first == -1 || this.processed == 0) return;

        long elapsed = this.last - this.first;
        calculateAndLogRate(this.logger, this.action, elapsed, this.reportTimeUnit, this.processed, this.itemsName);
    }


    private static double calculateRate(double count, double elapsed) {
        if (elapsed > 0) {
            return count / elapsed;
        }
        return count;
    }

    /**
     * Calculates and logs throughput rate
     *
     * @param action         The action to use for the log message
     * @param logger         Logger to log to
     * @param elapsed        Elapsed time in milliseconds
     * @param reportTimeUnit Time Unit in which to actually report elapsed time, if not {@link TimeUnit#MILLISECONDS}
     *                       then the elapsed time is appropriately converted into the desired time unit
     * @param count          Count of items seen
     * @param itemsName      Items name i.e. how you want to refer to items within the log message
     * @return Calculated rate
     */
    public static double calculateAndLogRate(Logger logger, String action, long elapsed, TimeUnit reportTimeUnit,
                                             long count, String itemsName) {
        if (reportTimeUnit != TimeUnit.MILLISECONDS) {
            elapsed = reportTimeUnit.convert(elapsed, TimeUnit.MILLISECONDS);
        }
        double rate = calculateRate((double) count, (double) elapsed);
        FmtLog.info(logger, "%s %,d %s in %,d %s at %.3f %s/%s", action, count, itemsName, elapsed,
                    reportTimeUnit.toString().toLowerCase(Locale.ROOT), rate, itemsName,
                    reportTimeUnit.toString().toLowerCase(Locale.ROOT));

        return rate;
    }

    /**
     * Gets the total count of items recorded as received by this tracker
     *
     * @return Received items count
     */
    public long receivedCount() {
        return this.received;
    }

    /**
     * Gets the total count of items recorded as processed by this tracker
     *
     * @return Processed items count
     */
    public long processedCount() {
        return this.processed;
    }

    /**
     * Gets when this sink first saw an item as milliseconds since the epoch
     *
     * @return First item time
     */
    public long getFirstTime() {
        return this.first;
    }

    /**
     * Gets when this sink last saw an item as milliseconds since the epoch
     *
     * @return Last item time
     */
    public long getLastTime() {
        return this.last;
    }

    /**
     * Gets the overall processing rate observed by this tracker.
     * <p>
     * This is calculated in terms of the configured reporting time unit, so if the reporting time unit is
     * {@link TimeUnit#SECONDS} then the reported rate is items per seconds.
     * </p>
     *
     * @return Overall processing rate
     */
    public double getOverallRate() {
        long elapsed = this.last - this.first;
        if (this.reportTimeUnit != TimeUnit.MILLISECONDS) {
            elapsed = reportTimeUnit.convert(elapsed, TimeUnit.MILLISECONDS);
        }
        return calculateRate(this.processed, elapsed);
    }

    /**
     * Resets the tracker
     */
    public void reset() {
        this.first = -1;
        this.processed = 0;
        this.received = 0;
        this.last = -1;
    }
}
