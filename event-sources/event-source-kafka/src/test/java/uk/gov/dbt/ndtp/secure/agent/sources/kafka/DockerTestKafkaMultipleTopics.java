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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class DockerTestKafkaMultipleTopics {

    public static final String ADDITIONAL_TOPIC = "secondary";
    public static final String[] MANY_TOPICS = new String[] {
            KafkaTestCluster.DEFAULT_TOPIC,
            ADDITIONAL_TOPIC,
            "topic3",
            "topic4",
            "topic5",
            "topic6",
            "topic7",
            "topic8",
            "topic9",
            "topic10"
    };

    private KafkaTestCluster kafka;

    @BeforeClass
    public void setup() {
        Utils.logTestClassStarted(this.getClass());
        this.kafka = new BasicKafkaTestCluster();
        this.kafka.setup();
    }

    @AfterClass
    public void teardown() {
        this.kafka.teardown();
        Utils.logTestClassFinished(this.getClass());
    }

    @BeforeMethod
    public void testCleanup() throws InterruptedException {
        createManyTopics();
        // Need to sleep briefly after deleting and recreating the topic or can get unpredictable behaviour
        Thread.sleep(500);
    }

    private <TKey, TValue> KafkaEventSource.Builder<TKey, TValue> buildEventSource(Class<?> keyDeserializer,
                                                                                   Class<?> valueDeserializer,
                                                                                   String consumerGroup,
                                                                                   String... topics) {
        return KafkaEventSource.<TKey, TValue>create()
                               .keyDeserializer(keyDeserializer)
                               .valueDeserializer(valueDeserializer)
                               .bootstrapServers(this.kafka.getBootstrapServers())
                               .topics(topics)
                               .consumerGroup(consumerGroup);
    }

    @DataProvider(name = "dataSizes")
    private Object[][] getDataSizes() {
        //@formatter:off
        return new Object[][] {
                { 500 },
                { 2_500 },
                { 5_000 }
        };
        //@formatter:on
    }

    private static String nth(int i) {
        return switch (i % 10) {
            case 1 -> i + "st";
            case 2 -> i + "nd";
            case 3 -> i + "rd";
            default -> i + "th";
        };
    }

    public static List<Event<Integer, String>> verifyTestEvents(int eventsToRead,
                                                                KafkaEventSource<Integer, String> source, int start) {
        return verifyTestEvents(eventsToRead, source, start, Duration.ofSeconds(3));
    }

    public static List<Event<Integer, String>> verifyTestEvents(int eventsToRead,
                                                                KafkaEventSource<Integer, String> source, int start,
                                                                Duration timeout) {
        Assert.assertTrue(eventsToRead > 0, "Expected number of events to read must be > 0");

        int i = 1;
        List<Event<Integer, String>> read = new ArrayList<>();
        while (i <= eventsToRead) {
            Event<Integer, String> next = source.poll(timeout);
            if (next == null && i == 1) {
                // If we failed on the very first poll this may be because our read policy invoked a seek, and seeks
                // are not necessarily guaranteed to process until a subsequent KafkaConsumer.poll() call
                next = source.poll(timeout);
            }
            Assert.assertNotNull(next, "Expected Test event was missing, failed reading the " + nth(i) + " event");
            read.add(next);
            i++;
        }

        // Sort events because no guarantee reading from multiple topics doesn't interleave the events
        read.sort(Comparator.comparingInt(Event::key));

        // Verify the events are all present
        int expected = start;
        for (Event<Integer, String> next : read) {
            Assert.assertEquals((int) next.key(), expected);
            Assert.assertEquals(next.value(), "Test event " + expected);
            expected++;
        }

        return read;
    }

    private void insertTestEvents(int size, String topic, int start) {
        try (KafkaSink<Integer, String> sink = KafkaSink.<Integer, String>create()
                                                        .bootstrapServers(this.kafka.getBootstrapServers())
                                                        .topic(topic)
                                                        .keySerializer(IntegerSerializer.class)
                                                        .valueSerializer(StringSerializer.class)
                                                        .lingerMs(5)
                                                        .build()) {
            for (int i = 1; i <= size; i++) {
                SimpleEvent<Integer, String> event =
                        new SimpleEvent<>(Collections.emptyList(), start + i, "Test event " + (start + i));
                sink.send(event);
            }
        }
    }

    @Test(dataProvider = "dataSizes", retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleInputTopics_whenReadingEventsFromEarliest_thenAllEventsAreRead(int size) throws
            InterruptedException {
        // Given
        insertTestEvents(size, KafkaTestCluster.DEFAULT_TOPIC, 0);
        insertTestEvents(size, ADDITIONAL_TOPIC, size);
        //@formatter:off
        KafkaEventSource<Integer, String> source =
                this.<Integer, String>buildEventSource(IntegerDeserializer.class, StringDeserializer.class,
                                                       "multi_topic_earliest_" + size, KafkaTestCluster.DEFAULT_TOPIC, ADDITIONAL_TOPIC)
                    .fromEarliest()
                    .autoCommit()
                    .build();
        //@formatter:on

        // When
        Assert.assertFalse(source.isClosed());
        Assert.assertFalse(source.isExhausted());

        // Then
        verifyTestEvents(size * 2, source, 1);
        DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
        source.close();
    }

    @Test(dataProvider = "dataSizes", retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleInputTopics_whenReadingEventsFromBeginning_thenAllEventsAreRead(int size) throws
            InterruptedException {
        // Given
        insertTestEvents(size, KafkaTestCluster.DEFAULT_TOPIC, 0);
        insertTestEvents(size, ADDITIONAL_TOPIC, size);
        //@formatter:off
        KafkaEventSource<Integer, String> source =
                this.<Integer, String>buildEventSource(IntegerDeserializer.class, StringDeserializer.class,
                                                       "multi_topic_beginning_" + size, KafkaTestCluster.DEFAULT_TOPIC, ADDITIONAL_TOPIC)
                    .fromBeginning()
                    .autoCommit()
                    .build();
        //@formatter:on

        // When
        Assert.assertFalse(source.isClosed());
        Assert.assertFalse(source.isExhausted());

        // Then
        verifyTestEvents(size * 2, source, 1);
        DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
        source.close();
    }

    @Test(dataProvider = "dataSizes", retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleInputTopics_whenReadingEventsFromEnd_thenNoEventsAreRead(int size) {
        // Given
        insertTestEvents(size, KafkaTestCluster.DEFAULT_TOPIC, 0);
        insertTestEvents(size, ADDITIONAL_TOPIC, size);
        //@formatter:off
        KafkaEventSource<Integer, String> source =
                this.<Integer, String>buildEventSource(IntegerDeserializer.class, StringDeserializer.class,
                                                       "multi_topic_end_" + size, KafkaTestCluster.DEFAULT_TOPIC, ADDITIONAL_TOPIC)
                    .fromEnd()
                    .autoCommit()
                    .build();
        //@formatter:on

        // When
        Assert.assertNull(source.poll(Duration.ofSeconds(1)));

        // Then
        DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
        source.close();
    }

    @Test(dataProvider = "dataSizes", retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMultipleInputTopics_whenReadingEventsFromLatest_thenNoEventsAreRead(int size) {
        // Given
        insertTestEvents(size, KafkaTestCluster.DEFAULT_TOPIC, 0);
        insertTestEvents(size, ADDITIONAL_TOPIC, size);
        //@formatter:off
        KafkaEventSource<Integer, String> source =
                this.<Integer, String>buildEventSource(IntegerDeserializer.class, StringDeserializer.class,
                                                       "multi_topic_latest_" + size, KafkaTestCluster.DEFAULT_TOPIC, ADDITIONAL_TOPIC)
                    .fromLatest()
                    .autoCommit()
                    .build();
        //@formatter:on

        // When
        Assert.assertNull(source.poll(Duration.ofSeconds(1)));

        // Then
        DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
        source.close();
    }

    @Test(dataProvider = "dataSizes", retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenManyInputTopicsWithEventsSpreadEvenly_whenReadingEvents_thenAllEventsAreRead(int size) throws
            InterruptedException {
        // Given
        // Spread the events across ALL the topics in small batches
        for (int i = 0, j = 0; i < size; i += 100, j = (j + 1) % MANY_TOPICS.length) {
            insertTestEvents(100, MANY_TOPICS[j], i);
        }
        //@formatter:off
        KafkaEventSource<Integer, String> source =
                this.<Integer, String>buildEventSource(IntegerDeserializer.class, StringDeserializer.class,
                                                       "many_topic_01_" + size, MANY_TOPICS)
                    .fromEarliest()
                    .autoCommit()
                    .build();
        //@formatter:on

        // When
        Assert.assertFalse(source.isClosed());
        Assert.assertFalse(source.isExhausted());

        // Then
        verifyTestEvents(size, source, 1);
        DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
        source.close();
    }

    private void createManyTopics() throws InterruptedException {
        Arrays.stream(MANY_TOPICS).forEach(t -> this.kafka.resetTopic(t));
        Thread.sleep(1000);
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenManyInputTopicsWithEventsSpreadEvenly_whenReadingEventsFromOffset_thenSomeEventsAreRead() {
        // Given
        // Spread the events across ALL the topics in small batches
        for (int i = 0, j = 0; i < 5_000; i += 100, j = (j + 1) % MANY_TOPICS.length) {
            insertTestEvents(100, MANY_TOPICS[j], i);
        }
        //@formatter:off
        Map<TopicPartition, Long> offsets = new HashMap<>();
        // Skip to the 100th position in each topic
        Arrays.stream(MANY_TOPICS).forEach(topic -> offsets.put(new TopicPartition(topic, 0), 100L));
        KafkaEventSource<Integer, String> source =
                this.<Integer, String>buildEventSource(IntegerDeserializer.class, StringDeserializer.class,
                                                       "many_topic_02", MANY_TOPICS)
                    .readPolicy(KafkaReadPolicies.fromOffsets(offsets, 100L))
                    .autoCommit()
                    .build();
        //@formatter:on

        // When
        Assert.assertFalse(source.isClosed());
        Assert.assertFalse(source.isExhausted());

        // Then
        verifyTestEvents(5_000 - (100 * MANY_TOPICS.length), source, (100 * MANY_TOPICS.length) + 1);
        DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
        source.close();
    }

    @Test(dataProvider = "dataSizes", invocationCount = 3, retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenManyInputTopicsWithEventsSpreadRandomly_whenReadingEvents_thenAllEventsAreRead(int size) throws
            InterruptedException {
        // Given
        // Spread the events across the topics randomly
        Random random = new Random();
        for (int i = 0; i < size; i += 100) {
            insertTestEvents(100, MANY_TOPICS[random.nextInt(MANY_TOPICS.length)], i);
        }
        //@formatter:off
        KafkaEventSource<Integer, String> source =
                this.<Integer, String>buildEventSource(IntegerDeserializer.class, StringDeserializer.class,
                                                       "many_topic_03_" + size, MANY_TOPICS)
                    .fromEarliest()
                    .autoCommit()
                    .build();
        //@formatter:on

        // When
        Assert.assertFalse(source.isClosed());
        Assert.assertFalse(source.isExhausted());

        // Then
        verifyTestEvents(size, source, 1);
        DockerTestKafkaEventSource.verifyNoFurtherEvents(source);
        source.close();
    }
}
