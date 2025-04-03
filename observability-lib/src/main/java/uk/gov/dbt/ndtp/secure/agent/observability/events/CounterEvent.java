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
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.CounterMetric;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.Metric;

/**
 *  A counter event, which represents a counter that is always increasing by a positive amount.
 */
@Getter
@SuperBuilder
public class CounterEvent extends AbstractEvent implements MetricEvent {
    /** The name of the counter metric to associate with this event, or null if the event name is to be used. */
    private final String metricName;
    /** Any labels applied to the metric associated with this event, which may be null indicating none. */
    private final Map<String, Object> metricLabels;
    /** A positive count of the items observed by this event. */
    private final Number count;

    /**
     * Constructs a counter event with the given metric name and count tally.
     *
     * @param name the name of the event and metric.
     * @param count the count associated with the counter event created.
     * @return a new counter event with the given event/metric name and count.
     */
    public static CounterEvent counterEvent(final String name, final long count) {
        return CounterEvent.builder().eventName(name).metricName(name).count(count).build();
    }

    /**
     * Constructs a counter event with the given metric name and single count.
     *
     * @param name the name of the event and metric.
     * @return a new counter event with the given event/metric name and single count.
     * @see #counterEvent(String, long)
     */
    public static CounterEvent counterEvent(final String name) {
        return counterEvent(name, 1);
    }

    /**
     * Constructs a counter event with the given metric name, single count and associated metric labels.
     *
     * @param name the name of the event and metric.
     * @param metricLabels the metric labels associated with this event,
     * @return a new counter event with the given event/metric name and labels.
     */
    public static CounterEvent counterEvent(final String name, final Map<String, Object> metricLabels) {
        return counterEvent(name, 1, metricLabels);
    }

    /**
     * Constructs a counter event with the given metric name, single count and associated metric labels.
     *
     * @param name the name of the event and metric.
     * @param count the count associated with the counter event created.
     * @param metricLabels the metric labels associated with this event,
     * @return a new counter event with the given event/metric name, count and labels.
     */
    public static CounterEvent counterEvent(final String name,
                                            final long count,
                                            final Map<String, Object> metricLabels) {
        return CounterEvent.builder().eventName(name).metricName(name).count(count).metricLabels(metricLabels).build();
    }

    @Override
    public List<Metric> getMetrics() {
        return singletonList(CounterMetric.builder()
                                          .startedAt(getStartedAt())
                                          .endedAt(getEndedAt())
                                          .metricName(nonNull(getMetricName()) ? getMetricName() : getEventName())
                                          .count(getCount())
                                          .labels((nonNull(getMetricLabels()) ? getMetricLabels() : Collections.emptyMap()))
                                          .build()
        );
    }
}
