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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestKafkaSinkErrorHandling {

    private static final SimpleEvent<Integer, String> EVENT = new SimpleEvent<>(Collections.emptyList(), 1, "Test");

    private KafkaSink.KafkaSinkBuilder<Integer, String> getBuilder() {
        Properties props = new Properties();
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, 1000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);

        return KafkaSink.<Integer, String>create()
                        .bootstrapServers("localhost:9092")
                        .topic(KafkaTestCluster.DEFAULT_TOPIC)
                        .keySerializer(IntegerSerializer.class)
                        .valueSerializer(StringSerializer.class)
                        .producerConfig(props);
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenKafkaSink_whenSendingToSink_thenSendSucceeds_andCloseFails() {
        // Given
        try (KafkaSink<Integer, String> sink = getBuilder().async().build()) {
            // When and Then
            sink.send(EVENT);

            // And
            sink.close();
            Assert.fail("Should have thrown a SinkException");
        }
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenKafkaSink_whenSendingToSink_thenSendSucceeds_andSubsequentSendFails() throws InterruptedException {
        // Given
        try (KafkaSink<Integer, String> sink = getBuilder().async().build()) {
            // When and Then
            sink.send(EVENT);

            // And
            // NB - Need a brief wait to allow the previous send to time out and fail
            Thread.sleep(1500);
            sink.send(EVENT);
            Assert.fail("Should have thrown a SinkException");
        }
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenKafkaSink_whenSendingToSinkSynchronously_thenSendFails() {
        // Given
        try (KafkaSink<Integer, String> sink = getBuilder().noAsync().build()) {
            // When and Then
            sink.send(EVENT);
        }
    }

    @Test
    public void givenKafkaSinkAndCustomCallback_whenSendingToSink_thenSendSucceeds_andCallbackInvoked() {
        // Given
        TrackerCallback callback = new TrackerCallback();

        try (KafkaSink<Integer, String> sink = getBuilder().async(callback).build()) {
            // When and Then
            sink.send(EVENT);
        }

        // And
        Assert.assertEquals(callback.failure.get(), 1);
        Assert.assertEquals(callback.errors.size(), 1);
    }

    public static final class TrackerCallback implements Callback {
        public final AtomicInteger success = new AtomicInteger(0);
        public final AtomicInteger failure = new AtomicInteger(0);
        public final List<Exception> errors = new ArrayList<>();

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception != null) {
                this.failure.incrementAndGet();
                this.errors.add(exception);
            } else {
                this.success.incrementAndGet();
            }
        }
    }
}
