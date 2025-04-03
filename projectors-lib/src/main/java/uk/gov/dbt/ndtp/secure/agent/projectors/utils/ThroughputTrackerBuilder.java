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

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for {@link ThroughputTracker}'s
 */
public class ThroughputTrackerBuilder {

    private Logger logger;
    private long reportBatchSize = 10_000;
    private TimeUnit reportTimeUnit = TimeUnit.MILLISECONDS;
    private String itemsName, action, metricsLabel;

    /**
     * Creates a new builder
     */
    ThroughputTrackerBuilder() {

    }

    /**
     * Sets the logger to which the tracker will report
     *
     * @param logger Logger
     * @return Builder
     */
    public ThroughputTrackerBuilder logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Sets the logger to which the tracker will report
     *
     * @param name Logger name
     * @return Builder
     */
    public ThroughputTrackerBuilder logger(String name) {
        return this.logger(LoggerFactory.getLogger(name));
    }

    /**
     * Sets the logger to which the tracker will report
     *
     * @param cls Class to use as Logger name
     * @return Builder
     */
    public ThroughputTrackerBuilder logger(Class<?> cls) {
        return this.logger(LoggerFactory.getLogger(cls));
    }

    /**
     * Sets the report batch size i.e. how frequently the tracker will report
     *
     * @param reportBatchSize Report batch size, expressed in number of items
     * @return Builder
     */
    public ThroughputTrackerBuilder reportBatchSize(long reportBatchSize) {
        this.reportBatchSize = reportBatchSize;
        return this;
    }

    /**
     * Sets the report time unit i.e. how the tracker reports timings and calculates processing rate
     *
     * @param unit Time unit
     * @return Builder
     */
    public ThroughputTrackerBuilder reportTimeUnit(TimeUnit unit) {
        this.reportTimeUnit = unit;
        return this;
    }

    /**
     * Sets the report time unit to milliseconds, see {@link #reportTimeUnit(TimeUnit)}
     *
     * @return Builder
     */
    public ThroughputTrackerBuilder inMilliseconds() {
        return this.reportTimeUnit(TimeUnit.MILLISECONDS);
    }

    /**
     * Sets the report time unit to seconds, see {@link #reportTimeUnit(TimeUnit)}
     *
     * @return Builder
     */
    public ThroughputTrackerBuilder inSeconds() {
        return this.reportTimeUnit(TimeUnit.SECONDS);
    }

    /**
     * Sets the report time unit to minutes, see {@link #reportTimeUnit(TimeUnit)}
     *
     * @return Builder
     */
    public ThroughputTrackerBuilder inMinutes() {
        return this.reportTimeUnit(TimeUnit.MINUTES);
    }

    /**
     * Sets the action that this tracker is tracking, this is used as the first part of the log messages.
     * <p>
     * If not set then {@link ThroughputTracker#DEFAULT_ACTION} is used.
     * </p>
     *
     * @param action Action
     * @return Builder
     */
    public ThroughputTrackerBuilder action(String action) {
        this.action = action;
        return this;
    }

    /**
     * Sets the items that this tracker is tracking, this is used as part of the log messages.
     * <p>
     * If not set then {@link ThroughputTracker#DEFAULT_ITEMS_NAME} is used.
     * </p>
     *
     * @param name Items name
     * @return Builder
     */
    public ThroughputTrackerBuilder itemsName(String name) {
        this.itemsName = name;
        return this;
    }

    /**
     * Sets the metrics label used to report metrics.
     * <p>
     * If not set then metrics will not be exposed.
     * </p>
     *
     * @param label Metrics label
     * @return Builder
     */
    public ThroughputTrackerBuilder metricsLabel(String label) {
        this.metricsLabel = label;
        return this;
    }

    /**
     * Builds a throughput tracker
     *
     * @return Throughput Tracker
     * @throws NullPointerException     If a required parameter has not been configured by this builder
     * @throws IllegalArgumentException If an illegal parameter value has been configured by this builder
     */
    public ThroughputTracker build() {
        return new ThroughputTracker(this.logger, this.reportBatchSize, this.reportTimeUnit, this.action,
                                     this.itemsName, this.metricsLabel);
    }


}
