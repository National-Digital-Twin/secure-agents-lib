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
package uk.gov.dbt.ndtp.secure.agent.observability.events;

import static uk.gov.dbt.ndtp.secure.agent.observability.events.CounterEvent.counterEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.CounterMetric;

public class CounterEventTest {
    @Test
    public void whenACounterEventWithNameAndCountIsCreated_thenTheCorrectEventValuesAreStored() {
        final CounterEvent event = counterEvent("theEventName", 99);

        Assert.assertEquals(event.getEventName(), "theEventName");
        Assert.assertEquals(event.getMetricName(), event.getEventName());
        Assert.assertEquals(event.getCount(), 99L);
        Assert.assertNotNull(event.getStartedAt());
        Assert.assertNotNull(event.getEndedAt());

        Assert.assertEquals(event.getMetrics().size(), 1);
        Assert.assertTrue(event.getMetrics().get(0).getClass().isAssignableFrom(CounterMetric.class));
        Assert.assertEquals(event.getMetrics().get(0).getMetricName(), event.getMetricName());
        Assert.assertEquals(event.getMetrics().get(0).getValue(), event.getCount());
        Assert.assertEquals(event.getMetrics().get(0).getStartedAt(), event.getStartedAt());
        Assert.assertEquals(event.getMetrics().get(0).getEndedAt(), event.getEndedAt());
    }

    @Test
    public void whenACounterEventWithNameOnlyIsCreated_thenTheEventHasTheCorrectNameAndSingleCount() {
        final CounterEvent event = counterEvent("theEventName");

        Assert.assertEquals(event.getEventName(), "theEventName");
        Assert.assertEquals(event.getCount(), 1L);
    }

    @Test
    public void whenACounterEventWithStartAndEndDatesIsCreated_thenTheEventHasTheCorrectChronologyAndDuration() {
        final Instant endedAt = Instant.now();
        final Instant startedAt = endedAt.minus(Duration.ofSeconds(10));
        final CounterEvent event = CounterEvent
                .builder()
                .eventName("theEventName")
                .startedAt(startedAt)
                .endedAt(endedAt)
                .build();

        Assert.assertEquals(event.getEventName(), "theEventName");
        Assert.assertEquals(event.getStartedAt(), startedAt);
        Assert.assertEquals(event.getEndedAt(), endedAt);
        Assert.assertEquals(event.getDuration(), Duration.of(endedAt.toEpochMilli() - startedAt.toEpochMilli(), ChronoUnit.MILLIS));
    }

    @Test
    public void givenACounterEventWithNameAndLabels_whenCreated_thenTheEventHasTheCorrectProperties() {
        final Instant endedAt = Instant.now();
        final Instant startedAt = endedAt.minus(Duration.ofSeconds(10));
        final CounterEvent event = CounterEvent.counterEvent("theEventName", Map.of("label1", "label1Value"));

        Assert.assertEquals(event.getEventName(), "theEventName");
        Assert.assertEquals(event.getMetricLabels().size(), 1);
        Assert.assertEquals(event.getMetricLabels().get("label1"), "label1Value");
    }
}
