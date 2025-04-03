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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.projection;

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Command;
import java.util.function.Supplier;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;

@Command(name = "project")
public class AsIsProjectionCommand extends AbstractKafkaProjectorCommand<Bytes, Bytes, Event<Bytes, Bytes>> {

    @AirlineModule
    public DeadLetterTestingOptions<Bytes, Bytes> deadLetterTestingOptions = new DeadLetterTestingOptions();

    public static void main(String[] args) {
        SecureAgentCommand.runAsSingleCommand(AsIsProjectionCommand.class, args);
    }

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
        return "events";
    }

    @Override
    protected Supplier<HealthStatus> getHealthProbeSupplier() {
        // Test commands always consider themselves to be healthy
        return () -> HealthStatus.builder().healthy(true).build();
    }

    @Override
    protected Sink<Event<Bytes, Bytes>> prepareWorkSink() {
        Sink<Event<Bytes, Bytes>> deadLetters =
                this.prepareDeadLetterSink(this.kafka.dlqTopic, this.keySerializerClass(), this.valueSerializerClass());
        return new PeriodicDeadLetterSink<>(this.deadLetterTestingOptions.successful,
                                            this.deadLetterTestingOptions.deadLetterFrequency,
                                            deadLetters);
    }

    @Override
    protected IODescriptor getLiveReporterOutputDescriptor() {
        return new IODescriptor("test", "test");
    }

}
