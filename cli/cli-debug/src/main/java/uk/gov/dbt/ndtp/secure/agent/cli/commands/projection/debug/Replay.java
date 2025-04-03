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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug;

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.model.CommandMetadata;
import java.util.function.Supplier;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.projectors.NoOpProjector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Projector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.AbstractProjectorCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.options.KafkaOutputOptions;

/**
 * A debug command for replaying an event capture back onto a Kafka topic
 */
@Command(name = "replay", description = "Replays a previously obtained event capture back onto a Kafka topic without any interpretation of its contents.")
public class Replay extends AbstractProjectorCommand<Bytes, Bytes, Event<Bytes, Bytes>> {

    @AirlineModule
    private final KafkaOutputOptions kafkaOutputOptions = new KafkaOutputOptions();

    @Override
    protected Serializer<Bytes> keySerializer() {
        return new BytesSerializer();
    }

    @Override
    protected Deserializer<Bytes> keyDeserializer() {
        return new BytesDeserializer();
    }

    @Override
    protected Serializer<Bytes> valueSerializer() {
        return new BytesSerializer();
    }

    @Override
    protected Deserializer<Bytes> valueDeserializer() {
        return new BytesDeserializer();
    }

    @Override
    protected String getThroughputItemsName() {
        return "Events";
    }

    @Override
    protected Supplier<HealthStatus> getHealthProbeSupplier() {
        // Debug commands always consider themselves to be healthy
        return () -> HealthStatus.builder().healthy(true).build();
    }

    @Override
    protected EventSource<Bytes, Bytes> getSource() {
        return this.fileSourceOptions.getFileSource(this.keyDeserializer(), this.valueDeserializer());
    }

    @Override
    protected Projector<Event<Bytes, Bytes>, Event<Bytes, Bytes>> getProjector() {
        return new NoOpProjector<>();
    }

    @Override
    protected Sink<Event<Bytes, Bytes>> prepareWorkSink() {
        return KafkaSink.<Bytes, Bytes>create()
                        .bootstrapServers(this.kafkaOutputOptions.bootstrapServers)
                        .topic(this.kafkaOutputOptions.topic)
                        .keySerializer(BytesSerializer.class)
                        .valueSerializer(BytesSerializer.class)
                        .producerConfig(this.kafkaOutputOptions.getAdditionalProperties())
                        .lingerMs(5)
                        .build();
    }

    @Override
    protected void setupLiveReporter(CommandMetadata metadata) {
        //@formatter:off
        this.liveReporter.setupLiveReporter(this.kafkaOutputOptions.bootstrapServers,
                                            "Event Capture Replay to Kafka",
                                            metadata.getName(),
                                            "adapter",
                                            new IODescriptor(this.fileSourceOptions.getCaptureDirectory(), "directory"),
                                            new IODescriptor(this.kafkaOutputOptions.topic, "topic"));
        //@formatter:on

        this.liveReporter.setupErrorReporter(this.kafkaOutputOptions.bootstrapServers, metadata.getName());
    }
}
