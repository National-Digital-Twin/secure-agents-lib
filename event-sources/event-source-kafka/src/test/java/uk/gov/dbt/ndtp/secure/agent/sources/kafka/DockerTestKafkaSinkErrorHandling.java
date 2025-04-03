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

import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class DockerTestKafkaSinkErrorHandling {

    private final KafkaTestCluster<?> kafka = new BasicKafkaTestCluster();

    /**
     * An event where the value is intentionally above Kafka's default record size limit so should always result in a
     * producer error
     */
    private static final SimpleEvent<Integer, Bytes> TOO_LARGE_EVENT =
            new SimpleEvent<>(Collections.emptyList(), 1, Bytes.wrap(new byte[1024 * 1024 * 2]));

    @BeforeClass
    public void setup() {
        Utils.logTestClassStarted(DockerTestKafkaSinkErrorHandling.class);
        this.kafka.setup();
    }

    @AfterClass
    public void teardown() {
        this.kafka.teardown();
        Utils.logTestClassFinished(DockerTestKafkaSinkErrorHandling.class);
    }

    private KafkaSink.KafkaSinkBuilder<Integer, Bytes> getBuilder() {
        Properties props = new Properties();
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);

        return KafkaSink.<Integer, Bytes>create()
                        .bootstrapServers(this.kafka.getBootstrapServers())
                        .topic(KafkaTestCluster.DEFAULT_TOPIC)
                        .keySerializer(IntegerSerializer.class)
                        .valueSerializer(BytesSerializer.class)
                        .producerConfig(props)
                        .producerConfig(this.kafka.getClientProperties());
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenKafkaSink_whenSendingToSink_thenSendSucceeds_andCloseFails() {
        // Given
        try (KafkaSink<Integer, Bytes> sink = getBuilder().async().build()) {
            // When and Then
            sink.send(TOO_LARGE_EVENT);

            // And
            sink.close();
            Assert.fail("Should have thrown a SinkException");
        }
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenKafkaSink_whenSendingToSink_thenSendSucceeds_andSubsequentSendFails() throws InterruptedException {
        // Given
        try (KafkaSink<Integer, Bytes> sink = getBuilder().async().build()) {
            // When and Then
            sink.send(TOO_LARGE_EVENT);

            // And
            // NB - Need a brief wait to allow the previous send to time out and fail
            Thread.sleep(1500);
            sink.send(TOO_LARGE_EVENT);
            Assert.fail("Should have thrown a SinkException");
        }
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenKafkaSink_whenSendingToSinkSynchronously_thenSendFails() {
        // Given
        try (KafkaSink<Integer, Bytes> sink = getBuilder().noAsync().build()) {
            // When and Then
            sink.send(TOO_LARGE_EVENT);
        }
    }

    @Test
    public void givenKafkaSinkAndCustomCallback_whenSendingToSink_thenSendSucceeds_andCallbackInvoked() {
        // Given
        TestKafkaSinkErrorHandling.TrackerCallback tracker = new TestKafkaSinkErrorHandling.TrackerCallback();

        try (KafkaSink<Integer, Bytes> sink = getBuilder().async(tracker).build()) {
            // When and Then
            sink.send(TOO_LARGE_EVENT);
        }

        // And
        Assert.assertEquals(tracker.failure.get(), 1);
        Assert.assertEquals(tracker.errors.size(), 1);
        Assert.assertTrue(tracker.errors.get(0) instanceof RecordTooLargeException);
    }
}
