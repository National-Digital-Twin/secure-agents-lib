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
package uk.gov.dbt.ndtp.secure.agent.live;

import java.time.Duration;
import java.util.function.Function;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.utils.Bytes;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveHeartbeat;
import uk.gov.dbt.ndtp.secure.agent.live.serializers.LiveHeartbeatSerializer;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;

/**
 * A builder for {@link LiveReporter} instances
 */
public class LiveReporterBuilder {

    private Sink<Event<Bytes, LiveHeartbeat>> sink = null;
    private Duration reportingPeriod = LiveReporter.DEFAULT_REPORTING_PERIOD_DURATION;
    private String id, name, componentType;
    private IODescriptor input, output;

    /**
     * Sets the destination sink to which heartbeats will be sent
     *
     * @param sink Destination sink
     * @return Builder
     */
    public LiveReporterBuilder destination(Sink<Event<Bytes, LiveHeartbeat>> sink) {
        this.sink = sink;
        return this;
    }

    /**
     * Sets the destination sink to which heartbeats will be sent
     *
     * @param sinkBuilder Destination sink builder
     * @param <T>         Destination sink type
     * @return Builder
     */
    public <T extends Sink<Event<Bytes, LiveHeartbeat>>> LiveReporterBuilder destination(
            SinkBuilder<Event<Bytes, LiveHeartbeat>, T> sinkBuilder) {
        this.sink = sinkBuilder.build();
        return this;
    }

    /**
     * Sets the destination sink to be Kafka providing some basic default values on the builder
     *
     * @param builderFunction Builder function that further configures the {@link KafkaSink.KafkaSinkBuilder} as
     *                        desired, as a minimum you should call
     *                        {@link
     *                        KafkaSink.KafkaSinkBuilder#bootstrapServers(String)}
     *                        to specify the Kafka cluster to which heartbeats are written.
     * @return Builder
     */
    public LiveReporterBuilder toKafka(
            Function<KafkaSink.KafkaSinkBuilder<Bytes, LiveHeartbeat>, KafkaSink.KafkaSinkBuilder<Bytes, LiveHeartbeat>> builderFunction) {
        this.sink = builderFunction.apply(KafkaSink.<Bytes, LiveHeartbeat>create()
                                                   .keySerializer(BytesSerializer.class)
                                                   .valueSerializer(LiveHeartbeatSerializer.class)
                                                   .topic(LiveReporter.DEFAULT_LIVE_TOPIC)).build();
        return this;
    }

    /**
     * Sets the ID of the application
     *
     * @param id ID
     * @return Builder
     */
    public LiveReporterBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the human-readable name of the application
     *
     * @param name Name
     * @return Builder
     */
    public LiveReporterBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the component type for the application
     *
     * @param componentType Component Type
     * @return Builder
     */
    public LiveReporterBuilder componentType(String componentType) {
        this.componentType = componentType;
        return this;
    }

    /**
     * Sets the reporting period for the application i.e. how often a heartbeat will be sent
     *
     * @param period Reporting period
     * @return Builder
     */
    public LiveReporterBuilder reportingPeriod(Duration period) {
        this.reportingPeriod = period;
        return this;
    }

    /**
     * Sets the input for the application
     *
     * @param input Input descriptor
     * @return Builder
     */
    public LiveReporterBuilder input(IODescriptor input) {
        this.input = input;
        return this;
    }

    /**
     * Sets the input for the application
     *
     * @param name Input name
     * @param type Input type
     * @return Builder
     */
    public LiveReporterBuilder input(String name, String type) {
        return input(new IODescriptor(name, type));
    }

    /**
     * Sets the output for the application
     *
     * @param output Output descriptor
     * @return Builder
     */
    public LiveReporterBuilder output(IODescriptor output) {
        this.output = output;
        return this;
    }

    /**
     * Sets the output for the application
     *
     * @param name Output name
     * @param type Output type
     * @return Builder
     */
    public LiveReporterBuilder output(String name, String type) {
        return output(new IODescriptor(name, type));
    }

    /**
     * Builds the Live Reporter based on the configuration defined on this builder
     *
     * @return Live Reporter
     */
    public LiveReporter build() {
        return new LiveReporter(this.sink, this.reportingPeriod, this.id, this.name, this.componentType, this.input,
                                this.output);
    }
}
