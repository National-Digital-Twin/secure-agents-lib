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

import static uk.gov.dbt.ndtp.secure.agent.sources.kafka.DockerTestSecureKafkaCluster.RECORD_ERROR_TOTAL;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSourceException;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class DockerTestMutualTlsKafkaCluster {

    private MutualTlsKafkaTestCluster kafka;

    @BeforeClass
    public void setup() {
        if (StringUtils.contains(System.getProperty("os.name"), "Windows")) {
            throw new SkipException(
                    "These tests cannot run on Windows because the SSL certificates generator script assumes a Posix compatible OS");
        }

        Utils.logTestClassStarted(this.getClass());
        this.kafka = new MutualTlsKafkaTestCluster();
        this.kafka.setup();
    }

    @AfterClass
    public void teardown() {
        this.kafka.teardown();
        Utils.logTestClassFinished(this.getClass());
    }

    @Test(expectedExceptions = KafkaException.class, expectedExceptionsMessageRegExp = "Failed to create.*")
    public void givenNoCredentials_whenCreatingAdminClient_thenErrors() {
        // Given, When and Then
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.kafka.getBootstrapServers());
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000);
        AdminClient.create(props);
    }

    @Test
    public void givenAdminClient_whenListingTopics_thenSuccess() throws ExecutionException, InterruptedException,
            TimeoutException {
        // Given
        AdminClient client = this.kafka.getAdminClient();
        Assert.assertNotNull(client);

        // When
        ListTopicsResult topics = client.listTopics();

        // Then
        Assert.assertNotNull(topics);
        Set<String> topicNames = topics.names().get(10, TimeUnit.SECONDS);
        Assert.assertNotNull(topicNames);
        Assert.assertFalse(topicNames.isEmpty());
        Assert.assertTrue(topicNames.contains(KafkaTestCluster.DEFAULT_TOPIC));
    }

    private void verifyCanPollEvents(Properties properties, boolean expectSuccess) {
        Event<Bytes, String> event = null;
        KafkaEventSource<Bytes, String> goodSource = KafkaEventSource.<Bytes, String>create()
                                                                     .bootstrapServers(this.kafka.getBootstrapServers())
                                                                     .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                                     .keyDeserializer(BytesDeserializer.class)
                                                                     .valueDeserializer(StringDeserializer.class)
                                                                     .consumerGroup("secure-cluster-03")
                                                                     .consumerConfig(
                                                                             CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG,
                                                                             5000)
                                                                     .consumerConfig(
                                                                             CommonClientConfigs.DEFAULT_API_TIMEOUT_MS_CONFIG,
                                                                             5000)
                                                                     .fromBeginning()
                                                                     .consumerConfig(properties)
                                                                     .build();
        try {
            event = goodSource.poll(Duration.ofSeconds(5));
        } catch (EventSourceException e) {
            if (!expectSuccess) {
                Assert.assertTrue(StringUtils.contains(e.getMessage(), "Security"));
            } else {
                Assert.fail("Event Source threw an error when credentials were expected to be valid");
            }
        }
        if (expectSuccess) {
            Assert.assertNotNull(event);
            Assert.assertEquals(event.value(), "Has authentication");
        } else {
            Assert.assertNull(event);
        }
        goodSource.close();
    }


    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenKafkaSink_whenSendingEvent_thenSuccess_andEventCanBePolled() throws InterruptedException {
        // Given
        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .producerConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000L)
                                                      .producerConfig(this.kafka.getClientProperties())
                                                      .build()) {

            // When
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, "Has authentication"));

            // Then
            Map<MetricName, ? extends Metric> metrics = sink.metrics();
            Assert.assertNotNull(metrics);
            Double totalErrors = DockerTestSecureKafkaCluster.findKafkaMetric(metrics, RECORD_ERROR_TOTAL);
            Assert.assertEquals(totalErrors, 0.0);
        }

        // And
        verifyCanPollEvents(this.kafka.getClientProperties(), true);
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenKafkaSinkWithoutAuthentication_whenSendingEvent_thenFails_andErrorsThrownOnClose() throws InterruptedException {
        // Given
        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .producerConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000L)
                                                      .build()) {

            // When
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, "No authentication"));

            // Then
            Map<MetricName, ? extends Metric> metrics = sink.metrics();
            Assert.assertNotNull(metrics);
            Double totalErrors = DockerTestSecureKafkaCluster.findKafkaMetric(metrics, RECORD_ERROR_TOTAL);
            Assert.assertEquals(totalErrors, 1.0);

            // And
            sink.close();
            Assert.fail("Should have thrown SinkException on closure");
        }
    }
}
