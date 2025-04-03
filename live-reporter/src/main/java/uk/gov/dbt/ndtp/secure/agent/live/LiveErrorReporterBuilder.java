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

import java.util.function.Function;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.utils.Bytes;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveError;
import uk.gov.dbt.ndtp.secure.agent.live.serializers.LiveErrorSerializer;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;

/**
 * A builder for {@link LiveErrorReporter} instances
 */
public class LiveErrorReporterBuilder {
    private String appId;
    private Sink<Event<Bytes, LiveError>> destination;

    /**
     * Sets the default Application ID that is included with reported errors
     *
     * @param applicationId Application ID
     * @return Builder
     */
    public LiveErrorReporterBuilder id(String applicationId) {
        this.appId = applicationId;
        return this;
    }

    /**
     * Sets the destination sink to which heartbeats will be sent
     *
     * @param sink Destination sink
     * @return Builder
     */
    public LiveErrorReporterBuilder destination(Sink<Event<Bytes, LiveError>> sink) {
        this.destination = sink;
        return this;
    }

    /**
     * Sets the destination sink to which heartbeats will be sent
     *
     * @param sinkBuilder Destination sink builder
     * @param <T>         Destination sink type
     * @return Builder
     */
    public <T extends Sink<Event<Bytes, LiveError>>> LiveErrorReporterBuilder destination(
            SinkBuilder<Event<Bytes, LiveError>, T> sinkBuilder) {
        this.destination = sinkBuilder.build();
        return this;
    }

    /**
     * Sets the destination sink to be Kafka providing some basic default values on the builder
     *
     * @param builderFunction Builder function that further configures the {@link KafkaSink.KafkaSinkBuilder} as
     *                        desired, as a minimum you should call
     *                        {@link KafkaSink.KafkaSinkBuilder#bootstrapServers(String)} to specify the Kafka cluster
     *                        to which heartbeats are written.
     * @return Builder
     */
    public LiveErrorReporterBuilder toKafka(
            Function<KafkaSink.KafkaSinkBuilder<Bytes, LiveError>, KafkaSink.KafkaSinkBuilder<Bytes, LiveError>> builderFunction) {
        this.destination = builderFunction.apply(KafkaSink.<Bytes, LiveError>create()
                                                          .keySerializer(BytesSerializer.class)
                                                          .valueSerializer(LiveErrorSerializer.class)
                                                          .topic(LiveErrorReporter.DEFAULT_LIVE_TOPIC)).build();
        return this;
    }

    /**
     * Builds a new {@link LiveErrorReporter}
     *
     * @return Live Error Reporter
     */
    public LiveErrorReporter build() {
        return new LiveErrorReporter(this.appId, this.destination);
    }
}
