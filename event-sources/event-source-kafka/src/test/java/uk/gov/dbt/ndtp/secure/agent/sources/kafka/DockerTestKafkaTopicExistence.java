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

import static java.util.Objects.nonNull;
import static uk.gov.dbt.ndtp.secure.agent.sources.kafka.Utils.waitAWhileFor;
import static uk.gov.dbt.ndtp.secure.agent.sources.kafka.Utils.waitAWhileOrFailFor;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.NetworkClient;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

/**
 * Topic existence checks run with a fresh Kafka cluster per-test because otherwise we've observed the tests interfering
 * with each other in CI/CD due to differing timing and scheduling behaviours in those environments versus local
 * developer machines
 */
public class DockerTestKafkaTopicExistence {

    public static final String NO_SUCH_TOPIC = "no-such-topic";
    private KafkaTestCluster<?> kafka;

    private AdminClient adminClient;

    @BeforeClass
    public void setup() {
        Utils.logTestClassStarted(this.getClass());
    }

    @BeforeMethod
    public void testSetup() {
        this.kafka = new BasicKafkaTestCluster();
        this.kafka.setup();

        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.kafka.getBootstrapServers());
        adminClient = KafkaAdminClient.create(props);
    }

    @AfterMethod
    public void testCleanup() {
        this.kafka.teardown();
    }

    @AfterClass
    public void teardown() {
        Utils.logTestClassFinished(this.getClass());
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNonExistentTopic_whenCheckingForExistence_thenFalseIsReturned() {
        // Given
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                                          Set.of(NO_SUCH_TOPIC), null);

        try {
            // When
            boolean anyTopicExists = checker.anyTopicExists(Duration.ofSeconds(1));

            // Then
            Assert.assertFalse(anyTopicExists);
        } finally {
            checker.close();
        }
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleTopicsWhereSomeExist_whenCheckingForExistence_thenTrueIsReturned() {
        // Given
        Set<String> topics = Set.of(NO_SUCH_TOPIC, KafkaTestCluster.DEFAULT_TOPIC);
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                        topics, null);

        try {
            // When
            boolean anyTopicExists = waitAWhileFor("Any topic "+topics+" to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));

            // Then
            Assert.assertTrue(anyTopicExists);
        } finally {
            checker.close();
        }
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleTopicsWhereSomeExist_whenCheckingForExistenceRepeatedly_thenTrueIsReturned() {
        // Given
        Set<String> topics = Set.of(NO_SUCH_TOPIC, KafkaTestCluster.DEFAULT_TOPIC);
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                        topics, null);

        try {
            // When
            waitAWhileOrFailFor("Any topic "+topics+" to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));

            // And
            waitAWhileOrFailFor("Any topic "+topics+" to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));

            // Then
            waitAWhileOrFailFor("Any topic "+topics+" to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));
        } finally {
            checker.close();
        }
    }

    @Test(timeOut = 10000, retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleTopicsWhereSomeExist_whenCheckingForExistenceWithDifferentTimeouts_thenTrueIsPromptlyReturned() {
        // Given
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                                          Set.of(NO_SUCH_TOPIC, KafkaTestCluster.DEFAULT_TOPIC), null);

        try {
            // When
            // Intentionally long timeout here, the positive check should still return in a timely fashion and allow us to
            // promptly proceed
            boolean anyTopicExists = checker.anyTopicExists(Duration.ofSeconds(30));
            Assert.assertTrue(anyTopicExists);
            anyTopicExists = checker.anyTopicExists(Duration.ofSeconds(1));

            // Then
            Assert.assertTrue(anyTopicExists);
        } finally {
            checker.close();
        }
    }

    @Test(timeOut = 15000, retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleTopicsWhereSomeExist_whenCheckingForExistenceWithDifferentTimeoutsAndLaterCreatingTopic_thenTrueIsEventuallyReturned() throws
            InterruptedException {
        // Given
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                                          Set.of(NO_SUCH_TOPIC, KafkaTestCluster.DEFAULT_TOPIC), null);

        try {
            // When
            // Intentionally long timeout here, the positive check should still return in a timely fashion and allow us to
            // promptly proceed
            boolean allTopicsExist = checker.allTopicsExist(Duration.ofSeconds(30));
            Assert.assertFalse(allTopicsExist);
            this.kafka.resetTopic(NO_SUCH_TOPIC);
            Thread.sleep(500);
            allTopicsExist = checker.allTopicsExist(Duration.ofSeconds(3));

            // Then
            Assert.assertTrue(allTopicsExist);
        } finally {
            checker.close();
        }
    }

    @Test(timeOut = 5000, retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNonExistentTopic_whenCheckingForExistenceAndLaterCreatingTopic_thenTrueIsEventuallyReturned() throws
            InterruptedException {
        // Given
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                                          Set.of(NO_SUCH_TOPIC), null);

        try {
            // When
            boolean anyTopicExists = checker.anyTopicExists(Duration.ofSeconds(1));
            Assert.assertFalse(anyTopicExists);

            // And
            this.kafka.resetTopic(NO_SUCH_TOPIC);

            // Then
            waitAWhileOrFailFor("Topic, "+NO_SUCH_TOPIC+", to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));
        } finally {
            checker.close();
        }
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenKafkaSourceForNonExistentTopic_whenPollingForEvents_thenLogsAreNotFlooded_andPollSucceedsAfterTopicCreation() throws
            InterruptedException {
        // Given
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                        Set.of(NO_SUCH_TOPIC), null);
        TestLogger networkClientLogger = TestLoggerFactory.getTestLogger(NetworkClient.class);
        networkClientLogger.clearAll();
        TestLogger kafkaSourceLogger = TestLoggerFactory.getTestLogger(KafkaEventSource.class);
        kafkaSourceLogger.clearAll();

        KafkaEventSource<Bytes, Bytes> source = KafkaEventSource.<Bytes, Bytes>create()
                                                                .bootstrapServers(this.kafka.getBootstrapServers())
                                                                .topic(NO_SUCH_TOPIC)
                                                                .keyDeserializer(BytesDeserializer.class)
                                                                .valueDeserializer(BytesDeserializer.class)
                                                                .consumerGroup("no-such-topic-01")
                                                                .build();

        try {
            // When
            Assert.assertNull(source.poll(Duration.ofSeconds(1)));

            // Then
            DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
            // The Kafka log message that would flood should not be triggered
            verifyLogMessages(networkClientLogger, "UNKNOWN_TOPIC_OR_PARTITION", 0);
            // Our log message that the topic does not exist should be generated only once
            verifyLogMessages(kafkaSourceLogger, "no-such-topic does not currently exist", 1);

            // And
            // Now create our previously missing topic

            this.kafka.resetTopic(NO_SUCH_TOPIC);

            waitAWhileOrFailFor("Topic, "+NO_SUCH_TOPIC+", to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));

            Assert.assertNull(source.poll(Duration.ofSeconds(3)));
            verifyLogMessages(networkClientLogger, "FetchableTopicResponse", 1);

            DockerTestKafkaEventSource.verifyClosure(source);
        } finally {
            checker.close();
            source.close();
            this.kafka.deleteTopic(NO_SUCH_TOPIC);
            Thread.sleep(1000);
        }

    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenKafkaSourceForMultipleTopicsWhereOnlySomeExist_whenPollingForEvents_thenValidTopicsArePolled_andSomeLogFloodingMayOccur() throws
            InterruptedException {
        // Given
        TestLogger networkClientLogger = TestLoggerFactory.getTestLogger(NetworkClient.class);
        networkClientLogger.clearAll();
        TestLogger kafkaSourceLogger = TestLoggerFactory.getTestLogger(KafkaEventSource.class);
        kafkaSourceLogger.clearAll();
        injectBlankEvent(KafkaTestCluster.DEFAULT_TOPIC);

        KafkaEventSource<Bytes, Bytes> source = KafkaEventSource.<Bytes, Bytes>create()
                                                                .bootstrapServers(this.kafka.getBootstrapServers())
                                                                .topics(KafkaTestCluster.DEFAULT_TOPIC, NO_SUCH_TOPIC)
                                                                .keyDeserializer(BytesDeserializer.class)
                                                                .valueDeserializer(BytesDeserializer.class)
                                                                .consumerGroup("no-such-topic-01")
                                                                .build();

        try {
            // When
            Assert.assertNotNull(source.poll(Duration.ofSeconds(1)));
            Thread.sleep(1250);

            // Then
            DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
            // Our log message that the topic does not exist will still be generated
            verifyLogMessages(kafkaSourceLogger, "no-such-topic does not currently exist", 1);

            // And
            // The Kafka log message that would flood may still be triggered as only some of the polled topics exist
            verifySomeLogMessages(networkClientLogger, "UNKNOWN_TOPIC_OR_PARTITION");
        } finally {
            source.close();
        }
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenKafkaSourceForMultipleTopicsWhereFirstIsNotExistent_whenPollingForEvents_thenValidTopicsArePolled_andSomeLogFloodingMayOccur() {
        // Given
        TestLogger networkClientLogger = TestLoggerFactory.getTestLogger(NetworkClient.class);
        networkClientLogger.clearAll();
        TestLogger kakfaSourceLogger = TestLoggerFactory.getTestLogger(KafkaEventSource.class);
        kakfaSourceLogger.clearAll();
        injectBlankEvent(KafkaTestCluster.DEFAULT_TOPIC);

        KafkaEventSource<Bytes, Bytes> source = KafkaEventSource.<Bytes, Bytes>create()
                                                                .bootstrapServers(this.kafka.getBootstrapServers())
                                                                .topics(NO_SUCH_TOPIC, KafkaTestCluster.DEFAULT_TOPIC)
                                                                .keyDeserializer(BytesDeserializer.class)
                                                                .valueDeserializer(BytesDeserializer.class)
                                                                .consumerGroup("no-such-topic-01")
                                                                .build();

        try {
            // When
            waitAWhileOrFailFor("Some Events to be read from topic, "+KafkaTestCluster.DEFAULT_TOPIC, () -> nonNull(source.poll(Duration.ofSeconds(1))));

            // Then
            DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
            // Our log message that the topic does not exist should be generated
            verifyLogMessages(kakfaSourceLogger, "no-such-topic does not currently exist", 1);

            // And
            // The Kafka log message that would flood may still be triggered as only some of the polled topics exist
            verifySomeLogMessages(networkClientLogger, "UNKNOWN_TOPIC_OR_PARTITION");
        } finally {
            source.close();
        }
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenKafkaSourceForNonExistentTopic_whenCreatingTopicLate_thenPollSucceeds() throws
            InterruptedException {
        // Given
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                        Set.of(NO_SUCH_TOPIC), null);
        TestLogger networkClientLogger = TestLoggerFactory.getTestLogger(NetworkClient.class);
        networkClientLogger.clearAll();
        TestLogger kafkaSourceLogger = TestLoggerFactory.getTestLogger(KafkaEventSource.class);
        kafkaSourceLogger.clearAll();

        KafkaEventSource<Bytes, Bytes> source = KafkaEventSource.<Bytes, Bytes>create()
                                                                .bootstrapServers(this.kafka.getBootstrapServers())
                                                                .topic(NO_SUCH_TOPIC)
                                                                .keyDeserializer(BytesDeserializer.class)
                                                                .valueDeserializer(BytesDeserializer.class)
                                                                .consumerGroup("no-such-topic-02")
                                                                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // When
            // Submit our poll() call.  This should block initially because the topic does not exist, and then because
            // it's waiting for events on the topic.  When we later create the topic this should return an event
            // successfully
            Future<?> future = executor.submit(() -> {
                // Then
                Assert.assertNotNull(source.poll(Duration.ofSeconds(20)));
            });
            Thread.sleep(2500);

            // Now create our previously missing topic
            try {
                this.kafka.resetTopic(NO_SUCH_TOPIC);

                waitAWhileOrFailFor("Topic, "+NO_SUCH_TOPIC+", to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));

                // Write an event to the topic, our blocked poll call should then receive this and pass its assertion
                injectBlankEvent();

                future.get(10, TimeUnit.SECONDS);

                DockerTestKafkaEventSource.verifyClosure(source);
            } catch (ExecutionException e) {
                Assert.fail("poll() failed: " + e.getMessage());
            } catch (TimeoutException e) {
                Assert.fail("poll() failed to return in a timely fashion");
            }
        } finally {
            checker.close();
            source.close();
            if (executor != null) executor.shutdownNow();
        }

        // Then
        // The Kafka log message that would flood should not be triggered
        verifyLogMessages(networkClientLogger, "UNKNOWN_TOPIC_OR_PARTITION", 0);
        // Our log message that the topic does not exist should never be generated as the topic will exist before our
        // first topic existence check times out
        verifyLogMessages(kafkaSourceLogger, "no-such-topic does not currently exist", 0);
        verifyLogMessages(networkClientLogger, "FetchableTopicResponse", 1);
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenKafkaSourceForNonExistentTopic_whenCreatingTopicLate_thenFirstPollFails_andSubsequentPollSucceeds() throws
            InterruptedException {
        // Given
        TopicExistenceChecker checker =
                new TopicExistenceChecker(adminClient, this.kafka.getBootstrapServers(),
                        Set.of(NO_SUCH_TOPIC), null);
        TestLogger networkClientLogger = TestLoggerFactory.getTestLogger(NetworkClient.class);
        networkClientLogger.clearAll();
        TestLogger kafkaSourceLogger = TestLoggerFactory.getTestLogger(KafkaEventSource.class);
        kafkaSourceLogger.clearAll();

        KafkaEventSource<Bytes, Bytes> source = KafkaEventSource.<Bytes, Bytes>create()
                                                                .bootstrapServers(this.kafka.getBootstrapServers())
                                                                .topic(NO_SUCH_TOPIC)
                                                                .keyDeserializer(BytesDeserializer.class)
                                                                .valueDeserializer(BytesDeserializer.class)
                                                                .consumerGroup("no-such-topic-03")
                                                                .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // When
            // Submit our poll() call.  This should block initially because the topic does not exist and return null
            // The subsequent poll() call should then block again because the topic does not exist and then because
            // it's waiting for events on the topic.  When we later create the topic this should return an event
            // successfully
            Future<?> future = executor.submit(() -> {
                // Then
                Assert.assertNull(source.poll(Duration.ofSeconds(5)), "First poll() should return null");

                // And wait between poll attempts for topic to be created
                waitAWhileOrFailFor("Topic, "+NO_SUCH_TOPIC+", to exist", () -> checker.anyTopicExists(Duration.ofSeconds(1)));

                // And
                Assert.assertNotNull(source.poll(Duration.ofSeconds(5)), "Second poll() should return an event");
            });
            Thread.sleep(6000);

            // Now create our previously missing topic
            try {
                this.kafka.resetTopic(NO_SUCH_TOPIC);
                Thread.sleep(2000);

                // Write an event to the topic, our blocked poll call should then receive this and pass its assertion
                injectBlankEvent();

                future.get(10, TimeUnit.SECONDS);

                DockerTestKafkaEventSource.verifyClosure(source);
            } catch (ExecutionException e) {
                Assert.fail("poll() failed: " + e.getMessage());
            } catch (TimeoutException e) {
                Assert.fail("poll() failed to return in a timely fashion");
            }
        } finally {
            checker.close();
            source.close();
            if (executor != null) executor.shutdownNow();
        }

        // Then
        // The Kafka log message that would flood should not be triggered
        verifyLogMessages(networkClientLogger, "UNKNOWN_TOPIC_OR_PARTITION", 0);
        // Our log message that the topic does not exist should be generated
        verifyLogMessages(kafkaSourceLogger, "no-such-topic does not currently exist", 1);
        verifyLogMessages(networkClientLogger, "FetchableTopicResponse", 1);
    }

    private void injectBlankEvent() {
        injectBlankEvent(NO_SUCH_TOPIC);
    }

    private void injectBlankEvent(String topic) {
        try (KafkaSink<Bytes, Bytes> sink = KafkaSink.<Bytes, Bytes>create()
                                                     .bootstrapServers(this.kafka.getBootstrapServers())
                                                     .topic(topic)
                                                     .keySerializer(BytesSerializer.class)
                                                     .valueSerializer(BytesSerializer.class)
                                                     .build()) {
            sink.send(new SimpleEvent<>(Collections.emptyList(), null, Bytes.wrap(new byte[10])));
        }
    }

    private static void verifySomeLogMessages(final TestLogger logger, final String expectedMessage) {
        waitAWhileOrFailFor("Any log messages containing \""+expectedMessage+"\"",
                () -> logger.getAllLoggingEvents()
                            .stream()
                            .map(e -> e.getFormattedMessage())
                            .filter(m -> StringUtils.contains(m, expectedMessage))
                            .anyMatch(x -> true)
        );
    }

    private static void verifyLogMessages(final TestLogger logger, final String expectedMessage, final int expectedMessageCount) {
        waitAWhileOrFailFor("There being "+expectedMessageCount+ " log messages containing \""+expectedMessage+"\"",
                () -> logger.getAllLoggingEvents()
                        .stream()
                        .map(e -> e.getFormattedMessage())
                        .filter(m -> StringUtils.contains(m, expectedMessage))
                        .count() == expectedMessageCount
        );
    }
}
