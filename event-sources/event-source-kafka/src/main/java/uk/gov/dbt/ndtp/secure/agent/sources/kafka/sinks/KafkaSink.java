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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import lombok.NonNull;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaSecurity;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sink that sends the received events to a Kafka topic
 * <p>
 * This uses a {@link KafkaProducer} internally so all the sent events are sent asynchronously.
 * </p>
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class KafkaSink<TKey, TValue> implements Sink<Event<TKey, TValue>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSink.class);

    private final KafkaProducer<TKey, TValue> producer;
    private final String topic;
    private final boolean async;
    private final Callback callback;
    private final List<Exception> producerErrors = new ArrayList<>();

    /**
     * Creates a new Kafka sink
     *
     * @param bootstrapServers     Bootstrap servers for connecting to the Kafka cluster
     * @param topic                Kafka topic to write events to
     * @param keySerializerClass   Serializer to use for event keys
     * @param valueSerializerClass Serializer to use for event values
     * @param lingerMilliseconds   Linger milliseconds, reduces the number of requests made to Kafka by batching events
     *                             together at the cost of event sending latency
     */
    KafkaSink(final String bootstrapServers, final String topic, final String keySerializerClass,
              final String valueSerializerClass, final Integer lingerMilliseconds, final boolean async,
              final Callback callback, Properties producerProperties) {
        if (StringUtils.isBlank(bootstrapServers)) {
            throw new IllegalArgumentException("Kafka bootstrapServers cannot be null");
        }
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Kafka topic to read cannot be null");
        }
        if (StringUtils.isBlank(keySerializerClass)) {
            throw new IllegalArgumentException("Kafka keySerializerClass cannot be null");
        }
        if (StringUtils.isBlank(valueSerializerClass)) {
            throw new IllegalArgumentException("Kafka valueSerializerClass cannot be null");
        }
        this.topic = topic;

        Properties props = new Properties();
        if (producerProperties != null) {
            props.putAll(producerProperties);
        }
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);
        if (lingerMilliseconds != null && lingerMilliseconds > 0) {
            if (!async) {
                LOGGER.warn("Kafka Sink created with synchronous sends so linger milliseconds is ignored");
            } else {
                props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMilliseconds);
            }
        }
        this.producer = new KafkaProducer<>(props);

        this.async = async;
        this.callback = this.async ? Objects.requireNonNullElse(callback, new CompletionHandler(this)) : null;
    }

    @Override
    public void send(Event<TKey, TValue> event) {
        Objects.requireNonNull(event, "Event cannot be null");
        ProducerRecord<TKey, TValue> record = new ProducerRecord<>(this.topic, null, null, event.key(), event.value(),
                                                                   toKafkaHeaders(event.headers()));
        if (this.async) {
            asynchronousSend(record);
        } else {
            synchronousSend(record);
        }
    }

    /**
     * Sends the prepared {@link ProducerRecord} asynchronously to Kafka.
     * <p>
     * Note that the error handling happens asynchronously as a send may not immediately fail, e.g. Kafka may be
     * retrying, so we may only see the error later.  A check for any previously received async errors is made as part
     * after the send and will produce a {@link SinkException} if any async errors have been received.
     * </p>
     *
     * @param record Producer Record
     */
    protected final void asynchronousSend(ProducerRecord<TKey, TValue> record) {
        // Asynchronous send, just send the record and use the callback to handle any issues
        this.producer.send(record, this.callback);

        // However immediately check for any async errors as we may only now be seeing errors from previous send
        // attempts
        this.checkForAsyncErrors();
    }

    /**
     * Sends the prepared {@link ProducerRecord} asynchronously to Kafka.
     * <p>
     * This waits for the producer to acknowledge the send and fails ASAP if this is not the case.  Note that for some
     * Kafka errors the producer may internally retry the send multiple times before giving up so the failure may not be
     * immediate and blocking may occur for a prolonged period.
     * </p>
     *
     * @param record Producer Record
     */
    protected final void synchronousSend(ProducerRecord<TKey, TValue> record) {
        // Synchronous send, send the message and wait for confirmation it was produced
        Future<RecordMetadata> future = this.producer.send(record);
        try {
            RecordMetadata metadata = future.get();
            if (metadata == null) {
                throw new SinkException("Kafka Producer returned null metadata for event");
            }
        } catch (Throwable e) {
            // Any send error we handle via throwing an error
            throw new SinkException("Failed to send event to Kafka, see cause for details", e);
        }
    }

    /**
     * Maps headers using our Event API into the Header format used by Kafka
     *
     * @param headers Event headers
     * @return Kafka headers
     */
    public static List<org.apache.kafka.common.header.Header> toKafkaHeaders(Stream<Header> headers) {
        return headers.map(h -> (org.apache.kafka.common.header.Header) new RecordHeader(h.key(), h.value()
                                                                                                   .getBytes(
                                                                                                           StandardCharsets.UTF_8)))
                      .toList();
    }


    @Override
    public void close() {
        this.producer.close();

        checkForAsyncErrors();
    }

    /**
     * Checks for any asynchronous errors that have been received when the sink is used in asynchronous sending mode
     * (the default)
     */
    protected final void checkForAsyncErrors() {
        synchronized (this.producerErrors) {
            if (!this.producerErrors.isEmpty()) {
                SinkException e = new SinkException(
                        "Received " + this.producerErrors.size() + " async producer errors from Kafka, see suppressed errors for details");
                for (Exception producerError : this.producerErrors) {
                    e.addSuppressed(producerError);
                }
                this.producerErrors.clear();
                throw e;
            }
        }
    }

    /**
     * Gets the metrics of the underlying {@link KafkaProducer}
     *
     * @return Kafka Producer Metrics
     *
     * Warning suppressed due to implementation in lib
     * kafka-clients.KafkaProducer returns the same generic type as the method signature
     */
    @SuppressWarnings("squid:S1452")
    public Map<MetricName, ? extends Metric> metrics() {
        return this.producer.metrics();
    }

    /**
     * A Kafka Producer callback that merely captures the async errors (if any) in the parent sink's
     * {@link #producerErrors} collection, these errors will be thrown at a later point when {@link #send(Event)} or
     * {@link #close()} are being called.
     *
     * @param sink Parent sink
     */
    private record CompletionHandler(@NonNull KafkaSink<?, ?> sink) implements Callback {

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception != null) {
                synchronized (this.sink.producerErrors) {
                    sink.producerErrors.add(exception);
                }
            }
        }
    }

    /**
     * Creates a new builder for Kafka Sinks
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     * @return Builder
     */
    public static <TKey, TValue> KafkaSinkBuilder<TKey, TValue> create() {
        return new KafkaSinkBuilder<>();
    }

    /**
     * A builder for Kafka sinks
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     */
    public static final class KafkaSinkBuilder<TKey, TValue>
            implements SinkBuilder<Event<TKey, TValue>, KafkaSink<TKey, TValue>> {

        private String bootstrapServers, topic, keySerializerClass, valueSerializerClass;
        private Integer lingerMs;
        private final Properties properties = new Properties();
        private boolean async = true;
        private Callback callback;

        /**
         * Sets the bootstrap servers
         *
         * @param servers Servers
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> bootstrapServers(String servers) {
            this.bootstrapServers = servers;
            return this;
        }

        /**
         * Sets the bootstrap servers
         *
         * @param servers Servers
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> bootstrapServers(String... servers) {
            return bootstrapServers(StringUtils.join(servers, ","));
        }

        /**
         * Sets the topic to be written to
         *
         * @param topic Topic
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> topic(String topic) {
            this.topic = topic;
            return this;
        }

        /**
         * Sets the key serializer class
         *
         * @param keySerializerClass Key serializer class
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> keySerializer(String keySerializerClass) {
            this.keySerializerClass = keySerializerClass;
            return this;
        }

        /**
         * Sets the value serializer class
         *
         * @param valueSerializerClass Value serializer class
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> valueSerializer(String valueSerializerClass) {
            this.valueSerializerClass = valueSerializerClass;
            return this;
        }

        /**
         * Sets the key serializer
         *
         * @param cls Class
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> keySerializer(Class<?> cls) {
            Objects.requireNonNull(cls, "Class cannot be null");
            return keySerializer(cls.getCanonicalName());
        }

        /**
         * Sets the value serializer
         *
         * @param cls Class
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> valueSerializer(Class<?> cls) {
            Objects.requireNonNull(cls, "Class cannot be null");
            return valueSerializer(cls.getCanonicalName());
        }

        /**
         * Sets linger milliseconds for the sink
         *
         * @param milliseconds Milliseconds
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> lingerMs(int milliseconds) {
            this.lingerMs = milliseconds;
            return this;
        }

        /**
         * Disables linger for the sink
         *
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> noLinger() {
            this.lingerMs = null;
            return this;
        }

        /**
         * Configures the sink to use asynchronous sends i.e. events are sent to Kafka asynchronously and any errors
         * that occur may be thrown on later {@link #send(Event)}/{@link #close()} calls.
         * <p>
         * This is the default behaviour so need not be specified explicitly.
         * </p>
         *
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> async() {
            this.async = true;
            this.callback = null;
            return this;
        }

        /**
         * Configures the sink to use synchronous sends i.e. events are sent to Kafka synchronously and the sink either
         * {@link #send(Event)} call either succeeds or throws an error.
         * <p>
         * Generally this should only be used in test/development as synchronous sends incur a significant performance
         * penalty.
         * </p>
         *
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> noAsync() {
            this.async = false;
            return this;
        }

        /**
         * Configures the sink to use asynchronous sends with a custom callback
         * <p>
         * As opposed to calling just {@link #async()} which configures our own internal callback by default, this
         * allows the caller to configure the sink with a custom callback function so the caller retains full control
         * over error handling.
         * </p>
         * <p>
         * Therefore when the sink is configured in this way {@link #send(Event)} will always succeed immediately once
         * it has handed the event off to Kafka for async sending.  This differs from the default behaviour described on
         * {@link #async()} so please ensure you understand the difference before using this method.
         * </p>
         *
         * @param callback Kafka Producer Callback
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> async(Callback callback) {
            this.callback = Objects.requireNonNull(callback,
                                                   "Callback cannot be null, use the no argument async() method if you want KafkaSink to handle async callbacks for you");
            this.async = true;
            return this;
        }

        /**
         * Sets a Kafka Producer configuration property that will be used to configure the underlying
         * {@link KafkaProducer}.  Note that some properties are always overridden by the other sink configuration
         * provided to this builder.
         *
         * @param name  Name
         * @param value Value
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> producerConfig(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }

        /**
         * Sets multiple Kafka Producer configuration properties that will be used to configure the underlying
         * {@link KafkaProducer}. Note that some properties are always overridden by the other sink configuration
         * provided to this builder.
         *
         * @param properties Producer configuration properties
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> producerConfig(Properties properties) {
            this.properties.putAll(properties);
            return this;
        }

        /**
         * Configures the sink to perform a plain SASL login to the Kafka cluster using the provided credentials
         * <p>
         * If an alternative authentication mechanism is needed use {@link #producerConfig(Properties)}} to supply the
         * necessary configuration properties with suitable values.
         * </p>
         *
         * @param username Username
         * @param password Password
         * @return Builder
         */
        public KafkaSinkBuilder<TKey, TValue> plainLogin(String username, String password) {
            this.properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            this.properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name);
            this.properties.put(SaslConfigs.SASL_JAAS_CONFIG, KafkaSecurity.plainLogin(username, password));
            return this;
        }

        @Override
        public KafkaSink<TKey, TValue> build() {
            return new KafkaSink<>(this.bootstrapServers, this.topic, this.keySerializerClass,
                                   this.valueSerializerClass, this.lingerMs, this.async, this.callback,
                                   this.properties);
        }
    }
}
