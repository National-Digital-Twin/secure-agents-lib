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

import static uk.gov.dbt.ndtp.secure.agent.projectors.sinks.TestJacksonJsonSink.verifyCollectedValues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.AttributeNames;
import uk.gov.dbt.ndtp.secure.agent.observability.MetricNames;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.MetricTestUtils;
import uk.gov.dbt.ndtp.secure.agent.projectors.TestLoggerUtils;

public class TestDuplicationSuppressionSinks {

    public static final Map<String, Object> A = Map.of("id", 1234);
    public static final Map<String, Object> B = Map.of("id", 5678);
    public static final Map<String, Object> D = Map.of("id", 1234, "data", "test");
    private final TestLogger suppressDuplicatesLogger = TestLoggerFactory.getTestLogger(SuppressDuplicatesSink.class),
            suppressUnmodifiedLogger = TestLoggerFactory.getTestLogger(SuppressUnmodifiedSink.class);
    private final Function<String, Boolean> invalidateDuplicateCacheTrue = item -> true,
            invalidateDuplicateCacheFalse = item -> false;
    private final Function<Map<String, Object>, Boolean> invalidateUnmodifiedCacheTrue = item -> true,
            invalidateUnmodifiedCacheFalse = item -> false;

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
        suppressDuplicatesLogger.clear();
        suppressUnmodifiedLogger.clear();
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*>= 1")
    public void suppress_duplicates_bad_01() {
        new SuppressDuplicatesSink<String>(null, 0, null, null, null, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*duration greater than zero")
    public void suppress_duplicates_bad_02() {
        new SuppressDuplicatesSink<String>(NullSink.of(), 10, null, null, null, Duration.ofSeconds(-1));
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*duration greater than zero")
    public void suppress_duplicates_bad_03() {
        new SuppressDuplicatesSink<String>(NullSink.of(), 10, null, null, null, Duration.ZERO);
    }

    protected void verifySuppressDuplicatesSink(List<String> values, int cacheSize, List<String> expectedValues) {
        verifySuppressDuplicatesSink(values, cacheSize, null, expectedValues);
    }

    protected void verifySuppressDuplicatesSink(List<String> values, int cacheSize, String metricsLabel,
                                                List<String> expectedValues) {
        verifySuppressDuplicatesSink(values, cacheSize, metricsLabel, null, expectedValues);
    }

    protected void verifySuppressDuplicatesSink(List<String> values, int cacheSize, String metricsLabel,
                                                Function<String, Boolean> invalidateCache,
                                                List<String> expectedValues) {
        verifySuppressDuplicatesSink(values, cacheSize, metricsLabel, invalidateCache, null, null, expectedValues);
    }

    protected void verifySuppressDuplicatesSink(List<String> values, int cacheSize, String metricsLabel,
                                                Function<String, Boolean> invalidateCache,
                                                Supplier<Boolean> invalidateWholeCache, Duration expireCacheAfter,
                                                List<String> expectedValues) {
        CollectorSink<String> collector = new CollectorSink<>();
        try (SuppressDuplicatesSink<String> sink = new SuppressDuplicatesSink<>(collector, cacheSize, metricsLabel,
                                                                                invalidateCache, invalidateWholeCache,
                                                                                expireCacheAfter)) {
            values.forEach(sink::send);

            verifyCollectedValues(collector, expectedValues);
            Assert.assertEquals(sink.getSuppressed(), values.size() - expectedValues.size());

            // Metric should exist if enabled and cache invalidation wasn't being triggered
            if (StringUtils.isNotBlank(metricsLabel) && (invalidateCache == null || invalidateCache.apply(
                    "a") == false) && (invalidateWholeCache == null || invalidateWholeCache.get() == false)) {
                double reportedMetric = MetricTestUtils.getReportedMetric(MetricNames.DUPLICATES_SUPPRESSED,
                                                                    AttributeNames.ITEMS_TYPE, metricsLabel);
                Assert.assertEquals(reportedMetric, values.size() - expectedValues.size());
            }

            // After close() the sink should be emptied
            sink.close();
            Assert.assertEquals(collector.get().size(), 0);
        }
    }

    @Test
    public void suppress_duplicates_01() {
        verifySuppressDuplicatesSink(Arrays.asList("a", "a", "b"), 100, "duplicates_01", Arrays.asList("a", "b"));
    }

    @Test
    public void suppress_duplicates_02() {
        verifySuppressDuplicatesSink(Arrays.asList("a", "b", "a", "b"), 100, "duplicates_02", Arrays.asList("a", "b"));
    }

    @Test
    public void suppress_duplicates_03() {
        // With a smaller cache size duplicates will be produced
        verifySuppressDuplicatesSink(Arrays.asList("a", "b", "a", "b"), 1, Arrays.asList("a", "b", "a", "b"));
    }

    @Test
    public void suppress_duplicates_04() {
        // With a smaller cache some duplicates will be produced
        verifySuppressDuplicatesSink(Arrays.asList("a", "b", "c", "a", "d", "b", "c"), 3, "duplicates_04",
                                     Arrays.asList("a", "b", "c", "d", "c"));
    }

    @Test
    public void suppress_duplicates_05() {
        verifySuppressDuplicatesSink(Arrays.asList("a", "b", "a", "b"), 100, "duplicates_05",
                                     this.invalidateDuplicateCacheFalse, Arrays.asList("a", "b"));
    }

    @Test
    public void suppress_duplicates_06() {
        // Verify cache entry invalidation happens
        verifySuppressDuplicatesSink(Arrays.asList("a", "b", "a", "b"), 100, "duplicates_06",
                                     this.invalidateDuplicateCacheTrue, Arrays.asList("a", "b", "a", "b"));
    }

    @Test
    public void suppress_duplicates_07() {
        // Verify whole cache invalidation happens
        verifySuppressDuplicatesSink(Arrays.asList("a", "b", "a", "b"), 100, "duplicates_07", null, () -> true, null,
                                     Arrays.asList("a", "b", "a", "b"));
    }

    @Test
    public void suppress_duplicates_08() {
        // Verify that cache expiry happens by introducing a delay into the destination sink
        CollectorSink<String> collector = CollectorSink.of();
        DelaySink<String> delay = new DelaySink<>(collector, 10);
        List<String> values = Arrays.asList("a", "b", "a", "b");
        try (SuppressDuplicatesSink<String> sink = SuppressDuplicatesSink.<String>create()
                                                                         .cacheSize(10)
                                                                         .expireCacheAfter(Duration.ofMillis(1))
                                                                         .destination(delay)
                                                                         .build()) {
            values.forEach(sink::send);
            verifyCollectedValues(collector, values);
        }
        Assert.assertTrue(TestLoggerUtils.formattedLogMessages(suppressDuplicatesLogger)
                                   .anyMatch(m -> StringUtils.contains(m, "Invalidated duplicate suppression cache")),
                          "Expected sink to log that it was invalidated");
    }

    @Test
    public void suppress_duplicates_09() {
        // Verify that cache expiry doesn't happen if the delay is too short
        CollectorSink<String> collector = CollectorSink.of();
        DelaySink<String> delay = new DelaySink<>(collector, 1);
        List<String> values = Arrays.asList("a", "b", "a", "b");
        try (SuppressDuplicatesSink<String> sink = SuppressDuplicatesSink.<String>create()
                                                                         .cacheSize(10)
                                                                         .expireCacheAfter(Duration.ofMillis(25))
                                                                         .destination(delay)
                                                                         .build()) {
            values.forEach(sink::send);
            verifyCollectedValues(collector, Arrays.asList("a", "b"));
        }
        Assert.assertTrue(TestLoggerUtils.formattedLogMessages(suppressDuplicatesLogger)
                                   .noneMatch(m -> StringUtils.contains(m, "Invalidated duplicate suppression cache")),
                          "Expected sink to NOT log that it was invalidated");
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*>= 1")
    public void suppress_unmodified_bad_01() {
        new SuppressUnmodifiedSink<String, String, String>(null, 0, null, null,
                                                           null, null, null, null, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void suppress_unmodified_bad_02() {
        new SuppressUnmodifiedSink<String, String, String>(null, 100, null, null,
                                                           null, null, null, null, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void suppress_unmodified_bad_03() {
        new SuppressUnmodifiedSink<String, String, String>(null, 100, null, k -> k,
                                                           null, null, null, null, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void suppress_unmodified_bad_04() {
        new SuppressUnmodifiedSink<String, String, String>(null, 100, null, k -> k, v -> v,
                                                           null, null, null, null);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*duration greater than zero")
    public void suppress_unmodified_bad_05() {
        new SuppressUnmodifiedSink<String, String, String>(null, 100, null, k -> k, v -> v,
                                                           Comparator.naturalOrder(), null, null,
                                                           Duration.ofSeconds(-1));
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*duration greater than zero")
    public void suppress_unmodified_bad_06() {
        new SuppressUnmodifiedSink<String, String, String>(null, 100, null, k -> k, v -> v,
                                                           Comparator.naturalOrder(), null, null, Duration.ZERO);
    }

    protected <T, TKey, TValue> void verifySuppressUnmodifiedSink(List<T> values, int cacheSize,
                                                                  Function<T, TKey> keyFunction,
                                                                  Function<T, TValue> valueFunction,
                                                                  Comparator<TValue> valueComparator,
                                                                  List<T> expectedValues) {
        verifySuppressUnmodifiedSink(values, cacheSize, null, keyFunction, valueFunction, valueComparator,
                                     expectedValues);
    }

    protected <T, TKey, TValue> void verifySuppressUnmodifiedSink(List<T> values, int cacheSize, String metricsLabel,
                                                                  Function<T, TKey> keyFunction,
                                                                  Function<T, TValue> valueFunction,
                                                                  Comparator<TValue> valueComparator,
                                                                  List<T> expectedValues) {
        verifySuppressUnmodifiedSink(values, cacheSize, null, keyFunction, valueFunction, valueComparator,
                                     null, expectedValues);
    }

    protected <T, TKey, TValue> void verifySuppressUnmodifiedSink(List<T> values, int cacheSize, String metricsLabel,
                                                                  Function<T, TKey> keyFunction,
                                                                  Function<T, TValue> valueFunction,
                                                                  Comparator<TValue> valueComparator,
                                                                  Function<T, Boolean> invalidateCache,
                                                                  List<T> expectedValues) {
        verifySuppressUnmodifiedSink(values, cacheSize, metricsLabel, keyFunction, valueFunction, valueComparator,
                                     invalidateCache, null, null, expectedValues);
    }

    protected <T, TKey, TValue> void verifySuppressUnmodifiedSink(List<T> values, int cacheSize, String metricsLabel,
                                                                  Function<T, TKey> keyFunction,
                                                                  Function<T, TValue> valueFunction,
                                                                  Comparator<TValue> valueComparator,
                                                                  Function<T, Boolean> invalidateCache,
                                                                  Supplier<Boolean> invalidateWholeCache,
                                                                  Duration expireCacheAfter,
                                                                  List<T> expectedValues) {
        CollectorSink<T> collector = new CollectorSink<>();
        try (SuppressUnmodifiedSink<T, TKey, TValue> sink = new SuppressUnmodifiedSink<>(collector, cacheSize,
                                                                                         metricsLabel,
                                                                                         keyFunction, valueFunction,
                                                                                         valueComparator,
                                                                                         invalidateCache,
                                                                                         invalidateWholeCache,
                                                                                         expireCacheAfter)) {
            values.forEach(sink::send);

            verifyCollectedValues(collector, expectedValues);

            Assert.assertEquals(sink.getSuppressed(), values.size() - expectedValues.size());
            if (StringUtils.isNotBlank(metricsLabel)) {
                try {
                    double reportedMetric =
                            MetricTestUtils.getReportedMetric(MetricNames.UNMODIFIED_SUPPRESSED, AttributeNames.ITEMS_TYPE,
                                                              metricsLabel);
                    Assert.assertEquals(reportedMetric, values.size() - expectedValues.size());
                } catch (IllegalStateException e) {
                    Assert.assertEquals(values.size() - expectedValues.size(), 0);
                }
            }

            // After close() the sink should be emptied
            sink.close();
            Assert.assertEquals(collector.get().size(), 0);
        }
    }

    protected Function<Map<String, Object>, String> createIntegerIdFunction(String key) {
        return m -> Integer.toString((Integer) m.get(key));
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    protected Function<Map<String, Object>, String> itemToJson = m -> {
        try {
            return mapper.writeValueAsString(m);
        } catch (Throwable e) {
            return m.toString();
        }
    };

    @Test
    public void suppress_unmodified_01() {
        List<Map<String, Object>> values = new ArrayList<>();
        Map<String, Object> a = A;
        Map<String, Object> b = B;
        Map<String, Object> c = A;
        Map<String, Object> d = D;
        CollectionUtils.addAll(values, a, b, c, d, d, b, c, a);
        List<Map<String, Object>> expected = new ArrayList<>();
        CollectionUtils.addAll(expected, a, b, d, c);

        verifySuppressUnmodifiedSink(values, 100, "unmodified_01", createIntegerIdFunction("id"), itemToJson,
                                     Comparator.naturalOrder(), expected);

        // Should log suppressed count at close()
        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 1);
        TestLoggerUtils.formattedLogMessages(suppressUnmodifiedLogger).forEach(m -> {
            Assert.assertTrue(m.startsWith("Suppressed 4"), "Expected message to indicate 4 suppressed duplicates");
        });
    }

    @Test
    public void suppress_unmodified_02() {
        List<Map<String, Object>> values = new ArrayList<>();
        Map<String, Object> a = A;
        Map<String, Object> b = B;
        Map<String, Object> c = A;
        Map<String, Object> d = D;
        Map<String, Object> e = Map.of("id", 2468);
        CollectionUtils.addAll(values, a, b, c, d, e, d, a, b, c, a);
        List<Map<String, Object>> expected = new ArrayList<>();
        // Small Cache Size will result in fewer duplicates being suppressed
        CollectionUtils.addAll(expected, a, b, d, e, a, b);

        verifySuppressUnmodifiedSink(values, 2, "unmodified_02", createIntegerIdFunction("id"), itemToJson,
                                     Comparator.naturalOrder(), expected);

        // Should log suppressed count at close()
        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 1);
        TestLoggerUtils.formattedLogMessages(suppressUnmodifiedLogger).forEach(m -> {
            Assert.assertTrue(m.startsWith("Suppressed 4"), "Expected message to indicate 4 suppressed duplicates");
        });
    }

    @Test
    public void suppress_unmodified_03() {
        verifySuppressUnmodifiedSink(Collections.emptyList(), 100, createIntegerIdFunction("id"), itemToJson,
                                     Comparator.naturalOrder(), Collections.emptyList());

        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 0);
    }

    @Test
    public void suppress_unmodified_04() {
        List<Map<String, Object>> values = new ArrayList<>();
        Map<String, Object> a = Map.of("id", 1);
        Map<String, Object> b = Map.of("id", 2);
        Map<String, Object> c = Map.of("id", 3);
        Map<String, Object> d = Map.of("id", 4);
        CollectionUtils.addAll(values, a, b, c, d);
        List<Map<String, Object>> expected = new ArrayList<>();
        CollectionUtils.addAll(expected, a, b, c, d);

        // If all items are unique no duplicate suppression should occur
        verifySuppressUnmodifiedSink(values, 100, "unmodified_04", createIntegerIdFunction("id"), itemToJson,
                                     Comparator.naturalOrder(), expected);

        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 0);
    }

    @Test
    public void suppress_unmodified_05() {
        List<Map<String, Object>> values = new ArrayList<>();
        Map<String, Object> a = A;
        Map<String, Object> b = B;
        Map<String, Object> c = A;
        Map<String, Object> d = D;
        CollectionUtils.addAll(values, a, b, c, d, d, b, c, a);
        List<Map<String, Object>> expected = new ArrayList<>();
        CollectionUtils.addAll(expected, a, b, d, c);

        verifySuppressUnmodifiedSink(values, 100, "unmodified_05", createIntegerIdFunction("id"), itemToJson,
                                     Comparator.naturalOrder(), invalidateUnmodifiedCacheFalse,
                                     expected);

        // Should log suppressed count at close()
        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 1);
        TestLoggerUtils.formattedLogMessages(suppressUnmodifiedLogger).forEach(m -> {
            Assert.assertTrue(m.startsWith("Suppressed 4"), "Expected message to indicate 4 suppressed duplicates");
        });
    }

    @Test
    public void suppress_unmodified_06() {
        List<Map<String, Object>> values = new ArrayList<>();
        CollectionUtils.addAll(values, A, B, A, D, D, B, A, A);
        List<Map<String, Object>> expected = new ArrayList<>();
        CollectionUtils.addAll(expected, A, B, A, D, D, B, A, A);

        verifySuppressUnmodifiedSink(values, 100, "unmodified_06", createIntegerIdFunction("id"), itemToJson,
                                     Comparator.naturalOrder(), invalidateUnmodifiedCacheTrue,
                                     expected);

        // Should log suppressed count at close()
        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 0);
    }

    @Test
    public void suppress_unmodified_07() {
        List<Map<String, Object>> values = new ArrayList<>();
        CollectionUtils.addAll(values, A, B, A, D, D, B, A, A);
        List<Map<String, Object>> expected = new ArrayList<>();
        CollectionUtils.addAll(expected, A, B, A, D, D, B, A, A);

        // Verify that we can invalidate the whole cache
        verifySuppressUnmodifiedSink(values, 100, "unmodified_07", createIntegerIdFunction("id"), itemToJson,
                                     Comparator.naturalOrder(), null, () -> true, null,
                                     expected);

        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 8);
        Assert.assertTrue(TestLoggerUtils.formattedLogMessages(suppressUnmodifiedLogger)
                                   .allMatch(m -> StringUtils.contains(m, "Invalidated unmodified suppression cache")));
    }

    @Test
    public void suppress_unmodified_08() {
        List<Map<String, Object>> values = new ArrayList<>();
        CollectionUtils.addAll(values, A, B, A, D, D, B, A, A);
        List<Map<String, Object>> expected = new ArrayList<>();
        CollectionUtils.addAll(expected, A, B, A, D, D, B, A, A);

        CollectorSink<Map<String, Object>> collector = CollectorSink.of();
        DelaySink<Map<String, Object>> delay = new DelaySink<>(collector, 10);
        //@formatter:off
        try (SuppressUnmodifiedSink<Map<String, Object>, String, String> sink
                     = SuppressUnmodifiedSink.<Map<String, Object>, String, String>create()
                                             .keyFunction(this.createIntegerIdFunction("id"))
                                             .valueFunction(itemToJson)
                                             .cacheSize(10)
                                             .expireCacheAfter(Duration.ofMillis(1))
                                             .comparator(Comparator.naturalOrder())
                                             .destination(delay)
                                             .build()) {
            //@formatter:on

            values.forEach(sink::send);
            verifyCollectedValues(collector, expected);
        }

        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 7);
        Assert.assertTrue(TestLoggerUtils.formattedLogMessages(suppressUnmodifiedLogger)
                                   .allMatch(m -> StringUtils.contains(m, "Invalidated unmodified suppression cache")));
    }

    @Test
    public void suppress_unmodified_09() {
        List<Map<String, Object>> values = new ArrayList<>();
        CollectionUtils.addAll(values, A, B, A, D, D, B, A, A);
        List<Map<String, Object>> expected = new ArrayList<>();
        CollectionUtils.addAll(expected, A, B, D, A);

        CollectorSink<Map<String, Object>> collector = CollectorSink.of();
        DelaySink<Map<String, Object>> delay = new DelaySink<>(collector, 1);
        //@formatter:off
        try (SuppressUnmodifiedSink<Map<String, Object>, String, String> sink
                     = SuppressUnmodifiedSink.<Map<String, Object>, String, String>create()
                                             .keyFunction(this.createIntegerIdFunction("id"))
                                             .valueFunction(itemToJson)
                                             .cacheSize(10)
                                             .expireCacheAfter(Duration.ofMillis(25))
                                             .comparator(Comparator.naturalOrder())
                                             .destination(delay)
                                             .build()) {
            //@formatter:on

            values.forEach(sink::send);
            verifyCollectedValues(collector, expected);
        }


        Assert.assertEquals(suppressUnmodifiedLogger.getLoggingEvents().size(), 1);
        TestLoggerUtils.formattedLogMessages(suppressUnmodifiedLogger).forEach(m -> {
            Assert.assertTrue(m.startsWith("Suppressed 4"), "Expected message to indicate 4 suppressed duplicates");
        });
    }
}
