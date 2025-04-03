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

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.AttributeNames;
import uk.gov.dbt.ndtp.secure.agent.observability.MetricNames;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.MetricTestUtils;

public class TestThroughputTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestThroughputTracker.class);

    private final TestLogger testLogger = TestLoggerFactory.getTestLogger(TestThroughputTracker.class);

    @BeforeClass
    public void setup() {
        MetricTestUtils.enableMetricsCapture();
    }

    @AfterClass
    public void teardown() {
        MetricTestUtils.disableMetricsCapture();
    }

    @BeforeMethod
    public void clearTestLogger() {
        testLogger.clear();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void throughput_tracker_bad_01() {
        new ThroughputTracker(null, -1, null, null, null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*>= 1")
    public void throughput_tracker_bad_02() {
        new ThroughputTracker(LOGGER, -1, null, null, null, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void throughput_tracker_bad_03() {
        new ThroughputTracker(LOGGER, 100, null, null, null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*>= 1")
    public void throughput_tracker_bad_04() {
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, 100, TimeUnit.SECONDS, null, null, null);
        tracker.itemsProcessed(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*>= 1")
    public void throughput_tracker_bad_05() {
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, 100, TimeUnit.SECONDS, null, null, null);
        tracker.itemsProcessed(-1);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ThroughputTracker.TRACKING_MISMATCH_ERROR)
    public void throughput_tracker_bad_06() {
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, 100, TimeUnit.SECONDS, null, null, null);
        tracker.itemProcessed();
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ThroughputTracker.TRACKING_MISMATCH_ERROR)
    public void throughput_tracker_bad_07() {
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, 100, TimeUnit.SECONDS, null, null, null);
        for (int i = 0; i < 100; i++) {
            tracker.itemReceived();
        }
        tracker.itemsProcessed(101);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*already started.*")
    public void throughput_tracker_bad_08() {
        ThroughputTracker tracker =
                new ThroughputTracker(LOGGER, 100, TimeUnit.SECONDS, ThroughputTracker.DEFAULT_ACTION,
                                      ThroughputTracker.DEFAULT_ITEMS_NAME, null);
        // Calling this multiple times should error
        tracker.start();
        tracker.start();
    }

    @DataProvider(name = "tracker")
    public Object[][] getTrackerTestParameters() {
        return new Object[][] {
                { 10_000, 100 },
                { 10_000, 100_000 },
                { 1_000_000, 10_000 },
                { 1_000_000, 25_000 },
                { 10_000, 333 }
        };
    }

    @Test(dataProvider = "tracker")
    public void throughput_tracker_01(int count, int reportBatchSize) {
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, reportBatchSize, TimeUnit.SECONDS,
                                                          ThroughputTracker.DEFAULT_ACTION,
                                                          ThroughputTracker.DEFAULT_ITEMS_NAME, null);

        Assert.assertEquals(tracker.receivedCount(), 0);
        Assert.assertEquals(tracker.processedCount(), 0);
        Assert.assertEquals(tracker.getOverallRate(), 0.0);

        for (int i = 0; i < count; i++) {
            tracker.itemReceived();
            tracker.itemProcessed();
        }

        Assert.assertEquals(tracker.receivedCount(), count);
        Assert.assertEquals(tracker.processedCount(), count);
        if (count > reportBatchSize) {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), count / reportBatchSize);
        } else {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), 0);
        }
    }

    @Test(dataProvider = "tracker")
    public void throughput_tracker_01b(int count, int reportBatchSize) {
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, reportBatchSize, TimeUnit.MILLISECONDS,
                                                          ThroughputTracker.DEFAULT_ACTION,
                                                          ThroughputTracker.DEFAULT_ITEMS_NAME, null);

        Assert.assertEquals(tracker.receivedCount(), 0);
        Assert.assertEquals(tracker.processedCount(), 0);
        Assert.assertEquals(tracker.getOverallRate(), 0.0);

        for (int i = 0; i < count; i++) {
            tracker.itemReceived();
            tracker.itemProcessed();
        }

        Assert.assertEquals(tracker.receivedCount(), count);
        Assert.assertEquals(tracker.processedCount(), count);
        if (count > reportBatchSize) {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), count / reportBatchSize);
        } else {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), 0);
        }
    }

    @Test(dataProvider = "tracker", timeOut = 1000L)
    public void throughput_tracker_02(int count, int reportBatchSize) {
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, reportBatchSize, TimeUnit.SECONDS,
                                                          ThroughputTracker.DEFAULT_ACTION,
                                                          ThroughputTracker.DEFAULT_ITEMS_NAME, null);

        Assert.assertEquals(tracker.receivedCount(), 0);
        Assert.assertEquals(tracker.processedCount(), 0);
        Assert.assertEquals(tracker.getOverallRate(), 0.0);

        for (int i = 1; i <= count; i++) {
            tracker.itemReceived();
            if (tracker.receivedCount() % reportBatchSize == 0) {
                tracker.itemsProcessed((int) (tracker.receivedCount() - tracker.processedCount()));
            }
        }
        if (tracker.receivedCount() > tracker.processedCount()) {
            tracker.itemsProcessed((int) (tracker.receivedCount() - tracker.processedCount()));
        }

        Assert.assertEquals(tracker.receivedCount(), count);
        Assert.assertEquals(tracker.processedCount(), count);
        if (count > reportBatchSize) {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), count / reportBatchSize);
        } else {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), 0);
        }
    }

    @Test(dataProvider = "tracker")
    public void throughput_tracker_with_metrics_01(int count, int reportBatchSize) {
        String metricsLabel = String.format("with_metrics_01_%d_%d", count, reportBatchSize);
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, reportBatchSize, TimeUnit.SECONDS,
                                                          ThroughputTracker.DEFAULT_ACTION,
                                                          ThroughputTracker.DEFAULT_ITEMS_NAME,
                                                          metricsLabel);

        Assert.assertEquals(tracker.receivedCount(), 0);
        Assert.assertEquals(tracker.processedCount(), 0);
        Assert.assertEquals(tracker.getOverallRate(), 0.0);

        for (int i = 0; i < count; i++) {
            tracker.itemReceived();
            tracker.itemProcessed();
        }

        Assert.assertEquals(tracker.receivedCount(), count);
        Assert.assertEquals(tracker.processedCount(), count);
        if (count > reportBatchSize) {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), count / reportBatchSize);
        } else {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), 0);
        }

        validateMetrics(tracker, metricsLabel);
    }

    @Test(dataProvider = "tracker")
    public void throughput_tracker_with_metrics_02(int count, int reportBatchSize) {
        String metricsLabel = String.format("with_metrics_02_%d_%d", count, reportBatchSize);
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, reportBatchSize, TimeUnit.SECONDS,
                                                          ThroughputTracker.DEFAULT_ACTION,
                                                          ThroughputTracker.DEFAULT_ITEMS_NAME,
                                                          metricsLabel);

        Assert.assertEquals(tracker.receivedCount(), 0);
        Assert.assertEquals(tracker.processedCount(), 0);
        Assert.assertEquals(tracker.getOverallRate(), 0.0);

        for (int i = 0; i < count; i++) {
            tracker.itemReceived();
            if (tracker.receivedCount() % reportBatchSize == 0) {
                tracker.itemsProcessed((int) (tracker.receivedCount() - tracker.processedCount()));
            }
        }
        if (tracker.receivedCount() > tracker.processedCount()) {
            tracker.itemsProcessed((int) (tracker.receivedCount() - tracker.processedCount()));
        }

        Assert.assertEquals(tracker.receivedCount(), count);
        Assert.assertEquals(tracker.processedCount(), count);
        if (count > reportBatchSize) {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), count / reportBatchSize);
        } else {
            Assert.assertEquals(testLogger.getLoggingEvents().size(), 0);
        }

        validateMetrics(tracker, metricsLabel);
    }

    @Test(dataProvider = "tracker")
    public void throughput_tracker_with_metrics_03(int count, int reportBatchSize) {
        String metricsLabel = String.format("with_metrics_03_%d_%d", count, reportBatchSize);
        ThroughputTracker tracker = new ThroughputTracker(LOGGER, reportBatchSize, TimeUnit.SECONDS,
                                                          ThroughputTracker.DEFAULT_ACTION,
                                                          ThroughputTracker.DEFAULT_ITEMS_NAME,
                                                          metricsLabel);

        Assert.assertEquals(tracker.receivedCount(), 0);
        Assert.assertEquals(tracker.processedCount(), 0);
        Assert.assertEquals(tracker.getOverallRate(), 0.0);

        for (int i = 0; i < count; i++) {
            tracker.itemReceived();
        }

        Assert.assertEquals(tracker.receivedCount(), count);
        Assert.assertEquals(tracker.processedCount(), 0);
        Assert.assertEquals(testLogger.getLoggingEvents().size(), 0);

        validateMetrics(tracker, metricsLabel);
    }

    private void validateMetrics(ThroughputTracker tracker, String metricsLabel) {
        double reportedMetric =
                MetricTestUtils.getReportedMetric(MetricNames.ITEMS_RECEIVED,
                                                  AttributeNames.ITEMS_TYPE,
                                                  metricsLabel);
        Assert.assertEquals(reportedMetric, (double) tracker.receivedCount());
        try {
            reportedMetric =
                    MetricTestUtils.getReportedMetric(MetricNames.ITEMS_PROCESSED,
                                                      AttributeNames.ITEMS_TYPE,
                                                      metricsLabel);
            Assert.assertEquals(reportedMetric, (double) tracker.processedCount());
        } catch (IllegalStateException e) {
            Assert.assertEquals(0L, tracker.processedCount());
        }

        // Force a throughput report to make sure that the rate metric gets reported
        tracker.reportThroughput();
        reportedMetric =
                MetricTestUtils.getReportedMetric(MetricNames.ITEMS_PROCESSING_RATE, AttributeNames.ITEMS_TYPE,
                                                  metricsLabel);
        Assert.assertEquals(reportedMetric, tracker.getOverallRate(), 500.0);
    }

    @Test
    public void builder_01() {
        ThroughputTracker.create().logger(TestThroughputTracker.class).inMilliseconds().metricsLabel("test").build();
    }

    @Test
    public void builder_02() {
        ThroughputTracker.create().logger("test-logger").inSeconds().metricsLabel("test").build();
    }

    @Test
    public void builder_03() {
        ThroughputTracker.create().logger(LoggerFactory.getLogger("ROOT")).inMinutes().metricsLabel("test").build();
    }

    @Test
    public void builder_04() {
        ThroughputTracker.create().logger("test").reportTimeUnit(TimeUnit.HOURS).metricsLabel("test").build();
    }
}
