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

import static uk.gov.dbt.ndtp.secure.agent.observability.events.DurationEvent.durationEvent;

import java.time.Instant;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.DurationMetric;

public class DurationEventTest {
    @Test
    public void givenADurationEventWithNameAndStartAndEnd_whenCreated_thenTheCorrectEventValuesAreStored() {
        final long startedAt = System.currentTimeMillis();
        final long endedAt = System.currentTimeMillis();
        final DurationEvent event = durationEvent("theEventName", startedAt, endedAt);

        Assert.assertEquals(event.getEventName(), "theEventName");
        Assert.assertEquals(event.getMetricName(), event.getEventName());
        Assert.assertEquals(event.getStartedAt(), Instant.ofEpochMilli(startedAt));
        Assert.assertEquals(event.getEndedAt(), Instant.ofEpochMilli(endedAt));

        Assert.assertEquals(event.getMetrics().size(), 1);
        Assert.assertTrue(event.getMetrics().get(0).getClass().isAssignableFrom(DurationMetric.class));
        Assert.assertEquals(event.getMetrics().get(0).getMetricName(), event.getMetricName());
        Assert.assertEquals(event.getMetrics().get(0).getStartedAt(), event.getStartedAt());
        Assert.assertEquals(event.getMetrics().get(0).getEndedAt(), event.getEndedAt());
    }

    @Test
    public void givenADurationEventWithNameAndStartAndEndAndLabels_whenCreated_thenTheCorrectEventValuesAreStored() {
        final long startedAt = System.currentTimeMillis();
        final long endedAt = System.currentTimeMillis();
        final DurationEvent event = durationEvent("theEventName", startedAt, endedAt, Map.of("label1", "label1Value"));

        Assert.assertEquals(event.getEventName(), "theEventName");
        Assert.assertEquals(event.getMetricName(), event.getEventName());
        Assert.assertEquals(event.getStartedAt(), Instant.ofEpochMilli(startedAt));
        Assert.assertEquals(event.getEndedAt(), Instant.ofEpochMilli(endedAt));

        Assert.assertEquals(event.getMetrics().size(), 1);
        Assert.assertTrue(event.getMetrics().get(0).getClass().isAssignableFrom(DurationMetric.class));
        Assert.assertEquals(event.getMetrics().get(0).getMetricName(), event.getMetricName());
        Assert.assertEquals(event.getMetrics().get(0).getStartedAt(), event.getStartedAt());
        Assert.assertEquals(event.getMetrics().get(0).getEndedAt(), event.getEndedAt());

        Assert.assertEquals(event.getMetrics().get(0).getLabels().size(), 1);
        Assert.assertEquals(event.getMetrics().get(0).getLabels().get("label1"), "label1Value");
    }
}
