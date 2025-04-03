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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events;

import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.InMemoryEventSource;

public class TestEventProcessedSink extends AbstractEventSinkCommonMethods {

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*>= 1")
    public void givenNegativeBatchSize_whenBuilding_thenIllegalArgument() {
        new EventProcessedSink<>(-1);
    }

    @Test
    public void givenNoBatching_whenSendingEvents_thenNoBatchesArePresent() {
        // Given
        try (EventProcessedSink<String, String> sink = EventProcessedSink.<String, String>create()
                                                                         .noBatching()
                                                                         .build()) {
            // When
            InMemoryEventSource<String, String> source = new InMemoryEventSource<>(Collections.emptyList());
            sendTestEvents(sink, source);

            // Then
            Assert.assertEquals(sink.incompleteBatches(), 0);
            Assert.assertEquals(sink.batchedEvents(), 0);
        }
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*has been closed")
    public void givenClosedSource_whenSendingEvents_thenIllegalState() {
        // Given
        try (EventProcessedSink<String, String> sink = EventProcessedSink.<String, String>create().build()) {
            InMemoryEventSource<String, String> source = new InMemoryEventSource<>(Collections.emptyList());
            source.close();

            // When and Then
            sendTestEvents(sink, source);
        }
    }

    @Test
    public void givenBatching_whenSendingEvents_thenBatchesArePresent() {
        // Given
        try (EventProcessedSink<String, String> sink = EventProcessedSink.<String, String>create()
                                                                         .batchSize(100)
                                                                         .build()) {
            InMemoryEventSource<String, String> source = new InMemoryEventSource<>(Collections.emptyList());

            // When
            sendTestEvents(sink, source);

            // Then
            Assert.assertEquals(sink.incompleteBatches(), 1);
            Assert.assertEquals(sink.batchedEvents(), KEYS.size());
        }
    }

    @Test
    public void givenBatching_whenSendingMoreEventsThanBatchSize_thenSomeBatchesArePresent() {
        // Given
        try (EventProcessedSink<String, String> sink = EventProcessedSink.<String, String>create()
                                                                         .batchSize(3)
                                                                         .build()) {
            InMemoryEventSource<String, String> source = new InMemoryEventSource<>(Collections.emptyList());

            // When
            sendTestEvents(sink, source);

            // Then
            Assert.assertEquals(sink.incompleteBatches(), 1);
            Assert.assertEquals(sink.batchedEvents(), KEYS.size() - 3);
        }
    }

    @Test
    public void givenNoSource_whenSendingEvents_thenNoBatchesPresent() {
        // Given
        try (EventProcessedSink<String, String> sink = EventProcessedSink.<String, String>create()
                                                                         .noBatching()
                                                                         .build()) {
            // When
            sendTestEvents(sink, null);

            // Then
            Assert.assertEquals(sink.incompleteBatches(), 0);
            Assert.assertEquals(sink.batchedEvents(), 0);
        }
    }

    @Test
    public void givenBatching_whenSendingEventsMatchingBatchSize_thenNoBatchesPresent() {
        // Given
        try (EventProcessedSink<String, String> sink = EventProcessedSink.<String, String>create()
                                                                         .batchSize(KEYS.size())
                                                                         .build()) {
            InMemoryEventSource<String, String> source = new InMemoryEventSource<>(Collections.emptyList());

            // When
            sendTestEvents(sink, source);

            // Then
            Assert.assertEquals(sink.incompleteBatches(), 0);
            Assert.assertEquals(sink.batchedEvents(), 0);
        }
    }

    @Test
    public void givenBatchSize_whenSendingNothing_thenNoBatchesPresent() {
        // Given
        try (EventProcessedSink<String, String> sink = EventProcessedSink.<String, String>create()
                                                                         .batchSize(10)
                                                                         .build()) {
            // When and Then
            Assert.assertEquals(sink.incompleteBatches(), 0);
            Assert.assertEquals(sink.batchedEvents(), 0);
        }
    }
}
