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

import static uk.gov.dbt.ndtp.secure.agent.sources.IANodeHeaders.DATA_SOURCE_NAME;
import static uk.gov.dbt.ndtp.secure.agent.sources.IANodeHeaders.DATA_SOURCE_TYPE;
import static uk.gov.dbt.ndtp.secure.agent.sources.IANodeHeaders.EXEC_PATH;
import static uk.gov.dbt.ndtp.secure.agent.sources.IANodeHeaders.INPUT_REQUEST_ID;
import static uk.gov.dbt.ndtp.secure.agent.sources.IANodeHeaders.REQUEST_ID;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.CollectorSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;

public class TestEventHeaderSink extends AbstractEventSinkCommonMethods {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void givenNoGenerators_whenBuilding_thenIllegalArgument() {
        // Given, When and Then
        try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create().build()) {
            Assert.fail("Should have thrown an error");
        }
    }

    @DataProvider(name = "badHeaderParams")
    private Object[][] badHeaderParameters() {
        return new Object[][] {
                { null, null },
                { "name", null },
                { null, "value" },
                { "", "" },
                { "name", "" },
                { "", "value" }
        };
    }

    @Test(dataProvider = "badHeaderParams", expectedExceptions = IllegalArgumentException.class)
    public void givenBadHeaderParameters_whenBuildingFixedHeader_thenIllegalArgument(String name, String value) {
        // Given, When and Then
        try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                   .fixedHeader(name, value)
                                                                   .build()) {
            Assert.fail("Should have thrown an error");
        }
    }

    @Test(dataProvider = "badHeaderParams", expectedExceptions = IllegalArgumentException.class)
    public void givenBadHeaderParameters_whenBuildingFixedIfMissingHeader_thenIllegalArgument(String name, String value) {
        // Given, When and Then
        try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                   .fixedHeaderIfMissing(name, value)
                                                                   .build()) {
            Assert.fail("Should have thrown an error");
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void givenNullGenerator_whenBuilding_thenIllegalArgument() {
        // Given, When and Then
        try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                   .headerGenerator(null)
                                                                   .build()) {
            Assert.fail("Should have thrown an error");
        }
    }

    @Test
    public void givenFixedHeader_whenSendingEvents_thenHeaderIsAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .fixedHeader("Test", "test")
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> Assert.assertEquals(e.lastHeader("Test"), "test"));
            }
        }
    }

    @Test
    public void givenConditionalFixedHeader_whenSendingEvents_thenHeaderIsAddedWhenNeeded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .fixedHeader("Test", "test")
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> Assert.assertEquals(e.lastHeader("Test"), "test"));
            }
        }
    }

    @Test
    public void givenSingleGenerator_whenSendingEvents_thenHeaderIsAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .headerGenerator(e -> new Header("Test",
                                                                                                        StringUtils.repeat(
                                                                                                                e.key(),
                                                                                                                5)))
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> Assert.assertEquals(e.lastHeader("Test"), e.value()));
            }
        }
    }

    @Test
    public void givenConditionalGenerator_whenSendingEvents_thenHeaderIsAddedForSomeEvents() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .headerGenerator(
                                                                               e -> Objects.equals(e.key(), "a") ?
                                                                                    new Header("Test", "test") : null)
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> {
                    if (Objects.equals(e.key(), "a")) {
                        Assert.assertEquals(e.lastHeader("Test"), "test");
                    } else {
                        Assert.assertNull(e.lastHeader("Test"), "Should not have added header to this event");
                    }
                });
            }
        }
    }

    @Test
    public void givenGeneratorWhichProducesNull_whenSendingEvents_thenNoHeaderIsAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .headerGenerator(e -> null)
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> Assert.assertEquals(e.headers().count(), 0));
            }
        }
    }

    @Test
    public void givenMultipleGenerators_whenSendingEvents_thenAllHeadersAreAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .headerGenerator(e -> new Header("Test", "test"))
                                                                       .headerGenerator(e -> new Header(REQUEST_ID,
                                                                                                        UUID.randomUUID()
                                                                                                            .toString()))
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> {
                    Assert.assertEquals(e.lastHeader("Test"), "test");
                    Assert.assertEquals(e.headers(REQUEST_ID).count(), 1);
                });
            }
        }
    }

    @Test
    public void givenNonIndependentGeneratorsOnSameSink_whenSendingEvents_thenOnlyFirstGeneratorAddsHeader() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .headerGenerator(e -> new Header("Test", "test"))
                                                                       .headerGenerator(e -> {
                                                                           if (StringUtils.isNotBlank(
                                                                                   e.lastHeader("Test"))) {
                                                                               return new Header("Another", "value");
                                                                           }
                                                                           return null;
                                                                       })
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> {
                    Assert.assertEquals(e.lastHeader("Test"), "test");
                    Assert.assertNull(e.lastHeader("Another"));
                });
            }
        }
    }

    @Test
    public void givenNonIndependentGeneratorsOnChainedSinks_whenSendingEvents_thenBothHeadersAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .headerGenerator(e -> new Header("Test", "test"))
                                                                       .destination(
                                                                               EventHeaderSink.<String, String>create()
                                                                                              .headerGenerator(e -> {
                                                                                                  if (StringUtils.isNotBlank(
                                                                                                          e.lastHeader(
                                                                                                                  "Test"))) {
                                                                                                      return new Header(
                                                                                                              "Another",
                                                                                                              "value");
                                                                                                  }
                                                                                                  return null;
                                                                                              })
                                                                                              .destination(collector))
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> {
                    Assert.assertEquals(e.lastHeader("Test"), "test");
                    Assert.assertEquals(e.lastHeader("Another"), "value");
                });
            }
        }
    }

    @Test
    public void givenSingleGenerator_whenSendingEventsWithExistingHeader_thenHeaderIsAdded_andAllHeaderValuesArePreserved() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .headerGenerator(e -> new Header("Test", "test"))
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink, x -> List.of(new Header("Test", "original")), () -> null);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> {
                    Assert.assertEquals(e.lastHeader("Test"), "test");
                    Assert.assertEquals(e.headers("Test").count(), 2);
                    Assert.assertEquals(e.headers("Test").findFirst().orElse(null), "original");
                });
            }
        }
    }

    @Test
    public void givenStandardHeaders_whenSendingEvents_thenExpectedHeadersAreAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .addStandardHeaders("test")
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                Set<String> requestIds = new LinkedHashSet<>();
                collector.get().forEach(e -> {
                    Assert.assertEquals(e.lastHeader(EXEC_PATH), "test");
                    String id = e.lastHeader(REQUEST_ID);
                    Assert.assertTrue(requestIds.add(id), "All Request-ID's should have been unique");
                    Assert.assertNull(e.lastHeader(INPUT_REQUEST_ID));
                });
            }
        }
    }

    @Test
    public void givenStandardHeadersWithoutAppName_whenSendingEvents_thenExpectedHeadersAreAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .addStandardHeaders(null)
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                Set<String> requestIds = new LinkedHashSet<>();
                collector.get().forEach(e -> {
                    Assert.assertNull(e.lastHeader(EXEC_PATH));
                    String id = e.lastHeader(REQUEST_ID);
                    Assert.assertTrue(requestIds.add(id), "All Request-ID's should have been unique");
                    Assert.assertNull(e.lastHeader(INPUT_REQUEST_ID));
                });
            }
        }
    }

    @Test
    public void givenStandardHeaders_whenSendingEventsWithExistingRequestId_thenExpectedHeadersAreAdded() {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .addStandardHeaders("test")
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink, x -> List.of(new Header(REQUEST_ID, x)), () -> null);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                Set<String> requestIds = new LinkedHashSet<>();
                collector.get().forEach(e -> {
                    Assert.assertEquals(e.lastHeader(EXEC_PATH), "test");
                    String id = e.lastHeader(REQUEST_ID);
                    Assert.assertTrue(requestIds.add(id), "All Request-ID's should have been unique");
                    String inputId = e.lastHeader(INPUT_REQUEST_ID);
                    Assert.assertEquals(inputId, e.key());
                });
            }
        }
    }

    @DataProvider(name = "dataSources")
    private Object[][] dataSources() {
        return new Object[][] {
                { "Test", "text/csv" },
                { "Cool Database", null },
                { null, "application/xml" },
                { "JSON Dump", "application/json" },
                { null, null }
        };
    }

    @Test(dataProvider = "dataSources")
    public void givenDataSourceHeaders_whenSendingEvents_thenDataSourceHeadersAreAdded(String name, String type) {
        // Given
        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .addRequestId()
                                                                       .addDataSourceHeaders(name, type)
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> {
                    Assert.assertNotNull(e.lastHeader(REQUEST_ID));
                    if (StringUtils.isNotBlank(name)) {
                        Assert.assertEquals(e.lastHeader(DATA_SOURCE_NAME), name);
                    } else {
                        Assert.assertNull(e.lastHeader(DATA_SOURCE_NAME),
                                          "Expected no " + DATA_SOURCE_NAME + " header to be added");
                    }
                    if (StringUtils.isNotBlank(type)) {
                        Assert.assertEquals(e.lastHeader(DATA_SOURCE_TYPE), type);
                    } else {
                        Assert.assertNull(e.lastHeader(DATA_SOURCE_TYPE));
                    }
                });
            }
        }
    }

    @Test(dataProvider = "dataSources")
    public void givenDataSourceHeaders_whenSendingEventsWithExistingDataSources_thenOnlyMissingHeadersAreChanged(
            String name,
            String type) {
        // Given
        List<Header> existing = new ArrayList<>();
        if (StringUtils.isNotBlank(name)) {
            existing.add(new Header(DATA_SOURCE_NAME, name));
        }
        if (StringUtils.isNotBlank(type)) {
            existing.add(new Header(DATA_SOURCE_TYPE, type));
        }

        try (CollectorSink<Event<String, String>> collector = CollectorSink.of()) {
            try (EventHeaderSink<String, String> sink = EventHeaderSink.<String, String>create()
                                                                       .addRequestId()
                                                                       .addDataSourceHeaders("New", "New")
                                                                       .destination(collector)
                                                                       .build()) {
                // When
                sendTestEvents(sink, x -> existing, () -> null);

                // Then
                Assert.assertEquals(collector.get().size(), KEYS.size());
                collector.get().forEach(e -> {
                    if (StringUtils.isNotBlank(name)) {
                        Assert.assertEquals(e.lastHeader(DATA_SOURCE_NAME), name,
                                            "Existing header should be unchanged");
                    } else {
                        Assert.assertEquals(e.lastHeader(DATA_SOURCE_NAME), "New", "Missing header should be added");
                    }
                    if (StringUtils.isNotBlank(type)) {
                        Assert.assertEquals(e.lastHeader(DATA_SOURCE_TYPE), type,
                                            "Existing header should be unchanged");
                    } else {
                        Assert.assertEquals(e.lastHeader(DATA_SOURCE_TYPE), "New", "Missing header should be added");
                    }
                });
            }
        }
    }
}
