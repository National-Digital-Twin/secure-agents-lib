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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.DurationMetric;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.Metric;

/**
 *  An event that reflects a duration of time (e.g. The time taken to service an HTTP request, time taken to perform a search etc.).
 */
@Getter
@SuperBuilder
public class DurationEvent extends AbstractEvent implements MetricEvent {
    /** The name of the counter metric to associate with this event, or null if the event name is to be used. */
    private final String metricName;
    /** Any labels applied to the metric associated with this event, which may be null indicating none. */
    private final Map<String, Object> metricLabels;

    /**
     * Creates a new duration event with the given start/end time and event/metric name.
     *
     * @param name the name of the event and associated metric.
     * @param startedAt the epoch duration start time in milliseconds.
     * @param endedAt the epoch duration end time in milliseconds.
     * @return a new duration event, configured with the given duration and name.
     */
    public static DurationEvent durationEvent(final String name,
                                              final long startedAt,
                                              final long endedAt) {
        return durationEvent(name, startedAt, endedAt, null);
    }

    /**
     * Creates a new duration event with the given start/end times, event/metric name and metric labels.
     *
     * @param name the name of the event and associated metric.
     * @param startedAt the epoch duration start time in milliseconds.
     * @param endedAt the epoch duration end time in milliseconds.
     * @param metricLabels the metric labels associated with this event,
     * @return a new duration event, configured with the given duration and name.
     */
    public static DurationEvent durationEvent(final String name,
                                              final long startedAt,
                                              final long endedAt,
                                              final Map<String, Object> metricLabels) {
        return builder()
                .startedAt(Instant.ofEpochMilli(startedAt))
                .endedAt(Instant.ofEpochMilli(endedAt))
                .eventName(name)
                .metricName(name)
                .metricLabels(metricLabels)
                .build();
    }

    @Override
    public List<Metric> getMetrics() {
        return singletonList(DurationMetric.builder()
                                           .startedAt(getStartedAt())
                                           .endedAt(getEndedAt())
                                           .metricName(nonNull(getMetricName()) ? getMetricName() : getEventName())
                                           .labels((nonNull(getMetricLabels()) ? getMetricLabels() : emptyMap()))
                                           .build()
        );
    }
}
