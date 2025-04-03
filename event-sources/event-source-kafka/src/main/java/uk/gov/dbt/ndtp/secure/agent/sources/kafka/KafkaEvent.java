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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

/**
 * Represents an event backed by a Kafka {@link ConsumerRecord}
 *
 * @param <TKey>   Event key type
 * @param <TValue> Event value type
 */
public class KafkaEvent<TKey, TValue> implements Event<TKey, TValue> {

    private final ConsumerRecord<TKey, TValue> record;
    private final KafkaEventSource source;

    /**
     * Creates a new event from a consumer record
     *
     * @param record Consumer record
     * @param source Kafka Event source
     */
    public KafkaEvent(ConsumerRecord<TKey, TValue> record, KafkaEventSource source) {
        Objects.requireNonNull(record, "Record cannot be null");
        this.record = record;
        this.source = source;
    }

    private String decodeValue(org.apache.kafka.common.header.Header h) {
        return h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    @Override
    public Stream<Header> headers() {
        return Arrays.stream(this.record.headers().toArray()).map(h -> new Header(h.key(), decodeValue(h)));
    }

    @Override
    public Stream<String> headers(String key) {
        return StreamSupport.stream(record.headers().headers(key).spliterator(), false).map(this::decodeValue);
    }

    @Override
    public String lastHeader(String key) {
        List<String> values = this.headers(key).collect(Collectors.toList());
        return CollectionUtils.isEmpty(values) ? null : values.get(values.size() - 1);
    }

    @Override
    public TKey key() {
        return this.record.key();
    }

    @Override
    public TValue value() {
        return this.record.value();
    }

    @Override
    public <TNewKey> Event<TNewKey, TValue> replaceKey(TNewKey newKey) {
        return new KafkaEvent<>(new ConsumerRecord<>(this.record.topic(), this.record.partition(), this.record.offset(),
                                                     this.record.timestamp(), this.record.timestampType(), -1,
                                                     this.record.serializedValueSize(), newKey, this.record.value(),
                                                     this.record.headers(), this.record.leaderEpoch()), this.source);
    }

    @Override
    public <TNewValue> Event<TKey, TNewValue> replaceValue(TNewValue newValue) {
        return new KafkaEvent<>(new ConsumerRecord<>(this.record.topic(), this.record.partition(), this.record.offset(),
                                                     this.record.timestamp(), this.record.timestampType(),
                                                     this.record.serializedKeySize(), -1, this.record.key(), newValue,
                                                     this.record.headers(), this.record.leaderEpoch()), this.source);
    }

    @Override
    public <TNewKey, TNewValue> Event<TNewKey, TNewValue> replace(TNewKey newKey, TNewValue newValue) {
        return new KafkaEvent<>(new ConsumerRecord<>(this.record.topic(), this.record.partition(), this.record.offset(),
                                                     this.record.timestamp(), this.record.timestampType(), -1, -1,
                                                     newKey, newValue, this.record.headers(),
                                                     this.record.leaderEpoch()), this.source);
    }

    @Override
    public Event<TKey, TValue> replaceHeaders(Stream<Header> headers) {
        return new KafkaEvent<>(new ConsumerRecord<>(this.record.topic(), this.record.partition(), this.record.offset(),
                                                     this.record.timestamp(), this.record.timestampType(),
                                                     this.record.serializedKeySize(), this.record.serializedValueSize(),
                                                     this.key(), this.value(),
                                                     new RecordHeaders(KafkaSink.toKafkaHeaders(headers)),
                                                     this.record.leaderEpoch()), this.source);
    }

    @Override
    public Event<TKey, TValue> addHeaders(Stream<Header> headers) {
        RecordHeaders newHeaders = new RecordHeaders(this.record.headers());
        headers.forEach(h -> newHeaders.add(new RecordHeader(h.key(), h.value().getBytes(StandardCharsets.UTF_8))));
        return new KafkaEvent<>(new ConsumerRecord<>(this.record.topic(), this.record.partition(), this.record.offset(),
                                                     this.record.timestamp(), this.record.timestampType(),
                                                     this.record.serializedKeySize(), this.record.serializedValueSize(),
                                                     this.key(), this.value(), newHeaders, this.record.leaderEpoch()),
                                this.source);
    }

    @Override
    public EventSource source() {
        return this.source;
    }

    /**
     * Gets the underlying Kafka {@link ConsumerRecord} instance
     * <p>
     * Package private so that only {@link KafkaEventSource} can determine the offsets to commit when
     * {@link EventSource#processed(Collection)} is called.
     * </p>
     *
     * @return Consumer Record
     */
    public ConsumerRecord<TKey, TValue> getConsumerRecord() {
        return this.record;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Event<?, ?> other)) {
            return false;
        }

        if (!SetUtils.isEqualSet(this.headers().collect(Collectors.toList()),
                                 other.headers().collect(Collectors.toList()))) {
            return false;
        }

        if (!Objects.equals(this.key(), other.key())) {
            return false;
        }
        if (!Objects.equals(this.value(), other.value())) {
            return false;
        }

        return true;
    }
}
