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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.TimestampType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestKafkaEvents {

    public static final String TEST_TOPIC = "test";
    public static final int TEST_PARTITION = 1;
    public static final int TEST_OFFSET = 0;
    public static final int TEST_KEY = 1234;
    public static final String TEST_VALUE = "value";

    private <TKey, TValue> ConsumerRecord<TKey, TValue> createConsumerRecord(TKey key, TValue value) {
        return createConsumerRecord(key, value, new RecordHeaders());
    }

    private <TKey, TValue> ConsumerRecord<TKey, TValue> createConsumerRecord(TKey key, TValue value, Headers headers) {
        return new ConsumerRecord<>(TEST_TOPIC, TEST_PARTITION, TEST_OFFSET, RecordBatch.NO_TIMESTAMP,
                                    TimestampType.NO_TIMESTAMP_TYPE, ConsumerRecord.NULL_SIZE, ConsumerRecord.NULL_SIZE,
                                    key, value, headers, Optional.empty());
    }

    @Test
    public void kafka_event_01() {
        KafkaEvent<Integer, String> event = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE), null);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 0);
    }

    @Test
    public void kafka_event_02() {
        Headers headers = new RecordHeaders(new Header[] {
                new RecordHeader("Content-Type", "text/plain".getBytes(
                        StandardCharsets.UTF_8))
        });
        KafkaEvent<Integer, String> event = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE, headers), null);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 1);
        Assert.assertEquals(event.headers("Content-Type").count(), 1);
        Assert.assertEquals(event.headers("Content-Type").findFirst().orElse(null), "text/plain");

        List<uk.gov.dbt.ndtp.secure.agent.sources.Header> actualHeaders = event.headers().toList();
        Assert.assertEquals(actualHeaders.size(), 1);
        uk.gov.dbt.ndtp.secure.agent.sources.Header actual = actualHeaders.get(0);
        Assert.assertEquals(actual.key(), "Content-Type");
        Assert.assertEquals(actual.value(), "text/plain");
    }

    @Test
    public void kafka_event_03() {
        Headers headers = new RecordHeaders(new Header[] {
                new RecordHeader("Content-Type", "".getBytes(
                        StandardCharsets.UTF_8))
        });
        KafkaEvent<Integer, String> event = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE, headers), null);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 1);
        Assert.assertEquals(event.headers("Content-Type").count(), 1);
        Assert.assertEquals(event.headers("Content-Type").findFirst().orElse(null), "");

        List<uk.gov.dbt.ndtp.secure.agent.sources.Header> actualHeaders = event.headers().toList();
        Assert.assertEquals(actualHeaders.size(), 1);
        uk.gov.dbt.ndtp.secure.agent.sources.Header actual = actualHeaders.get(0);
        Assert.assertEquals(actual.key(), "Content-Type");
        Assert.assertEquals(actual.value(), "");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void kafka_event_04() {
        Headers headers = new RecordHeaders(new Header[] {
                new RecordHeader("Content-Type", null)
        });
        KafkaEvent<Integer, String> event = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE, headers), null);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 1);
        Assert.assertEquals(event.headers("Content-Type").count(), 1);
        try {
            event.headers("Content-Type").findFirst();
            Assert.fail("Failed to throw an NPE as expected");
        } catch (NullPointerException npe) {
            // Ok, we expect findFirst() to throw this if the first element is null
        }

        List<uk.gov.dbt.ndtp.secure.agent.sources.Header> actualHeaders = event.headers().toList();
        Assert.assertEquals(actualHeaders.size(), 1);
        uk.gov.dbt.ndtp.secure.agent.sources.Header actual = actualHeaders.get(0);
        Assert.assertEquals(actual.key(), "Content-Type");
        Assert.assertNull(actual.value());
    }

    @Test
    public void kafka_event_05() {
        Headers headers = new RecordHeaders(new Header[] {
                new RecordHeader("Content-Type", "text/plain".getBytes(StandardCharsets.UTF_8)),
                new RecordHeader("Exec-Path", "foo,bar".getBytes(StandardCharsets.UTF_8))
        });
        KafkaEvent<Integer, String> event = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE, headers), null);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 2);
        Assert.assertEquals(event.headers("Content-Type").count(), 1);
        Assert.assertEquals(event.headers("Exec-Path").count(), 1);
        Assert.assertEquals(event.headers("foo").count(), 0);
        Assert.assertEquals(event.headers("Content-Type").findFirst().orElse(null), "text/plain");
        Assert.assertEquals(event.headers("Exec-Path").findFirst().orElse(null), "foo,bar");

        List<uk.gov.dbt.ndtp.secure.agent.sources.Header> actualHeaders = event.headers().toList();
        Assert.assertEquals(actualHeaders.size(), 2);
        uk.gov.dbt.ndtp.secure.agent.sources.Header actual = actualHeaders.get(0);
        Assert.assertEquals(actual.key(), "Content-Type");
        Assert.assertEquals(actual.value(), "text/plain");
        actual = actualHeaders.get(1);
        Assert.assertEquals(actual.key(), "Exec-Path");
        Assert.assertEquals(actual.value(), "foo,bar");
    }

    @Test
    public void kafka_event_06() {
        Headers headers = new RecordHeaders(new Header[] {
                new RecordHeader("Content-Type", "application/json".getBytes(StandardCharsets.UTF_8)),
                new RecordHeader("Content-Type", "application/n-quads".getBytes(StandardCharsets.UTF_8))
        });
        KafkaEvent<Integer, String> event = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE, headers), null);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 2);
        Assert.assertEquals(event.headers("Content-Type").count(), 2);
        Assert.assertEquals(event.headers("foo").count(), 0);
        Assert.assertEquals(event.headers("Content-Type").findFirst().orElse(null), "application/json");
        Assert.assertEquals(event.lastHeader("Content-Type"), "application/n-quads");
        Assert.assertNull(event.lastHeader("foo"));
    }

    @SuppressWarnings("AssertBetweenInconvertibleTypes")
    @Test
    public void event_equality() {
        KafkaEvent<Integer, String> event = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE), null);
        Assert.assertEquals(event, event);
        Assert.assertNotEquals(event, null);
        Assert.assertNotEquals(null, event);
        Assert.assertFalse(event.equals(null));
        Assert.assertTrue(event.equals(event));
        Assert.assertFalse(event.equals("foo"));

        KafkaEvent<Integer, String> copy = new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE), null);
        Assert.assertEquals(event, copy);
        Assert.assertEquals(copy, event);
        Assert.assertTrue(event.equals(copy));
        Assert.assertTrue(copy.equals(event));

        KafkaEvent<Integer, String> differentValue = new KafkaEvent<>(createConsumerRecord(TEST_KEY, "other"), null);
        Assert.assertNotEquals(event, differentValue);
        Assert.assertNotEquals(differentValue, event);
        Assert.assertFalse(event.equals(differentValue));
        Assert.assertFalse(differentValue.equals(event));

        KafkaEvent<Integer, Double> differentTypes = new KafkaEvent<>(createConsumerRecord(123, 4.56), null);
        Assert.assertNotEquals(event, differentTypes);
        Assert.assertNotEquals(differentTypes, event);
        Assert.assertFalse(event.equals(differentTypes));
        Assert.assertFalse(differentTypes.equals(event));

        KafkaEvent<Integer, String> withHeaders =
                new KafkaEvent<>(createConsumerRecord(TEST_KEY, TEST_VALUE, new RecordHeaders(new Header[] {
                        new RecordHeader("Content-Type", "text/plain".getBytes(
                                StandardCharsets.UTF_8))
                })), null);
        Assert.assertNotEquals(event, withHeaders);
        Assert.assertFalse(event.equals(withHeaders));
        Assert.assertFalse(withHeaders.equals(event));

        KafkaEvent<Integer, String> withOtherHeaders =
                new KafkaEvent<>(
                        createConsumerRecord(TEST_KEY,
                                             TEST_VALUE, new RecordHeaders(new Header[] {
                                        new RecordHeader("Content-Type", "text/plain".getBytes(
                                                StandardCharsets.UTF_8)), new RecordHeader("foo", "bar".getBytes(
                                        StandardCharsets.UTF_8))
                                })), null);
        Assert.assertNotEquals(withHeaders, withOtherHeaders);
        Assert.assertFalse(withHeaders.equals(withOtherHeaders));
        Assert.assertFalse(withOtherHeaders.equals(withHeaders));

        KafkaEvent<Integer, String> withOtherHeadersDifferentOrder =
                new KafkaEvent<>(
                        createConsumerRecord(TEST_KEY,
                                             TEST_VALUE, new RecordHeaders(new Header[] {
                                        new RecordHeader("foo", "bar".getBytes(
                                                StandardCharsets.UTF_8)),
                                        new RecordHeader("Content-Type", "text/plain".getBytes(
                                                StandardCharsets.UTF_8))
                                })), null);
        Assert.assertEquals(withOtherHeaders, withOtherHeadersDifferentOrder);
        Assert.assertTrue(withOtherHeadersDifferentOrder.equals(withOtherHeaders));
        Assert.assertTrue(withOtherHeaders.equals(withOtherHeadersDifferentOrder));
    }
}
