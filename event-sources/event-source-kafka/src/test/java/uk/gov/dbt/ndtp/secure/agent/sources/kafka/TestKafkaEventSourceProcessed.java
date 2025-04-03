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
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.CollectorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events.EventProcessedSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;

public class TestKafkaEventSourceProcessed extends TestKafkaEventSource {
    @Override
    protected MockKafkaEventSource<Integer, String> createMockKafkaEventSource(
            Collection<Event<Integer, String>> events) {
        return new MockKafkaEventSource<>(DEFAULT_BOOTSTRAP_SERVERS, Set.of(TEST_TOPIC), TEST_GROUP,
                                          StringSerializer.class.getCanonicalName(),
                                          StringSerializer.class.getCanonicalName(), 100,
                                          KafkaReadPolicies.fromBeginning(), false, events);
    }

    @DataProvider(name = "batchSizes")
    private Object[][] processedBatchSizes() {
        return new Object[][] {
                { 10 },
                { 25 },
                { 50 },
                { 100 },
                { 200 },
                { 500 },
                { 1_000 },
                { 10_000 }
        };
    }

    @Test(dataProvider = "batchSizes")
    public void kafka_processed_callback_01(int batchSize) {
        EventSource<Integer, String> source = createSource(createSampleData(10_001));
        MockConsumer<Integer, String> mock = this.kafkaEventSource.getMockConsumer();
        TopicPartition partition = new TopicPartition(TEST_TOPIC, 0);
        Set<TopicPartition> partitions = Set.of(partition);
        verifyNoCommittedOffsets(mock, partitions);

        EventProcessedSink<Integer, String> sink = EventProcessedSink.<Integer, String>create()
                                                                     .batchSize(batchSize)
                                                                     .build();
        verifyProcessedIsCalled(source, mock, partition, partitions, sink, batchSize);

        // We're one event over the configured batch size so expect there still to be an incomplete batch
        // Remember that the commit always commits the next offset to be processed so this will be 1 higher than the
        // offset of the actual events marked as processed at this point
        // NB - Our offsets start from 0, so we've processed 0-9,999 at this point hence our next offset will be 10,000
        Assert.assertEquals(sink.incompleteBatches(), 1);
        Assert.assertEquals(sink.batchedEvents(), 1);
        verifyCommittedOffsets(mock, partition, partitions, 10_000);

        // Closing the sink should mark incomplete batches as processed
        // Again we always commit the next offset to be processed so this will be 1 higher than the total number of
        // events we sent
        // NB - Our offsets start from 0, so we've processed 0-10,000 at this point hence our next offset will be 10,001
        sink.close();
        Assert.assertEquals(sink.incompleteBatches(), 0);
        Assert.assertEquals(sink.batchedEvents(), 0);
        verifyCommittedOffsets(mock, partition, partitions, 10_001);
    }

    @Test(dataProvider = "batchSizes")
    public void kafka_processed_callback_01b(int batchSize) {
        EventSource<Integer, String> source = createSource(createSampleData(10_001));
        MockConsumer<Integer, String> mock = this.kafkaEventSource.getMockConsumer();
        TopicPartition partition = new TopicPartition(TEST_TOPIC, 0);
        Set<TopicPartition> partitions = Set.of(partition);
        verifyNoCommittedOffsets(mock, partitions);

        EventProcessedSink<Integer, String> sink = EventProcessedSink.<Integer, String>create()
                                                                     .batchSize(batchSize)
                                                                     .build();
        verifyProcessedIsCalled(source, mock, partition, partitions, sink, batchSize);

        // We're one event over the configured batch size so expect there still to be an incomplete batch
        // Remember that the commit always commits the next offset to be processed so this will be 1 higher than the
        // offset of the actual events marked as processed at this point
        Assert.assertEquals(sink.incompleteBatches(), 1);
        Assert.assertEquals(sink.batchedEvents(), 1);
        verifyCommittedOffsets(mock, partition, partitions, 10_000);

        // If we remove our assignments then our subsequent commits() will not be attempted
        mock.unsubscribe();

        // Closing the sink should mark incomplete batches as processed
        // However as we have just unsubscribed from the topics the event source won't commit offsets for a partition
        // it isn't currently assigned, and we'll see no committed offsets
        sink.close();
        Assert.assertEquals(sink.incompleteBatches(), 0);
        Assert.assertEquals(sink.batchedEvents(), 0);
        verifyNoCommittedOffsets(mock, partitions);
    }

