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
import static java.util.Objects.nonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.GaugeMetric;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.Metric;

/**
 *  A gauge event, which represents a non-additive value at an instant (e.g. Current speed, request size, ...).
 */
@Getter
@SuperBuilder
public class GaugeEvent extends AbstractEvent implements MetricEvent {
    /** The name of the gauge metric to associate with this event, or null if the event name is to be used. */
    private final String metricName;
    /** Any labels applied to the metric associated with this event, which may be null indicating none. */
    private final Map<String, Object> metricLabels;
    /** The reported value of the gauge. */
    private final Number value;

    @Override
    public List<Metric> getMetrics() {
        return singletonList(GaugeMetric.builder()
                .startedAt(getStartedAt())
                .endedAt(getEndedAt())
                .metricName(nonNull(getMetricName()) ? getMetricName() : getEventName())
                .value(getValue())
                .labels((nonNull(getMetricLabels()) ? getMetricLabels() : Collections.emptyMap()))
                .build()
        );
    }
}
