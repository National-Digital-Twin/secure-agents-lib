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
package uk.gov.dbt.ndtp.secure.agent.sources.file.yaml;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventAccessMode;
import uk.gov.dbt.ndtp.secure.agent.sources.file.jackson.AbstractJacksonEventReaderWriter;

/**
 * A file event reader that encodes and decodes the events from YAML files
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class YamlEventReaderWriter<TKey, TValue> extends
        AbstractJacksonEventReaderWriter<TKey, TValue> {

    /**
     * Creates a new YAML Event reader writer
     *
     * @param mode              Event access mode
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     * @param keySerializer     Key serializer
     * @param valueSerializer   Value serializer
     */
    YamlEventReaderWriter(FileEventAccessMode mode, Deserializer<TKey> keyDeserializer,
                          Deserializer<TValue> valueDeserializer, Serializer<TKey> keySerializer,
                          Serializer<TValue> valueSerializer) {
        super(new YAMLMapper(), mode, keySerializer, valueSerializer, keyDeserializer,
              valueDeserializer);
    }

    /**
     * Creates a new YAML Event reader writer
     *
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     * @param keySerializer     Key serializer
     * @param valueSerializer   Value serializer
     */
    YamlEventReaderWriter(Deserializer<TKey> keyDeserializer, Deserializer<TValue> valueDeserializer,
                          Serializer<TKey> keySerializer,
                          Serializer<TValue> valueSerializer) {
        this(FileEventAccessMode.READ_WRITE, keyDeserializer, valueDeserializer, keySerializer, valueSerializer);
    }

    /**
     * Creates a new YAML Event reader
     *
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     */
    public YamlEventReaderWriter(Deserializer<TKey> keyDeserializer,
                                 Deserializer<TValue> valueDeserializer) {
        this(FileEventAccessMode.READ_ONLY, keyDeserializer, valueDeserializer, null, null);
    }

    /**
     * Creates a new YAML Event writer
     *
     * @param keySerializer   Key serializer
     * @param valueSerializer Value serializer
     */
    public YamlEventReaderWriter(Serializer<TKey> keySerializer,
                                 Serializer<TValue> valueSerializer) {
        this(FileEventAccessMode.WRITE_ONLY, null, null, keySerializer, valueSerializer);
    }

    /**
     * Creates a new builder
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     * @return Builder
     */
    public static <TKey, TValue> Builder<TKey, TValue> create() {
        return new Builder<>();
    }

    /**
     * Builder for YAML reader/writers
     *
     * @param <TKey>   Key type
     * @param <TValue> Value type
     */
    public static class Builder<TKey, TValue> {

        private Serializer<TKey> keySerializer;
        private Serializer<TValue> valueSerializer;
        private Deserializer<TKey> keyDeserializer;
        private Deserializer<TValue> valueDeserializer;

        /**
         * Sets the key serializer
         *
         * @param serializer Serializer
         * @return Builder
         */
        public Builder<TKey, TValue> keySerializer(Serializer<TKey> serializer) {
            this.keySerializer = serializer;
            return this;
        }

        /**
         * Sets the value serializer
         *
         * @param serializer Serializer
         * @return Builder
         */
        public Builder<TKey, TValue> valueSerializer(Serializer<TValue> serializer) {
            this.valueSerializer = serializer;
            return this;
        }

        /**
         * Sets the key deserializer
         *
         * @param deserializer Deserializer
         * @return Builder
         */
        public Builder<TKey, TValue> keyDeserializer(Deserializer<TKey> deserializer) {
            this.keyDeserializer = deserializer;
            return this;
        }

        /**
         * Sets the value deserializer
         *
         * @param deserializer Deserializer
         * @return Builder
         */
        public Builder<TKey, TValue> valueDeserializer(Deserializer<TValue> deserializer) {
            this.valueDeserializer = deserializer;
            return this;
        }

        /**
         * Builds a new YAML reader/writer
         *
         * @return YAML reader/writer
         */
        public YamlEventReaderWriter<TKey, TValue> build() {
            FileEventAccessMode mode = FileEventAccessMode.READ_WRITE;
            if (this.keySerializer == null && this.valueSerializer == null) {
                mode = FileEventAccessMode.READ_ONLY;
            } else if (this.keyDeserializer == null && this.valueDeserializer == null) {
                mode = FileEventAccessMode.WRITE_ONLY;
            }
            return new YamlEventReaderWriter<>(mode, this.keyDeserializer, this.valueDeserializer, this.keySerializer,
                                               this.valueSerializer
            );
        }
    }
}
