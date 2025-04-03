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
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.projectors.NoOpProjector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Projector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.AbstractKafkaProjectorCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.options.OffsetStoreOptions;

/**
 * A debug command that dumps a Kafka topic to the console assuming the values can be interpreted as strings
 */
@Command(name = "dump",
        description = "Dumps the contents of a topic to the console assuming values can be treated as strings and ignoring keys")
public class Dump extends AbstractKafkaProjectorCommand<Bytes, String, Event<String, String>> {

    @AirlineModule
    private OffsetStoreOptions offsetStoreOptions = new OffsetStoreOptions();

    @Override
    protected Serializer<Bytes> keySerializer() {
        return new BytesSerializer();
    }

    @Override
    protected Deserializer<Bytes> keyDeserializer() {
        return new BytesDeserializer();
    }

    @Override
    protected Serializer<String> valueSerializer() {
        return new StringSerializer();
    }

    @Override
    protected Deserializer<String> valueDeserializer() {
        return new StringDeserializer();
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
    protected EventSource<Bytes, String> getSource() {
        return KafkaEventSource
                .<Bytes, String>create()
                .keyDeserializer(BytesDeserializer.class)
                .valueDeserializer(StringDeserializer.class)
                .bootstrapServers(this.kafka.bootstrapServers)
                .topics(this.kafka.topics)
                .consumerGroup(this.kafka.getConsumerGroup())
                .consumerConfig(this.kafka.getAdditionalProperties())
                .maxPollRecords(this.kafka.getMaxPollRecords())
                .readPolicy(this.kafka.readPolicy.toReadPolicy())
                .lagReportInterval(this.kafka.getLagReportInterval())
                .autoCommit(this.useAutoCommit())
                .externalOffsetStore(this.offsetStoreOptions.getOffsetStore())
                .build();
    }

    @Override
    protected Projector getProjector() {
        return new NoOpProjector();
    }

    @Override
    protected Sink<Event<String, String>> prepareWorkSink() {
        return event -> System.out.println(event.value());
    }

    @Override
    protected String getLiveReporterApplicationName(CommandMetadata metadata) {
        return "Kafka Topic Dumper";
    }

    @Override
    protected IODescriptor getLiveReporterOutputDescriptor() {
        return new IODescriptor("stdout", "stream");
    }
}
