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

import static java.util.Map.entry;
import static java.util.Objects.isNull;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.CounterMetric;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.DurationMetric;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.GaugeMetric;
import uk.gov.dbt.ndtp.secure.agent.observability.metrics.Metric;

/**
 * <p>
 * An adapter of <code>{@link MetricEvent}s</code> to <a href="https://opentelemetry.io">OpenTelemetry metrics</a>.
 * </p>
 * <p>
 * Listens for metric events and dynamically creates associated metrics through the <a href="https://opentelemetry.io/docs/specs/otel/metrics/api/">OpenTelemetry Metrics API</a>
 * </p>
 * <p>
 * Note: Unfortunately, the <a href="https://opentelemetry.io">OpenTelemetry</a> API does not have a metric/instrument class hierarchy with a common base class. As a result we use {@link Object} as the common
 * base for all instruments created.
 * </p>
 */
@Slf4j
public class OpenTelemetryMetricsAdapter implements EventListener<ComponentEvent> {
    private final ConcurrentHashMap<String, Consumer<Metric>> metricKeyToOtMetric = new ConcurrentHashMap<>();

    private final Map<Class<? extends Metric>, Function<Metric, Pair<Class<?>, ?>>> metricTypeToOtAdapter;

    private final Map<Pair<Class<? extends Metric>, Class<?>>, BiConsumer<? extends Metric, ?>> metricTypesToAdapter;

    private final Consumer<? extends Metric> NO_OP_ADAPTER = m -> {};

    /**
     * Creates a new OpenTelemetry metrics adapter.
     *
     * @param meter the OpenTelemetry meter this adapter will use to create instruments and report measurements.
     */
    public OpenTelemetryMetricsAdapter(Meter meter) {
        metricTypeToOtAdapter = Map.ofEntries(
                entry(CounterMetric.class, (metric) -> Pair.of(LongCounter.class, meter.counterBuilder(metric.getMetricName()).build())),
                entry(GaugeMetric.class, (metric) -> Pair.of(DoubleHistogram.class, meter.histogramBuilder(metric.getMetricName()).build())),
                entry(DurationMetric.class, (metric) -> Pair.of(DoubleHistogram.class, meter.histogramBuilder(metric.getMetricName()).build()))
        );

        metricTypesToAdapter = Map.ofEntries(
            entry(Pair.of(CounterMetric.class, LongCounter.class), (BiConsumer<CounterMetric, LongCounter>)(m, ot) -> ot.add(m.getCount().longValue())),
            entry(Pair.of(GaugeMetric.class, DoubleHistogram.class), (BiConsumer<GaugeMetric, DoubleHistogram>)(m, ot) -> ot.record(m.getValue().doubleValue())),
            entry(Pair.of(DurationMetric.class, DoubleHistogram.class), (BiConsumer<DurationMetric, DoubleHistogram>)(m, ot) -> ot.record(m.getValue().doubleValue()))
        );
    }

    /**
     * Receives a new event, filtering on metric events and calling {@link #on(MetricEvent)}.
     *
     * @param event the event to be handled by this interested listener.
     * @see #on(MetricEvent)
     */
    @Override
    public void on(final ComponentEvent event) {
        if (event instanceof MetricEvent) {
            on((MetricEvent)event);
        }
    }

    /**
     * Receives a new metric event and adapts the event onto the equivalent OpenTelemetry metric, if possible.
     * @param event the event to be handled by this interested listener.
     */
    public void on(final MetricEvent event) {
        event.getMetrics().forEach(metric -> metricKeyToOtMetric.computeIfAbsent(determineMetricKeyFor(event, metric), k -> adapterFor(metric)).accept(metric));
    }

    private <M extends Metric> Pair<Class<?>, ?> instrumentFor(M metric) {
        Function<Metric, Pair<Class<?>, ?>> metricTypeAndOtelMetricType = metricTypeToOtAdapter.get(metric.getClass());
        return metricTypeAndOtelMetricType == null ? null : metricTypeAndOtelMetricType.apply(metric);
    }

    private String determineMetricKeyFor(final MetricEvent event, final Metric metric) {
        return event.getEventName() + "_" + metric.getMetricName();
    }

    @SuppressWarnings("unchecked")
    private <M extends Metric> Consumer<M> adapterFor(final M metric) {
        final Pair<Class<?>, ?> otelInstrumentTypeAndInstance = instrumentFor(metric);
        if (isNull(otelInstrumentTypeAndInstance)) {
            log.info("No OpenTelemetry instrument registered for metric type [{}] - no instrumentation will occur for this metric type...", metric.getClass());
            return (Consumer<M>) NO_OP_ADAPTER;
        }

        final BiConsumer<M, Object> adapterForMetricAndOtMetricTypes = (BiConsumer<M, Object>)metricTypesToAdapter.get(Pair.of(metric.getClass(), otelInstrumentTypeAndInstance.getLeft()));
        if (isNull(adapterForMetricAndOtMetricTypes)) {
            log.info("No metric adapter registered for metric type [{}] and OpenTelemetry metricType [{}]- no instrumentation will occur for this metric type...", metric.getClass(), otelInstrumentTypeAndInstance.getLeft());
            return (Consumer<M>) NO_OP_ADAPTER;
        }

        return (M m) -> adapterForMetricAndOtMetricTypes.accept(m, otelInstrumentTypeAndInstance.getRight());
    }
}
