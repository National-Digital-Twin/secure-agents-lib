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

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.AttributeNames;
import uk.gov.dbt.ndtp.secure.agent.observability.MetricNames;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.MetricTestUtils;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.TestLoggerUtils;
import uk.gov.dbt.ndtp.secure.agent.projectors.utils.ThroughputTracker;

public class TestThroughputSink {

    public static final List<String> DEFAULT_TEST_VALUES = Arrays.asList("a", "b", "c");

    /**
     * Acceptable delta when comparing expected and actual times.  Due to the vagaries of processing scheduling,
     * especially in CI/CD environments need to allow some leeway.
     */
    public static final int ACCEPTABLE_DELTA = 50;

    private final TestLogger throughputLogger = TestLoggerFactory.getTestLogger(ThroughputSink.class);

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
        throughputLogger.clear();
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*>= 1.*")
    public void throughput_bad_01() {
        new ThroughputSink<String>(null, -1);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*time unit.*null")
    public void throughput_bad_02() {
        new ThroughputSink<String>(null, 100, null, ThroughputTracker.DEFAULT_ACTION,
                                   ThroughputTracker.DEFAULT_ITEMS_NAME, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*maximum precision is milliseconds")
    public void throughput_bad_03() {
        new ThroughputSink<String>(null, 100, TimeUnit.MICROSECONDS, ThroughputTracker.DEFAULT_ACTION,
                                   ThroughputTracker.DEFAULT_ITEMS_NAME, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*maximum precision is milliseconds")
    public void throughput_bad_04() {
        new ThroughputSink<String>(null, 100, TimeUnit.NANOSECONDS, ThroughputTracker.DEFAULT_ACTION,
                                   ThroughputTracker.DEFAULT_ITEMS_NAME, null);
    }

    @Test
    public void throughput_bad_05() {
        // Sending an item to an error sink should initialize the first item time BUT won't update the count
        try (ThroughputSink<String> sink = new ThroughputSink<>(new ErrorSink<>(), 100)) {
            try {
                sink.send("test");
            } catch (SinkException e) {
                Assert.assertEquals(e.getMessage(), "Failed");
            }
            Assert.assertNotEquals(sink.getFirstTime(), -1);
            Assert.assertEquals(sink.processedCount(), 0);
        }
    }

    protected void verifyThroughput(List<String> values, long reportBatchSize) {
        verifyThroughput(values, null, reportBatchSize, TimeUnit.MILLISECONDS, ThroughputTracker.DEFAULT_ACTION,
                         ThroughputTracker.DEFAULT_ITEMS_NAME);
    }

    protected void verifyThroughput(List<String> values, Sink<String> destination, long reportBatchSize,
                                    TimeUnit reportingTimeUnit, String action,
                                    String itemsName) {
        verifyThroughput(values, destination, reportBatchSize, reportingTimeUnit, action, itemsName, null);
    }

    protected void verifyThroughput(List<String> values, Sink<String> destination, long reportBatchSize,
                                    TimeUnit reportingTimeUnit, String action,
                                    String itemsName, String metricsLabel) {
        this.throughputLogger.clear();

        try (ThroughputSink<String> sink = new ThroughputSink<>(destination, reportBatchSize, reportingTimeUnit,
                                                                action, itemsName, metricsLabel)) {
            long start = System.currentTimeMillis();
            values.forEach(sink::send);
            long end = System.currentTimeMillis();

            // Verify counters are as expected
            // For the timings we're subject to the vagaries of processor scheduling, so we expect our captured values
            // to be within a certain delta.  On GitHub Actions the actual delta can be a lot more because you're
            // running within a container on virtualized hardware
            Assert.assertEquals(sink.processedCount(), values.size());
            assertAlmostEqual(sink.getFirstTime(), start, ACCEPTABLE_DELTA);
            assertAlmostEqual(sink.getLastTime(), end, ACCEPTABLE_DELTA);

            // Verify metric was as expected
            if (StringUtils.isNotBlank(metricsLabel)) {
                double reportedMetric = MetricTestUtils.getReportedMetric(MetricNames.ITEMS_PROCESSED,
                                                                          AttributeNames.ITEMS_TYPE, metricsLabel);
                Assert.assertEquals(reportedMetric, values.size());
            }

            // Verify that the throughput was reported the expected number of times
            long expectedThroughputReports = values.size() < reportBatchSize ? 0 : values.size() / reportBatchSize;
            Assert.assertEquals(throughputLogger.getLoggingEvents().size(), expectedThroughputReports);

            // After close() the counters should be reset
            sink.close();
            Assert.assertEquals(sink.processedCount(), 0);
            Assert.assertEquals(sink.getFirstTime(), -1);
            Assert.assertEquals(sink.getLastTime(), -1);

            // Verify that the final throughout was reported when the sink was closed
            Assert.assertEquals(throughputLogger.getLoggingEvents().size(), expectedThroughputReports + 1);
        } catch (Exception e) {
            Assert.fail("Unexpected sink error", e);
        }
    }

    private void assertAlmostEqual(long actual, long expected, long delta) {
        if (actual == expected) {
            Assert.assertEquals(actual, expected);
        } else {
            Assert.assertTrue(Math.abs(actual - expected) <= delta,
                              String.format(
                                      "Expected %,d but got %,d which was not within the permitted delta %,d (actual delta was %,d)",
                                      expected, actual, delta, Math.abs(actual - expected)));
        }
    }

    @Test
    public void throughput_01() {
        try (ThroughputSink<String> sink = new ThroughputSink<>(null, 1)) {
            // Just an explicit close to ensure that throughput isn't printed if the sink never receives any inputs
            sink.close();
            Assert.assertEquals(throughputLogger.getLoggingEvents().size(), 0);
        }
    }

    @Test
    public void throughput_02() {
        verifyThroughput(DEFAULT_TEST_VALUES, 1);
    }

    @Test
    public void throughput_03() {
        verifyThroughput(DEFAULT_TEST_VALUES, null, 1, TimeUnit.SECONDS, ThroughputTracker.DEFAULT_ACTION, "strings");

        // Expect all throughput to be reported in seconds and refer to strings rather than items
        TestLoggerUtils.formattedLogMessages(throughputLogger).forEach(m -> {
            Assert.assertFalse(m.contains("milliseconds"), "Log message unexpectedly contains milliseconds: " + m);
            Assert.assertFalse(m.contains("items"), "Log message unexpectedly contains items: " + m);
            Assert.assertTrue(m.contains("strings/seconds"),
                              "Log Message does not contain strings/seconds as expected: " + m);
        });
    }

    @Test
    public void throughput_04() {
        // Set high batch size, so we'll only report throughput once when the sink is closed
        verifyThroughput(DEFAULT_TEST_VALUES, 100_000);
    }

    @Test
    public void throughput_05() {
        // Use a delay sink to produce a predictable rate
        DelaySink<String> delay = new DelaySink<>(null, 1000);
        verifyThroughput(DEFAULT_TEST_VALUES, delay, 100, TimeUnit.SECONDS, ThroughputTracker.DEFAULT_ACTION,
                         "strings", "throughput_05");

        // Expected rate to be reported as 1.000 strings/seconds
        TestLoggerUtils.formattedLogMessages(throughputLogger).forEach(m -> {
            Assert.assertTrue(m.contains("1.000 strings/seconds"), "Expected rate to be 1.000 strings/seconds: " + m);
        });
    }

    @Test
    public void throughput_06() {
        // Use a delay sink to produce a predictable rate
        DelaySink<String> delay = new DelaySink<>(null, 2000);
        verifyThroughput(DEFAULT_TEST_VALUES, delay, 100, TimeUnit.SECONDS, "Collected",
                         "strings", "throughput_06");

        // Expected action to be customised appropriately
        // Expected rate to be reported as 1.000 strings/seconds
        TestLoggerUtils.formattedLogMessages(throughputLogger).forEach(m -> {
            Assert.assertTrue(m.startsWith("Collected"), "Expected action to be Collected: " + m);
            Assert.assertFalse(m.startsWith(ThroughputTracker.DEFAULT_ACTION),
                               "Expected default action to not be present: " + m);
            Assert.assertTrue(m.contains("0.500 strings/seconds"), "Expected rate to be 0.500 strings/seconds: " + m);
        });
    }

    @Test
    public void throughput_07() {
        verifyThroughput(DEFAULT_TEST_VALUES, null, 1, TimeUnit.MILLISECONDS, "  ", "   ");

        // If action was blank/null the default action should be used
        // If items name was blank/null the default items should be used
        TestLoggerUtils.formattedLogMessages(throughputLogger).forEach(m -> {
            Assert.assertTrue(m.startsWith(ThroughputTracker.DEFAULT_ACTION),
                              "Log message did not start with default action as expected: " + m);
            Assert.assertTrue(m.contains("items"), "Log message does not contains items as expected: " + m);
            Assert.assertTrue(m.contains("items/milliseconds"),
                              "Log message does not contain items/milliseconds as expected: " + m);
        });
    }

    @Test
    public void throughput_08() {
        verifyThroughput(DEFAULT_TEST_VALUES, null, 1, TimeUnit.MILLISECONDS, null, null);

        // If action was blank/null the default action should be used
        // If items name was blank/null the default items should be used
        TestLoggerUtils.formattedLogMessages(throughputLogger).forEach(m -> {
            Assert.assertTrue(m.startsWith(ThroughputTracker.DEFAULT_ACTION),
                              "Log message did not start with default action as expected: " + m);
            Assert.assertTrue(m.contains("items"), "Log message does not contains items as expected: " + m);
            Assert.assertTrue(m.contains("items/milliseconds"),
                              "Log message does not contain items/milliseconds as expected: " + m);
        });
    }

    protected void verifyDiscardingThroughput(List<String> values, Sink<String> destination, long reportBatchSize) {
        this.throughputLogger.clear();

        try (ThroughputSink<String> sink = new DiscardingThroughputSink<>(destination, reportBatchSize)) {
            long start = System.currentTimeMillis();
            values.forEach(sink::send);
            long end = System.currentTimeMillis();

            // Verify counters are as expected
            // Since we're using a Discarding variant of the sink the processed and received counts should differ
            Assert.assertEquals(sink.processedCount(), 0);
            Assert.assertEquals(sink.receivedCount(), values.size());
            if (!values.isEmpty()) {
                Assert.assertNotEquals(sink.receivedCount(), sink.processedCount());
            }

            // Verify that the throughput was never reported since no items were ever forwarded
            Assert.assertEquals(throughputLogger.getLoggingEvents().size(), 0);

            // After close() the counters should be reset
            sink.close();
            Assert.assertEquals(sink.receivedCount(), 0);
            Assert.assertEquals(sink.processedCount(), 0);
            Assert.assertEquals(sink.getFirstTime(), -1);
            Assert.assertEquals(sink.getLastTime(), -1);

            // Verify that no throughput was reported since no items were ever processed
            Assert.assertEquals(throughputLogger.getLoggingEvents().size(), 0);
        } catch (Exception e) {
            Assert.fail("Unexpected sink error", e);
        }
    }

    @Test
    public void discarding_throughput_02() {
        verifyDiscardingThroughput(DEFAULT_TEST_VALUES, null, 1);
    }
}
