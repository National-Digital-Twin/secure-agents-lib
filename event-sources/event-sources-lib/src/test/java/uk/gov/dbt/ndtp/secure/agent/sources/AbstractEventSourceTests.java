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
package uk.gov.dbt.ndtp.secure.agent.sources;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * An abstract suite of tests for event sources that verify that an arbitrary event source is able to fulfill the
 * behavioural contract defined for the {@link EventSource} interface.
 *
 * @param <TKey> Key type
 * @param <TValue> Value type
 */
public abstract class AbstractEventSourceTests<TKey, TValue> {

    /**
     * Creates an empty event source for testing
     *
     * @return Empty event source
     */
    protected abstract EventSource<TKey, TValue> createEmptySource();

    /**
     * Creates an event source consisting of the given events
     *
     * @param events Events
     * @return Event source
     */
    protected abstract EventSource<TKey, TValue> createSource(Collection<Event<TKey, TValue>> events);

    /**
     * Creates sample data
     *
     * @param size Size of the sample data i.e. this method should return a collection of this size
     * @return Sample data
     */
    protected abstract Collection<Event<TKey, TValue>> createSampleData(int size);

    /**
     * Creates a set of sample events that are just strings
     *
     * @param size Size of the sample data
     * @return Sample data
     */
    public static List<String> createSampleStrings(int size) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            data.add("event-" + i);
        }
        return data;
    }

    /**
     * Indicates whether the event source being tested guarantees that the test events are immediately available
     *
     * @return True if immediately available, false otherwise
     */
    public abstract boolean guaranteesImmediateAvailability();

    /**
     * Indicates whether the event source being tested is unbounded i.e. will it always return {@code false} for
     * {@link EventSource#isExhausted()} until it is closed?
     *
     * @return True if unbounded, false otherwise
     */
    public abstract boolean isUnbounded();

    @Test
    public void givenEmptySource_whenQueryingAvailability_thenNothingAvailableImmediately_andReportsExhausted() {
        // Given
        EventSource<TKey, TValue> source = createEmptySource();
        try {
            // When and Then
            Assert.assertFalse(source.availableImmediately());
            Assert.assertNotEquals(source.isExhausted(), isUnbounded());
        } finally {
            source.close();
        }
    }

    @Test
    public void givenEmptySource_whenPolling_thenNullIsReturned() {
        // Given
        EventSource<TKey, TValue> source = createEmptySource();
        try {
            // When and Then
            Assert.assertNull(source.poll(Duration.ofSeconds(5)));
        } finally {
            source.close();
        }
    }

    @Test
    public void givenEmptySource_whenClosed_thenAvailabilityIsFalse_andExhaustedIsTrue() {
        // Given
        EventSource<TKey, TValue> source = createEmptySource();

        // When
        source.close();

        // Then
        Assert.assertFalse(source.availableImmediately());
        Assert.assertTrue(source.isExhausted());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void givenEmptySource_whenClosed_thenPollingThrowsError() {
        // Given
        EventSource<TKey, TValue> source = createEmptySource();

        // When
        Assert.assertNull(source.poll(Duration.ofSeconds(1)));
        source.close();

        // Then
        source.poll(Duration.ofSeconds(1));
    }

    @Test(timeOut = 10_000L)
    public void givenSingleItemSource_whenPolling_thenSingleItemIsReturned_andStatusReportedAccurately() {
        // Given
        List<Event<TKey, TValue>> sampleData = new ArrayList<>(createSampleData(1));
        EventSource<TKey, TValue> source = createSource(sampleData);

        try {
            // When
            Assert.assertFalse(source.isExhausted());
            if (guaranteesImmediateAvailability()) {
                Assert.assertTrue(source.availableImmediately());
            }

            // Then
            Event<TKey, TValue> next = source.poll(Duration.ofSeconds(5));
            Assert.assertEquals(next, sampleData.get(0));

            // And
            Assert.assertFalse(source.availableImmediately());
            Assert.assertNotEquals(source.isExhausted(), isUnbounded());
            // Should return null when no further events
            Assert.assertNull(source.poll(Duration.ofSeconds(1)));
        } finally {
            source.close();
        }
    }

    @Test(timeOut = 10_000L)
    public void givenSingleItemSource_whenClosed_thenNoFurtherEventsAvailable() {
        // Given
        List<Event<TKey, TValue>> sampleData = new ArrayList<>(createSampleData(1));
        EventSource<TKey, TValue> source = createSource(sampleData);
        Assert.assertFalse(source.isExhausted());
        if (guaranteesImmediateAvailability()) {
            Assert.assertTrue(source.availableImmediately());
        }

        // When and Then
        verifySourceClosure(source);
    }

    private void verifySourceClosure(EventSource<TKey, TValue> source) {
        // After close() no further events should be available
        source.close();
        Assert.assertTrue(source.isClosed());
        Assert.assertTrue(source.isExhausted());
        Assert.assertFalse(source.availableImmediately());

        try {
            // Should error when poll() is called after closure()
            source.poll(Duration.ofSeconds(1));
            Assert.fail("Should error when poll() is called after close() has been called");
        } catch (IllegalStateException e) {
            // Errored as expected
        }

        // Calling close() multiple times should be fine
        source.close();
    }

    @DataProvider(name = "sample-data-sizes")
    public Object[][] getTestSizes() {
        return new Object[][] {
                { 100 },
                { 10_000 },
                { 100_000 }
        };
    }

    @Test(dataProvider = "sample-data-sizes")
    public void givenPopulatedEventSource_whenPolling_thenAllEventsAreReturned(int size) {
        // Given
        List<Event<TKey, TValue>> sampleData = new ArrayList<>(this.createSampleData(size));
        EventSource<TKey, TValue> source = createSource(sampleData);

        try {
            Assert.assertFalse(source.isExhausted());
            if (guaranteesImmediateAvailability()) {
                Assert.assertTrue(source.availableImmediately());
            }

            // When and Then
            for (int i = 0; i < size; i++) {
                Assert.assertFalse(source.isExhausted());
                Event<TKey, TValue> next = source.poll(Duration.ofSeconds(5));
                Assert.assertEquals(next, sampleData.get(i));
            }

            Assert.assertFalse(source.availableImmediately());
            Assert.assertNotEquals(source.isExhausted(), isUnbounded());
        } finally {
            verifySourceClosure(source);
        }
    }

    @Test(dataProvider = "sample-data-sizes")
    public void givenPopulatedEventSource_whenPollingFirst10Items_then10ItemsAreReturned(int size) {
        // Given
        List<Event<TKey, TValue>> sampleData = new ArrayList<>(this.createSampleData(size));
        EventSource<TKey, TValue> source = createSource(sampleData);
        try {
            Assert.assertFalse(source.isExhausted());
            if (guaranteesImmediateAvailability()) {
                Assert.assertTrue(source.availableImmediately());
            }

            // When and Then
            // Read just the first 10 items then close
            for (int i = 0; i < 10; i++) {
                Assert.assertFalse(source.isExhausted());
                Event<TKey, TValue> next = source.poll(Duration.ofSeconds(5000));
                Assert.assertEquals(next, sampleData.get(i));
            }
        } finally {
            verifySourceClosure(source);
        }
    }

    @Test
    public void givenEmptyEventSource_whenQueryingRemaining_thenZeroIsReturned() {
        // Given
        EventSource<TKey, TValue> source = createEmptySource();

        try {
            // When and Then
            source.poll(Duration.ofMillis(10));
            if (this.isUnbounded()) {
                Long remaining = source.remaining();
                Assert.assertTrue(remaining == null || remaining == 0L,
                                  "Remaining should be null/0 for unbounded sources");
            } else {
                Assert.assertEquals(source.remaining(), 0L, "Remaining should be 0 for bounded sources");
            }
        } finally {
            source.close();
        }
    }

    @Test
    public void givenNonEmptyEventSource_whenReadingOneEvent_thenRemainingIsOneLessThanTotal_02() {
        // Given
        EventSource<TKey, TValue> source = createSource(createSampleData(100));

        try {
            // When
            Assert.assertNotNull(source.poll(Duration.ofSeconds(3)), "Should have read an event");

            // Then
            Assert.assertEquals(source.remaining(), 99L);
        } finally {
            source.close();
        }
    }

    @Test
    public void givenNonEmptyEventSource_whenQueryingRemaining_thenRemainingIsTotal() {
        // Given
        long expected = 100L;
        EventSource<TKey, TValue> source = createSource(createSampleData((int) expected));

        try {
            // When and Then
            verifyRemaining(source, expected);
        } finally {
            source.close();
        }
    }

    private void verifyRemaining(EventSource<TKey, TValue> source, long expected) {
        // When
        while (source.poll(Duration.ofSeconds(3)) != null) {
            // Decrement expected as the act of polling will have reduced the remaining events by 1
            expected--;

            // Then
            Assert.assertEquals(source.remaining(), expected);
        }

        // Then
        Assert.assertEquals(source.remaining(), 0L);
    }

    @Test(dataProvider = "sample-data-sizes")
    public void givenNonEmptyEventSource_whenReadingRecords_thenRemainingIsAlwaysCorrect(long expected) {
        // Given
        EventSource<TKey, TValue> source = createSource(createSampleData((int) expected));

        try {
            // When and Then
            verifyRemaining(source, expected);
        } finally {
            source.close();
        }
    }
}
