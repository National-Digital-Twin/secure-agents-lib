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
package uk.gov.dbt.ndtp.secure.agent.sources.file.rdf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.web.HttpNames;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventAccessMode;
import uk.gov.dbt.ndtp.secure.agent.sources.file.kafka.AbstractKafkaDelegatingEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

/**
 * An event reader/writer that expects raw RDF files using their file extensions to infer the {@code Content-Type}
 * header for the events.
 * <p>
 * Note that this format <strong>DOES NOT</strong> consider keys so the key for events will not round-trip and will
 * always be deserialized as {@code null}.
 * </p>
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class RdfEventReaderWriter<TKey, TValue> extends AbstractKafkaDelegatingEventReaderWriter<TKey, TValue> {

    /**
     * Creates a new RDF event reader/writer
     *
     * @param mode              File Event access mode
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     * @param keySerializer     Key serializer
     * @param valueSerializer   Value serializer
     */
    RdfEventReaderWriter(FileEventAccessMode mode, Deserializer<TKey> keyDeserializer,
                         Deserializer<TValue> valueDeserializer, Serializer<TKey> keySerializer,
                         Serializer<TValue> valueSerializer) {
        super(mode, keyDeserializer, valueDeserializer, keySerializer, valueSerializer);
    }

    /**
     * Creates a new RDF event reader/writer
     *
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     * @param keySerializer     Key serializer
     * @param valueSerializer   Value serializer
     */
    public RdfEventReaderWriter(Deserializer<TKey> keyDeserializer, Deserializer<TValue> valueDeserializer,
                                Serializer<TKey> keySerializer, Serializer<TValue> valueSerializer) {
        this(FileEventAccessMode.READ_WRITE, keyDeserializer, valueDeserializer, keySerializer, valueSerializer);
    }

    /**
     * Creates a new RDF event reader
     *
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     */
    public RdfEventReaderWriter(Deserializer<TKey> keyDeserializer, Deserializer<TValue> valueDeserializer) {
        this(FileEventAccessMode.READ_ONLY, keyDeserializer, valueDeserializer, null, null);
    }

    /**
     * Creates a new RDF event writer
     *
     * @param keySerializer   Key serializer
     * @param valueSerializer Value serializer
     */
    public RdfEventReaderWriter(Serializer<TKey> keySerializer, Serializer<TValue> valueSerializer) {
        this(FileEventAccessMode.WRITE_ONLY, null, null, keySerializer, valueSerializer);
    }

    @Override
    public Event<TKey, TValue> read(File f) throws IOException {
        Lang lang = RDFLanguages.filenameToLang(f.getAbsolutePath());

        // Infer Content-Type header from filename
        List<Header> headers = new ArrayList<>();
        if (lang != null) {
            headers.add(new Header(HttpNames.hContentType, lang.getContentType().getContentTypeStr()));
        }

        byte[] rawData = Files.readAllBytes(f.toPath());
        TValue value = this.valueDeserializer.deserialize(FAKE_TOPIC_FILE,
                                                          new RecordHeaders(KafkaSink.toKafkaHeaders(headers.stream())),
                                                          rawData);
        return new SimpleEvent<>(headers, null, value);
    }

    @Override
    public Event<TKey, TValue> read(InputStream input) {
        byte[] rawData = IO.readWholeFile(input);
        TValue value = this.valueDeserializer.deserialize(FAKE_TOPIC_FILE, rawData);
        return new SimpleEvent<>(Collections.emptyList(), null, value);
    }

    @Override
    public void write(Event<TKey, TValue> event, File f) throws IOException {
        Lang lang = RDFLanguages.filenameToLang(f.getAbsolutePath());

        List<Header> headers = new ArrayList<>(event.headers().toList());
        if (lang != null) {
            headers.add(new Header(HttpNames.hContentType, lang.getContentType().getContentTypeStr()));
        }

        byte[] rawData =
                this.valueSerializer.serialize(FAKE_TOPIC_FILE,
                                               new RecordHeaders(KafkaSink.toKafkaHeaders(headers.stream())),
                                               event.value());
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(f))) {
            output.write(rawData);
        }
    }

    @Override
    public void write(Event<TKey, TValue> event, OutputStream output) throws IOException {
        byte[] rawData =
                this.valueSerializer.serialize(FAKE_TOPIC_FILE, event.value());
        output.write(rawData);
    }
}
