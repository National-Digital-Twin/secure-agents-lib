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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.AttributeNames;
import uk.gov.dbt.ndtp.secure.agent.observability.MetricNames;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.MetricTestUtils;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;

public class TestFilterSink extends AbstractSinkHelper {

    @Test
    public void givenAlwaysTruePredicate_whenFilteringItems_thenNothingIsFiltered() {
        // Given
        List<String> values = Arrays.asList("foo", "bar", "faz");
        Predicate<String> filter = x -> true;

        // When and Then
        verifyFilter(values, filter, values);
    }

    @Test
    public void givenAlwaysFalsePredicate_whenFilteringItems_thenEverythingIsFiltered() {
        // Given
        List<String> values = Arrays.asList("foo", "bar", "faz");
        Predicate<String> filter = x -> false;

        // When and Then
        verifyFilter(values, filter, Collections.emptyList());
    }

    @Test
    public void givenAlwaysFalsePredicate_whenFilteringItems_thenEverythingIsFiltered_andMetricsAreCorrect() {
        // Given
        List<String> values = Arrays.asList("foo", "bar", "faz");
        Predicate<String> filter = x -> false;

        // When, Then, And
        verifyFilter(values, filter, "filter_02", Collections.emptyList());
    }

    @Test
    public void givenActualPredicate_whenFilteringItems_thenSomeItemsAreFiltered() {
        // Given
        List<String> values = Arrays.asList("foo", "bar", "faz");
        Predicate<String> filter = x -> StringUtils.startsWith(x, "f");

        // When and Then
        verifyFilter(values, filter, Arrays.asList("foo", "faz"));
    }

    @Test
    public void givenActualPredicate_whenFilteringItems_thenSomeItemsAreFiltered_andMetricsAreCorrect() {
        // Given
        List<String> values = Arrays.asList("foo", "bar", "faz");
        Predicate<String> filter = x -> StringUtils.startsWith(x, "f");

        // When, Then, And
        verifyFilter(values, filter, "filter_03", Arrays.asList("foo", "faz"));
    }

    @Test
    public void givenAlwaysTruePredicate_whenFilteringNoItems_thenNothingIsFiltered() {
        // Given
        Predicate<String> filter = x -> true;

        // When and Then
        verifyFilter(Collections.emptyList(), filter, Collections.emptyList());
    }

    @Test(expectedExceptions = SinkException.class, expectedExceptionsMessageRegExp = "Failed")
    public void givenFailingDestination_whenFilteringItems_thenErrorIsThrownUpwards() {
        // Given
        // Create a sink chain that will error
        try (FilterSink<String> sink = new FilterSink<>(new ErrorSink<>(() -> new SinkException("Failed")),
                                                        x -> true)) {
            // When and Then
            sink.send("test");
        }
    }

    @Test(expectedExceptions = SinkException.class, expectedExceptionsMessageRegExp = ".* IO Failed.*")
    public void givenFailingDestinationWithCausedError_whenFilteringItems_thenCauseIsThrownUpwards() {
        // Given
        // Create a sink chain that will error
        try (FilterSink<String> sink = new FilterSink<>(
                new ErrorSink<>(() -> new SinkException(new IOException("IO Failed"))), x -> true)) {
            // When and Then
            sink.send("test");
        }
    }

    @Test
    public void givenNullPredicate_whenFilteringItems_thenNothingIsFiltered() {
        // Given
        List<String> values = Arrays.asList("foo", "bar", "faz");

        // When and Then
        // Null filter should default to accepting all items i.e. it sets the filter to x -> true
        verifyFilter(values, null, values);
    }

    protected void verifyFilter(List<String> values, Predicate<String> filter, List<String> expected) {
        // When and Then
        verifyFilter(values, filter, null, expected);
    }

    protected void verifyFilter(List<String> values, Predicate<String> filter, String metricsLabel,
                                List<String> expected) {
        // When
        try (CollectorSink<String> collector = new CollectorSink<>()) {
            try (FilterSink<String> sink = new FilterSink<>(collector, filter, metricsLabel)) {
                values.forEach(sink::send);

                // Then
                verifyCollectedValues(collector, expected);

                // And
                // After close() the close should be passed down to the destination sink clearing the collection
                sink.close();
                Assert.assertEquals(collector.get().size(), 0);
            }
        }

        // And
        // If metrics were enabled then the appropriate number of filtered items should have been reported
        if (StringUtils.isNotBlank(metricsLabel)) {
            double metricValue = MetricTestUtils.getReportedMetric(MetricNames.ITEMS_FILTERED, AttributeNames.ITEMS_TYPE, metricsLabel);
            Assert.assertEquals(metricValue, values.size() - expected.size());
        }
    }
}
