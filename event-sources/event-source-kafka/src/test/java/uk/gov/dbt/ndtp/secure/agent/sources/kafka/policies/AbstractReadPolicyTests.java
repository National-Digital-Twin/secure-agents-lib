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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.mockito.internal.invocation.InvocationMarker;
import org.mockito.internal.invocation.InvocationsFinder;
import org.mockito.invocation.Invocation;
import org.mockito.verification.VerificationMode;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.TestKafkaEventSource;

public abstract class AbstractReadPolicyTests<TKey, TValue> {

    public static final String OTHER_TOPIC = "other";

    /**
     * Creates the read policy to be tested
     *
     * @return Read Policy
     */
    protected abstract KafkaReadPolicy<TKey, TValue> createPolicy();

    @Test
    public void read_policy_set_consumer_01() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        policy.setConsumer(consumer);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void read_policy_set_consumer_02() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        policy.setConsumer(consumer);
        // Can't set multiple times
        policy.setConsumer(consumer);
    }

    /**
     * Indicates whether the read policy will modify consumer configuration
     *
     * @return True if consumer configuration will be modified, false otherwise
     */
    protected abstract boolean modifiesConsumerConfiguration();

    @Test
    public void read_policy_prepare_consumer_01() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        Properties props = new Properties();
        policy.prepareConsumerConfiguration(props);

        if (modifiesConsumerConfiguration()) {
            Assert.assertNotEquals(props.size(), 0, "Expected the policy to add some consumer configuration");
        } else {
            Assert.assertEquals(props.size(), 0, "Expected the policy to leave consumer configuration unmodified");
        }
    }

    @Test
    public void read_policy_start_stop_events_01() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        mockConsumerGroup(consumer);
        TopicPartition partition = mockPartition(consumer, TestKafkaEventSource.TEST_TOPIC);
        mockSubscribeUnsubscribe(consumer);
        mockAssignment(consumer);
        policy.setConsumer(consumer);

        // Should be safe to call multiple times
        policy.startEvents(TestKafkaEventSource.TEST_TOPIC);
        policy.startEvents(TestKafkaEventSource.TEST_TOPIC);

        if (policy.isSubscriptionBased()) {
            verify(consumer, times(1)).subscribe(Collections.singletonList(TestKafkaEventSource.TEST_TOPIC), policy);
        } else {
            verify(consumer, atLeastOnce()).partitionsFor(TestKafkaEventSource.TEST_TOPIC);
            verify(consumer, times(1)).assign(Collections.singletonList(partition));
            if (seeksOnAssignment()) {
                verifySeek(consumer, atMostOnce());
            }
        }

        // Should be safe to call multiple times
        policy.stopEvents(TestKafkaEventSource.TEST_TOPIC);
        policy.stopEvents(TestKafkaEventSource.TEST_TOPIC);

        if (policy.isSubscriptionBased()) {
            verify(consumer, times(1)).unsubscribe();
        } else {
            verify(consumer, atLeastOnce()).assign(Collections.emptyList());
        }
    }

    private void mockSubscribeUnsubscribe(KafkaConsumer consumer) {
        Set<String> topics = new LinkedHashSet<>();
        doAnswer(invocation -> {
            topics.clear();
            topics.addAll(invocation.getArgument(0, Collection.class));
            return null;
        }).when(consumer).subscribe(any(Collection.class), any());
        doAnswer(invocation -> {
            topics.clear();
            return null;
        }).when(consumer).unsubscribe();
        when(consumer.subscription()).thenReturn(topics);
    }

    private void mockAssignment(KafkaConsumer consumer) {
        Set<TopicPartition> partitions = new LinkedHashSet<>();
        doAnswer(invocation -> {
            partitions.addAll(invocation.getArgument(0, Collection.class));
            return null;
        }).when(consumer).assign(any());
        when(consumer.assignment()).thenReturn(partitions);
    }

    private TopicPartition mockPartition(KafkaConsumer consumer, String topic) {
        TopicPartition partition = new TopicPartition(topic, 0);
        when(consumer.partitionsFor(topic)).thenReturn(asPartitionInfo(partition));
        return partition;
    }

    private void mockConsumerGroup(KafkaConsumer consumer) {
        ConsumerGroupMetadata group = mock(ConsumerGroupMetadata.class);
        when(group.groupId()).thenReturn(TestKafkaEventSource.TEST_GROUP);
        when(consumer.groupMetadata()).thenReturn(group);
    }

    @Test
    public void read_policy_start_stop_events_02() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        mockConsumerGroup(consumer);
        TopicPartition partition = mockPartition(consumer, TestKafkaEventSource.TEST_TOPIC);
        TopicPartition otherPartition = mockPartition(consumer, OTHER_TOPIC);
        mockSubscribeUnsubscribe(consumer);
        mockAssignment(consumer);
        policy.setConsumer(consumer);

        // Should be safe to call multiple times
        policy.startEvents(TestKafkaEventSource.TEST_TOPIC);
        policy.startEvents(TestKafkaEventSource.TEST_TOPIC);

        if (policy.isSubscriptionBased()) {
            verify(consumer, times(1)).subscribe(Collections.singletonList(TestKafkaEventSource.TEST_TOPIC), policy);
        } else {
            verify(consumer, atLeastOnce()).partitionsFor(TestKafkaEventSource.TEST_TOPIC);
            verify(consumer, times(1)).assign(Collections.singletonList(partition));
            if (seeksOnAssignment()) {
                verifySeek(consumer, atMostOnce());
            }
        }

        // Can also start events for multiple topics
        policy.startEvents(OTHER_TOPIC);

        if (policy.isSubscriptionBased()) {
            verify(consumer, atMostOnce()).subscribe(Arrays.asList(TestKafkaEventSource.TEST_TOPIC, OTHER_TOPIC),
                                                     policy);
        } else {
            verify(consumer, atLeastOnce()).partitionsFor(OTHER_TOPIC);
            verify(consumer, atMostOnce()).assign(Collections.singletonList(otherPartition));
            if (seeksOnAssignment()) {
                verifySeek(consumer, either(0, 2));
            }
        }

        // Should be safe to call multiple times
        policy.stopEvents(TestKafkaEventSource.TEST_TOPIC);
        policy.stopEvents(TestKafkaEventSource.TEST_TOPIC);

        if (policy.isSubscriptionBased()) {
            verify(consumer, atMostOnce()).subscribe(Collections.singletonList(OTHER_TOPIC), policy);
        } else {
            verify(consumer, atMostOnce()).assign(Collections.singletonList(OTHER_TOPIC));
        }

        // A full unsubscribe only happens when all topics are stopped
        policy.stopEvents(OTHER_TOPIC);
        if (policy.isSubscriptionBased()) {
            verify(consumer, times(1)).unsubscribe();
        } else {
            verify(consumer, atMostOnce()).assign(Collections.emptyList());
        }
    }

    @Test
    public void read_policy_start_stop_events_03() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        mockConsumerGroup(consumer);
        mockSubscribeUnsubscribe(consumer);
        mockAssignment(consumer);
        policy.setConsumer(consumer);

        policy.startEvents("foo");

        if (policy.isSubscriptionBased()) {
            verify(consumer, times(1)).subscribe(Collections.singletonList("foo"), policy);
        } else {
            verify(consumer, atLeastOnce()).partitionsFor("foo");
            verify(consumer, never()).assign(any());
        }
    }

    @Test
    public void read_policy_start_stop_events_04() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        mockConsumerGroup(consumer);
        mockSubscribeUnsubscribe(consumer);
        mockAssignment(consumer);
        policy.setConsumer(consumer);

        policy.stopEvents("foo");

        if (policy.isSubscriptionBased()) {
            verify(consumer, never()).unsubscribe();
        } else {
            verify(consumer, atLeastOnce()).assignment();
            verify(consumer, never()).assign(any());
        }
    }

    private VerificationMode either(int x, int y) {
        return data -> {
            List<Invocation> invokes = InvocationsFinder.findInvocations(data.getAllInvocations(), data.getTarget());
            int actual = invokes.size();
            if (actual == x || actual == y) {
                InvocationMarker.markVerified(invokes, data.getTarget());
            } else {
                throw new MockitoAssertionError(String.format("Wanted %d or %d invocations but got %d", x, y, actual));
            }
        };
    }

    private List<PartitionInfo> asPartitionInfo(TopicPartition partition) {
        Node n = mock(Node.class);
        Node[] ns = new Node[] { n };
        return Collections.singletonList(new PartitionInfo(partition.topic(), partition.partition(), n, ns, ns));
    }

    protected abstract boolean seeksOnAssignment();

    @Test
    public void read_policy_rebalance_listener_01() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        policy.setConsumer(consumer);

        policy.onPartitionsAssigned(Collections.singletonList(new TopicPartition(TestKafkaEventSource.TEST_TOPIC, 0)));

        if (seeksOnAssignment()) {
            verifySeek(consumer, atMostOnce());
        } else {
            verifySeek(consumer, never());
        }
    }

    @Test
    public void read_policy_rebalance_listener_02() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        policy.setConsumer(consumer);

        policy.onPartitionsRevoked(Collections.singletonList(new TopicPartition(TestKafkaEventSource.TEST_TOPIC, 0)));
    }

    @Test
    public void read_policy_rebalance_listener_03() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        policy.setConsumer(consumer);

        policy.onPartitionsLost(Collections.singletonList(new TopicPartition(TestKafkaEventSource.TEST_TOPIC, 0)));
    }

    @Test
    public void read_policy_rebalance_listener_04() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        policy.setConsumer(consumer);

        List<TopicPartition> partitions =
                Collections.singletonList(new TopicPartition(TestKafkaEventSource.TEST_TOPIC, 0));
        policy.onPartitionsAssigned(partitions);

        if (seeksOnAssignment()) {
            verifySeek(consumer, atMostOnce());

            // Assigning the same partitions again MUST NOT seek again
            policy.onPartitionsAssigned(partitions);
            verifySeek(consumer, atMostOnce());

            // Assigning different partitions MUST seek
            policy.onPartitionsAssigned(List.of(new TopicPartition(TestKafkaEventSource.TEST_TOPIC, 1)));
            verifySeek(consumer, atMost(2));
        } else {
            verifySeek(consumer, never());
        }
    }

    private void verifySeek(KafkaConsumer consumer, VerificationMode verificationMode) {
        verify(consumer, verificationMode).seek(any(), any());
        verify(consumer, verificationMode).seek(any(), anyLong());
        verify(consumer, verificationMode).seekToBeginning(any());
        verify(consumer, verificationMode).seekToEnd(any());
    }

    @Test
    public void read_policy_remaining_01() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        policy.setConsumer(consumer);

        Long remaining = policy.currentLag("test");
        Assert.assertNull(remaining);
    }

    @Test
    public void read_policy_remaining_02() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        when(consumer.currentLag(any())).thenReturn(OptionalLong.of(100));
        when(consumer.assignment()).thenReturn(Set.of(new TopicPartition("test", 0)));
        policy.setConsumer(consumer);

        Long remaining = policy.currentLag("test");
        Assert.assertEquals(remaining, 100L);
    }

    @Test
    public void read_policy_remaining_03() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        when(consumer.currentLag(any())).thenReturn(OptionalLong.of(100));
        when(consumer.assignment()).thenReturn(Set.of(new TopicPartition("test", 0), new TopicPartition("test", 1)));
        policy.setConsumer(consumer);

        Long remaining = policy.currentLag("test");
        Assert.assertEquals(remaining, 200L);
    }

    @Test
    public void read_policy_remaining_04() {
        KafkaReadPolicy<TKey, TValue> policy = createPolicy();
        KafkaConsumer consumer = mock(KafkaConsumer.class);
        when(consumer.currentLag(eq(new TopicPartition("test", 0)))).thenReturn(OptionalLong.of(100));
        when(consumer.currentLag(eq(new TopicPartition("test", 1)))).thenReturn(OptionalLong.empty());
        when(consumer.assignment()).thenReturn(Set.of(new TopicPartition("test", 0), new TopicPartition("test", 1)));
        policy.setConsumer(consumer);

        Long remaining = policy.currentLag("test");
        Assert.assertNull(remaining);
    }
}
