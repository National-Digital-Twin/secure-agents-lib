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
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.projectors.Projector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;

@Command(name = "projector")
public class TransformingProjectorCommand
        extends AbstractKafkaProjectorCommand<String, String, Event<Integer, String>> {

    @AirlineModule
    public DeadLetterTestingOptions<Integer, String> deadLetterTestingOptions = new DeadLetterTestingOptions();

    public static void main(String[] args) {
        SecureAgentCommand.runAsSingleCommand(TransformingProjectorCommand.class, args);
    }

    @Override
    protected IODescriptor getLiveReporterOutputDescriptor() {
        return new IODescriptor("test", "test");
    }

    @Override
    protected Serializer<String> keySerializer() {
        return new StringSerializer();
    }

    @Override
    protected Deserializer<String> keyDeserializer() {
        return new StringDeserializer();
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
        return "events";
    }

    @Override
    protected Supplier<HealthStatus> getHealthProbeSupplier() {
        // Test commands always consider themselves to be healthy
        return () -> HealthStatus.builder().healthy(true).build();
    }

    @Override
    protected Projector<Event<String, String>, Event<Integer, String>> getProjector() {
        return (event, sink) -> sink.send(event.replaceKey(Integer.parseInt(event.key())));
    }

    @Override
    protected Sink<Event<Integer, String>> prepareWorkSink() {
        Sink<Event<Integer, String>> deadLetters =
                this.prepareDeadLetterSink(this.kafka.dlqTopic, IntegerSerializer.class, StringSerializer.class);
        return new PeriodicDeadLetterSink<>(this.deadLetterTestingOptions.successful,
                                            this.deadLetterTestingOptions.deadLetterFrequency,
                                            deadLetters);
    }

    @Override
    protected <K, V> Sink<Event<K, V>> prepareDeadLetterSink(String dlqTopic, Class<?> keySerializer,
                                                             Class<?> valueSerializer) {
        if (StringUtils.isBlank(dlqTopic)) return null;

        return KafkaSink.<K, V>create()
                        .bootstrapServers(this.kafka.bootstrapServers)
                        .topic(dlqTopic)
                        .keySerializer(keySerializer)
                        .valueSerializer(valueSerializer)
                        .producerConfig(this.kafka.getAdditionalProperties())
                        .lingerMs(5)
                        .build();
    }
}
