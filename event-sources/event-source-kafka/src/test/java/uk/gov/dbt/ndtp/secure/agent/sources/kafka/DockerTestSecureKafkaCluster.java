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
import org.apache.kafka.clients.admin.KafkaAdminClient;
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
import org.jetbrains.annotations.Nullable;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSourceException;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class DockerTestSecureKafkaCluster {

    public static final String RECORD_ERROR_TOTAL = "record-error-total";
    public static final String NO_SUCH_USER = "no-such-user";
    public static final String NO_SUCH_SECRET = "no-such-secret";
    public SecureKafkaTestCluster kafka;

    @BeforeClass
    public void setup() {
        Utils.logTestClassStarted(this.getClass());
        this.kafka = new SecureKafkaTestCluster(SecureKafkaTestCluster.DEFAULT_ADMIN_USERNAME,
                                                SecureKafkaTestCluster.DEFAULT_ADMIN_PASSWORD,
                                                Map.of(SecureKafkaTestCluster.DEFAULT_CLIENT_USERNAME,
                                                       SecureKafkaTestCluster.DEFAULT_CLIENT_PASSWORD, "extra",
                                                       "secret-squirrel"));
        this.kafka.setup();
    }

    @AfterClass
    public void teardown() {
        this.kafka.teardown();
        Utils.logTestClassFinished(this.getClass());
    }

    @Test
    public void givenValidCredentials_whenListingTopics_thenSuccess() throws ExecutionException, InterruptedException,
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

    @SuppressWarnings("resource")
    @Test(expectedExceptions = KafkaException.class, expectedExceptionsMessageRegExp = "Failed to create.*")
    public void givenNoCredentials_whenCreatingAdminClient_thenFails() {
        // Given
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafka.getBootstrapServers());
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000);

        // When and Then
        KafkaAdminClient.create(props);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = KafkaException.class, expectedExceptionsMessageRegExp = "Failed to create.*")
    public void givenBadCredentials_whenCreatingAdminClient_thenFails() {
        // Given
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafka.getBootstrapServers());
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000);
        props.putAll(this.kafka.getClientProperties(NO_SUCH_USER, NO_SUCH_SECRET));

        // When and Then
        KafkaAdminClient.create(props);
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenNoCredentials_whenSendingToKafka_thenNothingSent_andErrorsThrownOnClose() {
        // Given
        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .producerConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000L)
                                                      .build()) {

            // When
            // This doesn't fail in of itself because the underlying KafkaProducer.send() call is async
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, "No authentication"));

            // Then
            // However, the error should have been recorded by the producer
            verifyRecordErrorMetric(sink);

            // And
            // When we close the async errors should have been recorded and will be thrown
            verifyFailOnClosure(sink);
        }
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenNoCredentials_whenSendingToKafkaSynchronously_thenFailsImmediately() {
        // Given
        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .producerConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000L)
                                                      .noAsync()
                                                      .build()) {

            // When and Then
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, "No authentication"));
            Assert.fail("Synchronous send should have failed immediately");
        }
    }

    private static void verifyRecordErrorMetric(KafkaSink<?, ?> sink) {
        Map<MetricName, ? extends Metric> metrics = sink.metrics();
        Assert.assertNotNull(metrics);
        Double totalErrors = findKafkaMetric(metrics, RECORD_ERROR_TOTAL);
        Assert.assertNotNull(totalErrors);
        Assert.assertTrue(Double.compare(totalErrors, 0.0) > 0);
    }

    private static void verifyFailOnClosure(KafkaSink<Bytes, String> sink) {
        sink.close();
        Assert.fail("Should have thrown SinkException upon closure");
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenBadCredentials_whenSendingToKafka_thenNothingSent_andErrorsThrownOnClose() {
        // Given
        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .producerConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000L)
                                                      .producerConfig(this.kafka.getClientProperties(NO_SUCH_USER,
                                                                                                     NO_SUCH_SECRET))
                                                      .build()) {
            // When
            // This doesn't fail in of itself because the underlying KafkaProducer.send() is async
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, "Bad authentication"));

            // Then
            // However, the error should have been recorded by the producer
            verifyRecordErrorMetric(sink);

            // And
            verifyFailOnClosure(sink);
        }
    }

    @Test(expectedExceptions = SinkException.class)
    public void givenBadCredentials_whenSendingToKafkaSynchronously_thenFailsImmediately() {
        // Given
        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .producerConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000L)
                                                      .producerConfig(this.kafka.getClientProperties(NO_SUCH_USER,
                                                                                                     NO_SUCH_SECRET))
                                                      .noAsync()
                                                      .build()) {

            // When and Then
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, "No authentication"));
            Assert.fail("Synchronous send should have failed immediately");
        }
    }

    @Nullable
    public static Double findKafkaMetric(Map<MetricName, ? extends Metric> metrics, String name) {
        return metrics.entrySet()
                      .stream()
                      .filter(m -> m.getKey().name().equals(name))
                      .map(m -> (Double) m.getValue().metricValue())
                      .findFirst()
                      .orElse(null);
    }

    @Test
    public void givenValidCredentials_whenSendingToKafka_thenSuccess_andEventsCanBePolled() {
        // Given
        try (KafkaSink<Bytes, String> sink = KafkaSink.<Bytes, String>create()
                                                      .bootstrapServers(this.kafka.getBootstrapServers())
                                                      .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                      .keySerializer(BytesSerializer.class)
                                                      .valueSerializer(StringSerializer.class)
                                                      .producerConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000L)
                                                      .producerConfig(this.kafka.getClientProperties(
                                                              this.kafka.getAdminUsername(),
                                                              this.kafka.getAdminPassword()))
                                                      .build()) {
            // When
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, "Has authentication"));

            // Then
            verifyNoRecordErrors(sink);
        }

        // And
        verifyUnableToPollEventsWithoutCredentials();
        verifyPollWithAllValidUsers();
        verifyPollEventsWithCredentials(NO_SUCH_USER, NO_SUCH_SECRET, false);
    }

    private void verifyPollWithAllValidUsers() {
        verifyPollEventsWithCredentials(this.kafka.getAdminUsername(), this.kafka.getAdminPassword(), true);
        for (Map.Entry<String, String> user : this.kafka.getAdditionalUsers().entrySet()) {
            verifyPollEventsWithCredentials(user.getKey(), user.getValue(), true);
        }
    }

    private static void verifyNoRecordErrors(KafkaSink<Bytes, String> sink) {
        Map<MetricName, ? extends Metric> metrics = sink.metrics();
        Assert.assertNotNull(metrics);
        Double totalErrors = findKafkaMetric(metrics, RECORD_ERROR_TOTAL);
        Assert.assertEquals(totalErrors, 0.0);
    }

    private void verifyPollEventsWithCredentials(String username, String password, boolean areCredentialsValid) {
        Utils.logStdOut("Polling for events with %s credentials", areCredentialsValid ? "valid" : "invalid");
        Event<Bytes, String> event = null;
        KafkaEventSource<Bytes, String> goodSource = KafkaEventSource.<Bytes, String>create()
                                                                     .bootstrapServers(this.kafka.getBootstrapServers())
                                                                     .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                                     .keyDeserializer(BytesDeserializer.class)
                                                                     .valueDeserializer(StringDeserializer.class)
                                                                     .consumerGroup("secure-cluster-03")
                                                                     .fromBeginning()
                                                                     .consumerConfig(
                                                                             this.kafka.getClientProperties(username,
                                                                                                            password))
                                                                     .consumerConfig(
                                                                             CommonClientConfigs.DEFAULT_API_TIMEOUT_MS_CONFIG,
                                                                             3000)
                                                                     .consumerConfig(
                                                                             CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG,
                                                                             3000)
                                                                     .plainLogin(username, password)
                                                                     .build();
        try {
            event = goodSource.poll(Duration.ofSeconds(3));
        } catch (EventSourceException e) {
            if (!areCredentialsValid) {
                Assert.assertTrue(StringUtils.contains(e.getMessage(), "Security"));
            } else {
                Assert.fail("Event Source threw an error when credentials were expected to be valid");
            }
        }
        if (areCredentialsValid) {
            Assert.assertNotNull(event);
            Assert.assertEquals(event.value(), "Has authentication");
        } else {
            Assert.assertNull(event);
        }
        goodSource.close();
        Utils.logStdOut("Finished polling for events");
    }

    private void verifyUnableToPollEventsWithoutCredentials() {
        Utils.logStdOut("Trying to poll for events with no credentials");
        KafkaEventSource<Bytes, String> badSource = KafkaEventSource.<Bytes, String>create()
                                                                    .bootstrapServers(this.kafka.getBootstrapServers())
                                                                    .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                                    .keyDeserializer(BytesDeserializer.class)
                                                                    .valueDeserializer(StringDeserializer.class)
                                                                    .consumerGroup("secure-cluster-03")
                                                                    .consumerConfig(
                                                                            CommonClientConfigs.DEFAULT_API_TIMEOUT_MS_CONFIG,
                                                                            3000)
                                                                    .consumerConfig(
                                                                            CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG,
                                                                            3000)
                                                                    .fromBeginning()
                                                                    .build();
        Event<Bytes, String> event = badSource.poll(Duration.ofSeconds(3));
        Assert.assertNull(event);
        badSource.close();
        Utils.logStdOut("Finished polling for events");
    }
}
