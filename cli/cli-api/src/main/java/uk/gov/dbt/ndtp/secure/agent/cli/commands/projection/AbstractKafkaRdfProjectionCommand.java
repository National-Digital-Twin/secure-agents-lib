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

import java.util.List;
import java.util.function.Function;
import uk.gov.dbt.ndtp.secure.agent.payloads.RdfPayload;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaDatasetGraphSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaRdfPayloadSource;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.utils.Bytes;

/**
 * Abstract base class for commands that run a Projector using a Kafka Event source where the events are represented as
 * RDF graphs i.e. using a {@link KafkaDatasetGraphSource}
 *
 * @param <TOutput> Output type
 */
public abstract class AbstractKafkaRdfProjectionCommand<TOutput>
        extends AbstractKafkaProjectorCommand<Bytes, RdfPayload, TOutput> {

    @Override
    protected EventSource<Bytes, RdfPayload> getSource() {
        return KafkaRdfPayloadSource.<Bytes>createRdfPayload()
                                    .bootstrapServers(this.kafka.bootstrapServers)
                                    .topics(this.kafka.topics)
                                    .consumerGroup(this.kafka.getConsumerGroup())
                                    .consumerConfig(this.kafka.getAdditionalProperties())
                                    .keyDeserializer(BytesDeserializer.class)
                                    .maxPollRecords(this.kafka.getMaxPollRecords())
                                    .readPolicy(this.kafka.readPolicy.toReadPolicy())
                                    .lagReportInterval(this.kafka.getLagReportInterval())
                                    .autoCommit(this.useAutoCommit())
                                    .build();
    }

    @Override
    protected List<Function<Event<Bytes, RdfPayload>, Header>> additionalCaptureHeaderGenerators() {
        // Force the Content-Type header of captured events to the simplest and most portable format regardless of their input Content-Type header
        Function<Event<Bytes, RdfPayload>, Header> generator = e -> e.value() == null ? null : e.value().isDataset() ?
                                                                                               new Header(
                                                                                                       HttpNames.hContentType,
                                                                                                       WebContent.contentTypeNQuads) :
                                                                                               new Header(
                                                                                                       HttpNames.hContentType,
                                                                                                       WebContent.ctPatch.getContentTypeStr());
        return List.of(generator);
    }
}
