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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.LogTruncationException;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.NoOffsetForPartitionException;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.FencedInstanceIdException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.OffsetOutOfRangeException;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.AbstractEventSourceTests;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSourceException;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestKafkaEventSource extends AbstractEventSourceTests<Integer, String> {

    public static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String TEST_TOPIC = "test";
    public static final String TEST_GROUP = "test-group";
    protected MockKafkaEventSource<Integer, String> kafkaEventSource;

    @Override
    protected EventSource<Integer, String> createEmptySource() {
        return createSource(Collections.emptyList());
    }

    @Override
    protected final EventSource<Integer, String> createSource(Collection<Event<Integer, String>> events) {
        kafkaEventSource = createMockKafkaEventSource(events);
        return kafkaEventSource;
    }

    /**
     * Provides a mock Kafka event source for testing
     *
     * @param events Events that the mock source should provide
     * @return Mock Kafka Event Source
     */
    protected MockKafkaEventSource<Integer, String> createMockKafkaEventSource(
            Collection<Event<Integer, String>> events) {
        return new MockKafkaEventSource<>(DEFAULT_BOOTSTRAP_SERVERS, Set.of(TEST_TOPIC), TEST_GROUP,
                                          StringSerializer.class.getCanonicalName(),
                                          StringSerializer.class.getCanonicalName(), 100,
                                          KafkaReadPolicies.fromBeginning(), true, events);
    }

    @Override
    protected Collection<Event<Integer, String>> createSampleData(int size) {
        AtomicInteger counter = new AtomicInteger(0);
        return createSampleStrings(size).stream()
                                        .map(s -> new SimpleEvent<>(Collections.emptyList(), counter.getAndIncrement(),
                                                                    s)).collect(Collectors.toList());
    }

    @Override
    public boolean guaranteesImmediateAvailability() {
        return false;
    }

    @Override
    public boolean isUnbounded() {
        return true;
    }

    @DataProvider(name = "kafka-poll-unrecoverable-errors")
    protected Object[][] getKafkaPollUnrecoverableErrors() {
        TopicPartition foo = new TopicPartition("foo", 0);
        return new Object[][] {
                { new NoOffsetForPartitionException(foo) },
                { new OffsetOutOfRangeException("Bad offset") },
                {
                        new LogTruncationException("Kafka log was truncated, data loss has occurred",
                                                   Map.of(foo, 1L),
                                                   Map.of(foo, new OffsetAndMetadata(2L)))
                },
                { new AuthenticationException("User 'foo' is not authenticated") },
                { new AuthorizationException("User 'foo' does not have permission to read topic 'bar'") },
                { new InvalidTopicException("Topic 'bar' is invalid") },
                { new FencedInstanceIdException("Fenced by Kafka") },
                { new UnsupportedVersionException("Version unsupported") }
        };
    }

    @DataProvider(name = "kafka-poll-unrecoverable-errors-2")
    protected Object[][] getKafkaPollUnrecoverableErrors2() {
        return new Object[][] {
                { new IllegalStateException("No topic subscriptions/assignments have been made on this consumer") },
                { new IllegalArgumentException("Invalid timeout") },
                { new ArithmeticException("Timeout too large") },
                {
                        new RecordDeserializationException(new TopicPartition(KafkaTestCluster.DEFAULT_TOPIC, 0), 12345,
                                                           "Bad record", null)
                }
        };
    }

    @Test(dataProvider = "kafka-poll-unrecoverable-errors", expectedExceptions = EventSourceException.class)
    public void kafka_poll_failures_unrecoverable_01(KafkaException e) {
        EventSource<Integer, String> source = createEmptySource();
        MockConsumer<Integer, String> mock = this.kafkaEventSource.getMockConsumer();
        mock.setPollException(e);
        source.poll(Duration.ofSeconds(3));
    }

    @Test(dataProvider = "kafka-poll-unrecoverable-errors-2", expectedExceptions = EventSourceException.class)
    public void kafka_poll_failures_unrecoverable_02(RuntimeException e) {
        EventSource<Integer, String> source = createEmptySource();
        MockConsumer<Integer, String> mock = this.kafkaEventSource.getMockConsumer();
        mock.schedulePollTask(() -> {throw e;});
        source.poll(Duration.ofSeconds(3));
    }

    @Test
    public void kafka_poll_failures_recoverable() {
        EventSource<Integer, String> source = createSource(createSampleData(10));
        MockConsumer<Integer, String> mock = this.kafkaEventSource.getMockConsumer();
        mock.wakeup();
        Assert.assertNull(source.poll(Duration.ofSeconds(3)));
    }
}
