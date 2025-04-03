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

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.dbt.ndtp.secure.agent.observability.events.CounterEvent.counterEvent;

import io.opentelemetry.api.metrics.Meter;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.IANodeMetrics;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.Metric;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.MetricTestUtils;

public class OpenTelemetryMetricsAdapterTest {
    private OpenTelemetryMetricsAdapter adapter;

    @BeforeClass
    public void setup() {
        MetricTestUtils.enableMetricsCapture();
        final Meter meter = IANodeMetrics.getMeter("test", "1.2.3");
        adapter = new OpenTelemetryMetricsAdapter(meter);
    }

    @AfterClass
    public void teardown() {
        MetricTestUtils.disableMetricsCapture();
    }

    @Test
    public void givenACounterEvent_whenTheEventIsDispatchedThroughTheAdapter_thenAnOtelCounterMetricIsCreatedAndUsedToRepresentTheCounterMetricEvent() {
        // Given counter events
        CounterEvent counterEvent1 = counterEvent("theMetricName", 111);
        CounterEvent counterEvent2 = counterEvent("theMetricName", 222);
        CounterEvent unrelatedCounterEvent = counterEvent("anotherMetricName", 222);

        // When counter metric for event1 is emitted
        adapter.on(counterEvent1);

        // Then a corresponding OTel counter metric is created with the given count
        Assert.assertEquals(MetricTestUtils.getReportedMetric(counterEvent1.getMetricName()), counterEvent1.getCount().doubleValue());

        // When another counter metric for relatedCounterEvent is emitted
        adapter.on(unrelatedCounterEvent);

        // Then a corresponding OTel counter metric is created with the given count
        Assert.assertEquals(MetricTestUtils.getReportedMetric(unrelatedCounterEvent.getMetricName()), unrelatedCounterEvent.getCount().doubleValue());

        // When more metrics for event1 are emitted
        adapter.on(counterEvent2);

        // Then the existing OTel counter metric for event1 is updated with the given count
        Assert.assertEquals(MetricTestUtils.getReportedMetric(counterEvent2.getMetricName()), counterEvent1.getCount().doubleValue() + counterEvent2.getCount().doubleValue());
    }

    @Test
    public void givenAGaugeEvent_whenTheEventIsDispatchedThroughTheAdapter_thenAnOtelHistogramMetricIsCreatedAndUsedToRepresentTheGaugeMetricEvent() {
        // Given gauge events
        GaugeEvent gauge1Event1 = GaugeEvent.builder().metricName("gauge1MetricName").value(1111d).build();
        GaugeEvent gauge1Event2 = GaugeEvent.builder().metricName("gauge1MetricName").value(2222d).build();

        // When gauge 1 metric event 1 is emitted
        adapter.on(gauge1Event1);

        // Then a corresponding OTel metric is created with the given count
        Assert.assertEquals(MetricTestUtils.getReportedMetric(gauge1Event1.getMetricName()), gauge1Event1.getValue().doubleValue());

        // When gauge 1 metric event 2 is emitted with an updated value
        adapter.on(gauge1Event1);

        // Then a corresponding OTel metric is updated with the new gauge value
        Assert.assertEquals(MetricTestUtils.getReportedMetric(gauge1Event2.getMetricName()), gauge1Event2.getValue().doubleValue());
    }

    @Test
    public void givenADurationEvent_whenTheEventIsDispatchedThroughTheAdapter_thenAnOtelHistogramMetricIsCreatedAndUsedToRepresentTheDurationMetricEvent() {
        // When a duration event for metric1 is emitted
        DurationEvent durationEvent1Metric1 = DurationEvent.durationEvent("durationMetric1Name", 1000, 5000);
        adapter.on(durationEvent1Metric1);

        // Then a corresponding OTel histogram metric is created with the given value
        Assert.assertEquals(MetricTestUtils.getReportedMetric(durationEvent1Metric1.getMetricName()), durationEvent1Metric1.getDuration().toMillis());

        // When a different duration event for metric2 is emitted
        DurationEvent anotherDurationEventMetric = DurationEvent.durationEvent("anotherDurationEventMetric", 2000, 4000);
        adapter.on(anotherDurationEventMetric);

        // Then a corresponding OTel metric is created with the given count
        Assert.assertEquals(MetricTestUtils.getReportedMetric(anotherDurationEventMetric.getMetricName()), anotherDurationEventMetric.getDuration().toMillis());

        // When another duration event for metric1 is emitted
        DurationEvent durationEvent2Metric1 = DurationEvent.durationEvent(durationEvent1Metric1.getMetricName(), 1000, 2000);
        adapter.on(durationEvent2Metric1);

        // Then the existing OTel histogram metric is updated with the given value (test utility sum of all values added to histrogram)
        Assert.assertEquals(MetricTestUtils.getReportedMetric(durationEvent2Metric1.getMetricName()), durationEvent1Metric1.getDuration().toMillis() + durationEvent2Metric1.getDuration().toMillis());
    }

    @Test
    public void givenAnUnknownEventTypeNotHandledByTheAdapter_whenTheEventIsDispatchedThroughTheAdapter_thenTheAdapterIgnoresTheEvent() {
        // Given an unknown event type
        Meter meter = mock(Meter.class);
        OpenTelemetryMetricsAdapter adapter = new OpenTelemetryMetricsAdapter(meter);
        ComponentEvent unhandledTypeOfEvent = mock(ComponentEvent.class);
        Metric unhandledTypeOfMetric = mock(Metric.class);
        MetricEvent unhandledTypeOfMetricEvent = mock(MetricEvent.class);
        when(unhandledTypeOfMetricEvent.getMetrics()).thenReturn(singletonList(unhandledTypeOfMetric));

        // When
        adapter.on(unhandledTypeOfEvent);
        adapter.on((ComponentEvent)unhandledTypeOfMetricEvent);

        // Then
        verifyNoMoreInteractions(meter);
    }
}
