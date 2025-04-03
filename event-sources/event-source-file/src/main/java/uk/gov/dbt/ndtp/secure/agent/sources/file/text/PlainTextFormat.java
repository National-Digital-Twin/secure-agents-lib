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
package uk.gov.dbt.ndtp.secure.agent.sources.file.text;

import java.io.File;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormatProvider;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventReader;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.SingleFileEventSource;

/**
 * Plain text event file format
 */
public class PlainTextFormat implements FileEventFormatProvider {

    /**
     * Name of the plain text format
     */
    public static final String EVENT_FORMAT_NAME = "text";

    @Override
    public String name() {
        return EVENT_FORMAT_NAME;
    }

    @Override
    public <TKey, TValue> FileEventReader<TKey, TValue> createReader(Deserializer<TKey> keyDeserializer,
                                                                     Deserializer<TValue> valueDeserializer) {
        return new PlainTextEventReaderWriter<>(valueDeserializer);
    }

    @Override
    public <TKey, TValue> FileEventWriter<TKey, TValue> createWriter(Serializer<TKey> keySerializer,
                                                                     Serializer<TValue> valueSerializer) {
        return new PlainTextEventReaderWriter<>(valueSerializer);
    }

    @Override
    public <TKey, TValue> FileEventReaderWriter<TKey, TValue> createReaderWriter(Deserializer<TKey> keyDeserializer,
                                                                                 Deserializer<TValue> valueDeserializer,
                                                                                 Serializer<TKey> keySerializer,
                                                                                 Serializer<TValue> valueSerializer) {
        return new PlainTextEventReaderWriter<>(valueDeserializer, valueSerializer);
    }

    @Override
    public <TKey, TValue> FileEventSource<TKey, TValue> createSource(Deserializer<TKey> keyDeserializer,
                                                                     Deserializer<TValue> valueDeserializer,
                                                                     File source) {
        return new PlainTextFileEventSource<>(source, valueDeserializer);
    }

    @Override
    public <TKey, TValue> FileEventSource<TKey, TValue> createSingleFileSource(Deserializer<TKey> keyDeserializer,
                                                                               Deserializer<TValue> valueDeserializer,
                                                                               File source) {
        return new SingleFileEventSource<>(source, new PlainTextEventReaderWriter<>(valueDeserializer));
    }

    @Override
    public String defaultFileExtension() {
        return ".txt";
    }
}