    private static void verifyProcessedIsCalled(EventSource<Integer, String> source, MockConsumer<Integer, String> mock,
                                                TopicPartition partition, Set<TopicPartition> partitions,
                                                EventProcessedSink<Integer, String> sink, int batchSize) {
        Event<Integer, String> event;
        while ((event = source.poll(Duration.ofSeconds(1))) != null) {
            sink.send(event);

            if (event.key() > 0 && (event.key() + 1) % batchSize == 0) {
                // If we're at a batch boundary then expect processed to have been called and our committed offsets
                // to be updated
                verifyCommittedOffsets(mock, partition, partitions, event.key().longValue() + 1);
                Assert.assertEquals(sink.incompleteBatches(), 0);
                Assert.assertEquals(sink.batchedEvents(), 0);
            } else {
                // Otherwise expect an incomplete batch
                Assert.assertEquals(sink.incompleteBatches(), 1, "Expected incomplete batch for Key " + event.key());
                Assert.assertEquals(sink.batchedEvents(), (event.key() + 1) % batchSize);
            }
        }
    }

    private static void verifyCommittedOffsets(MockConsumer<Integer, String> mock, TopicPartition partition,
                                               Set<TopicPartition> partitions, long expected) {
        Map<TopicPartition, OffsetAndMetadata> committed;
        committed = mock.committed(partitions);
        Assert.assertFalse(committed.isEmpty());
        Assert.assertEquals(committed.getOrDefault(partition, new OffsetAndMetadata(0)).offset(), expected);
    }

    @Test
    public void kafka_processed_callback_02() {
        EventSource<Integer, String> source = createSource(createSampleData(10_001));
        MockConsumer<Integer, String> mock = this.kafkaEventSource.getMockConsumer();
        TopicPartition partition = new TopicPartition(TEST_TOPIC, 0);
        Set<TopicPartition> partitions = Set.of(partition);
        verifyNoCommittedOffsets(mock, partitions);

        try (NullSink<Event<Integer, String>> sink = NullSink.of()) {
            Event<Integer, String> event;
            while ((event = source.poll(Duration.ofSeconds(1))) != null) {
                sink.send(event);
            }
            Assert.assertEquals(sink.count(), 10_001);

            // As we are not using auto-commit and have not sent the events to a sink that reports them as processed we
            // don't expect to see anything committed
            verifyNoCommittedOffsets(mock, partitions);
        }
    }

    @Test
    public void kafka_processed_callback_03() {
        EventSource<Integer, String> source = createSource(createSampleData(10_001));
        MockConsumer<Integer, String> mock = this.kafkaEventSource.getMockConsumer();
        TopicPartition partition = new TopicPartition(TEST_TOPIC, 0);
        Set<TopicPartition> partitions = Set.of(partition);
        verifyNoCommittedOffsets(mock, partitions);

        try (CollectorSink<Event<Integer, String>> sink = CollectorSink.of()) {
            Event<Integer, String> event;
            while ((event = source.poll(Duration.ofSeconds(1))) != null) {
                sink.send(event);
            }
            Assert.assertEquals(sink.get().size(), 10_001);

            // As we are not using auto-commit and have not sent the events to a sink that reports them as processed we
            // don't expect to see anything committed
            verifyNoCommittedOffsets(mock, partitions);

            // If we explicitly call processed then we should see committed offsets
            source.processed(sink.get().stream().map(e -> (Event) e).toList());
            verifyCommittedOffsets(mock, partition, partitions, 10_001);
        }
    }

    private static void verifyNoCommittedOffsets(MockConsumer<Integer, String> mock, Set<TopicPartition> partitions) {
        Map<TopicPartition, OffsetAndMetadata> committed = mock.committed(partitions);
        Assert.assertTrue(committed.isEmpty());
    }
}
