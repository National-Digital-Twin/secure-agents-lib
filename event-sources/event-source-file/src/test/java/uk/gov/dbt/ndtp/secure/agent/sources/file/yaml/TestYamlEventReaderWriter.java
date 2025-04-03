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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.Serdes;
import uk.gov.dbt.ndtp.secure.agent.sources.file.gzip.GZipEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestYamlEventReaderWriter {

    @Test
    public void yaml_event_read_01() throws IOException {
        YamlEventReaderWriter<Integer, String> reader = Serdes.YAML_INTEGER_STRING;
        Event<Integer, String> event = reader.read(new File("test-data/event.yaml"));

        Assert.assertEquals(event.key(), 123456789);
        Assert.assertNotNull(event.value());

        verifyRoundTrip(reader, event);
    }

    @Test
    public void yaml_event_read_02() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        Event<String, String> event = reader.read(new File("test-data/event.yaml"));

        Assert.assertNotEquals(event.key(), Integer.toString(123456789));
        Assert.assertNotNull(event.value());

        verifyRoundTrip(reader, event);
    }

    @Test
    public void yaml_event_read_03() throws IOException {
        YamlEventReaderWriter<Header, Map> reader =
                new YamlEventReaderWriter<>(Serdes.HEADER_DESERIALIZER, Serdes.MAP_DESERIALIZER,
                                            Serdes.HEADER_SERIALIZER, Serdes.MAP_SERIALIZER
                );
        Event<Header, Map> event = reader.read(new File("test-data/complex-event.yaml"));

        Assert.assertEquals(event.key(), new Header("Key", "Value"));
        Assert.assertNotNull(event.value());
        Map<String, Object> value = event.value();
        Assert.assertFalse(value.isEmpty());
        Assert.assertEquals(value.keySet().size(), 5);

        verifyRoundTrip(reader, event);
    }

    @Test
    public void yaml_event_read_04() throws IOException {
        YamlEventReaderWriter<Integer, String> reader = Serdes.YAML_INTEGER_STRING;
        Event<Integer, String> event = reader.read(new File("test-data/no-value.yaml"));

        Assert.assertEquals(event.key(), 123456789);
        Assert.assertNull(event.value());

        verifyRoundTrip(reader, event);
    }

    @Test
    public void yaml_event_read_05() throws IOException {
        YamlEventReaderWriter<Integer, String> reader = Serdes.YAML_INTEGER_STRING;
        Event<Integer, String> event = reader.read(new File("test-data/no-key.yaml"));

        Assert.assertNull(event.key());
        Assert.assertNotNull(event.value());

        verifyRoundTrip(reader, event);
    }

    @DataProvider(name = "rdfEvents")
    public Object[][] rdfEvents() {
        File base = new File("test-data");
        return new Object[][] {
                { new File(base, "event.yaml") },
                { new File(base, "rdf/rdf1.yaml") },
                { new File(base, "rdf/rdf2.yaml") },
                { new File(base, "rdf/rdf3.yaml") },
                { new File(base, "rdf/rdf4.yaml") },
                };
    }

    @Test(dataProvider = "rdfEvents")
    public void yaml_event_read_06(File input) throws IOException {
        YamlEventReaderWriter<Integer, Graph> reader =
                new YamlEventReaderWriter<>(Serdes.INTEGER_DESERIALIZER, Serdes.GRAPH_DESERIALIZER,
                                            Serdes.INTEGER_SERIALIZER, Serdes.GRAPH_SERIALIZER
                );
        Event<Integer, Graph> event = reader.read(input);

        Assert.assertNotNull(event.value());
        Assert.assertEquals(event.value().size(), 2);

        verifyRoundTrip(reader, event);
    }

    @Test(dataProvider = "rdfEvents")
    public void yaml_event_read_07(File input) throws IOException {
        YamlEventReaderWriter<Integer, DatasetGraph> reader =
                new YamlEventReaderWriter<>(Serdes.INTEGER_DESERIALIZER, Serdes.DATASET_GRAPH_DESERIALIZER,
                                            Serdes.INTEGER_SERIALIZER, Serdes.DATASET_GRAPH_SERIALIZER
                );
        Event<Integer, DatasetGraph> event = reader.read(input);

        Assert.assertNotNull(event.value());
        Assert.assertEquals(event.value().size(), 0);
        Assert.assertEquals(event.value().stream().count(), 2);

        verifyRoundTrip(reader, event);
    }

    @Test(dataProvider = "rdfEvents")
    public void yaml_event_read_08(File input) throws IOException {
        YamlEventReaderWriter<Integer, DatasetGraph> reader =
                new YamlEventReaderWriter<>(Serdes.INTEGER_DESERIALIZER, Serdes.DATASET_GRAPH_DESERIALIZER,
                                            Serdes.INTEGER_SERIALIZER, Serdes.DATASET_GRAPH_SERIALIZER
                );
        Event<Integer, DatasetGraph> event = reader.read(input);

        Event<DatasetGraph, Integer> swappedEvent = event.replace(event.value(), event.key());
        YamlEventReaderWriter<DatasetGraph, Integer> swappedReader =
                new YamlEventReaderWriter<>(Serdes.DATASET_GRAPH_DESERIALIZER, Serdes.INTEGER_DESERIALIZER,
                                            Serdes.DATASET_GRAPH_SERIALIZER, Serdes.INTEGER_SERIALIZER
                );
        verifyRoundTrip(swappedReader, swappedEvent);
    }

    @Test(dataProvider = "rdfEvents")
    public void yaml_event_read_09(File input) throws IOException {
        FileEventReaderWriter<Integer, DatasetGraph> reader = new GZipEventReaderWriter<>(
                new YamlEventReaderWriter<>(Serdes.INTEGER_DESERIALIZER, Serdes.DATASET_GRAPH_DESERIALIZER,
                                            Serdes.INTEGER_SERIALIZER, Serdes.DATASET_GRAPH_SERIALIZER
                ));
        Event<Integer, DatasetGraph> event = reader.read(new File(input.getParentFile(), input.getName() + ".gz"));

        Assert.assertNotNull(event.value());
        Assert.assertEquals(event.value().size(), 0);
        Assert.assertEquals(event.value().stream().count(), 2);

        verifyRoundTrip(reader, event);
    }

    @Test(dataProvider = "rdfEvents")
    public void yaml_event_read_10(File input) throws IOException {
        FileEventReaderWriter<Integer, DatasetGraph> reader = new GZipEventReaderWriter<>(
                new YamlEventReaderWriter<>(Serdes.INTEGER_DESERIALIZER, Serdes.DATASET_GRAPH_DESERIALIZER,
                                            Serdes.INTEGER_SERIALIZER, Serdes.DATASET_GRAPH_SERIALIZER
                ));
        try (InputStream inputStream = new FileInputStream(new File(input.getParentFile(), input.getName() + ".gz"))) {
            Event<Integer, DatasetGraph> event = reader.read(inputStream);

            Assert.assertNotNull(event.value());
            Assert.assertEquals(event.value().size(), 0);
            Assert.assertEquals(event.value().stream().count(), 2);

            verifyRoundTrip(reader, event);

            File tempOutput = new File("target/" + input.getName() + ".gz");
            tempOutput.deleteOnExit();
            try (FileOutputStream outputStream = new FileOutputStream(tempOutput)) {
                reader.write(event, outputStream);

                Event<Integer, DatasetGraph> retrieved = reader.read(tempOutput);
                verifySameEvent(event, retrieved);
            }
        }
    }

    @Test
    public void yaml_event_read_11() throws IOException {
        YamlEventReaderWriter<Bytes, String> reader = Serdes.YAML_BYTES_STRING;
        Event<Bytes, String> event = reader.read(new File("test-data/kafka/bytes1.yaml"));

        Assert.assertNull(event.key());
        Assert.assertEquals(event.value(), "test");
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Expected START_ARRAY.*")
    public void yaml_event_read_bad_01() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed-headers.yaml"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Expected VALUE_STRING but got START_OBJECT .*")
    public void yaml_event_read_bad_02() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed-key.yaml"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Both key and value cannot be null")
    public void yaml_event_read_bad_03() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed-key2.yaml"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Expected VALUE_STRING but got START_OBJECT.*")
    public void yaml_event_read_bad_04() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed-value.yaml"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Expected one of.*")
    public void yaml_event_read_bad_05() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed-value2.yaml"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Expected START_OBJECT.*")
    public void yaml_event_read_bad_06() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed.yaml"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Expected a field name.*")
    public void yaml_event_read_bad_07() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed2.yaml"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*but found '-'.*")
    public void yaml_event_read_bad_08() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/malformed3.yaml"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*CharConversionException.*")
    public void yaml_event_read_bad_09() throws IOException {
        YamlEventReaderWriter<String, String> reader = Serdes.YAML_STRING_STRING;
        reader.read(new File("test-data/event.yaml.gz"));
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Expected VALUE_STRING but got VALUE_NUMBER_INT.*")
    public void yaml_event_read_bad_10() throws IOException {
        YamlEventReaderWriter<Bytes, String> reader = Serdes.YAML_BYTES_STRING;
        reader.read(new File("test-data/kafka/bytes2.yaml"));
    }

    @Test
    public void yaml_event_write_01() throws IOException {
        YamlEventReaderWriter<Integer, String> writer = Serdes.YAML_INTEGER_STRING;
        SimpleEvent<Integer, String> event = new SimpleEvent(Collections.emptyList(), 123, "foo");

        verifyRoundTrip(writer, event);
    }

    @Test
    public void yaml_event_write_02() throws IOException {
        YamlEventReaderWriter<String, String> writer = Serdes.YAML_STRING_STRING;

        SimpleEvent<String, String> event = new SimpleEvent(Collections.emptyList(), null, "bar");
        verifyRoundTrip(writer, event);

        event = new SimpleEvent(Collections.emptyList(), "foo", null);
        verifyRoundTrip(writer, event);
    }

    @Test
    public void yaml_event_write_03() throws IOException {
        YamlEventReaderWriter<Integer, String> writer = Serdes.YAML_INTEGER_STRING;
        SimpleEvent<Integer, String> event =
                new SimpleEvent(List.of(new Header("Test", "test"), new Header("Foo", "bar")), 123, "foo");

        verifyRoundTrip(writer, event);
    }

    @Test
    public void yaml_event_write_04() throws IOException {
        YamlEventReaderWriter<Bytes, String> writer = Serdes.YAML_BYTES_STRING;
        SimpleEvent<Bytes, String> event =
                new SimpleEvent(Collections.emptyList(), new Bytes("test".getBytes(StandardCharsets.UTF_8)), "foo");

        verifyRoundTrip(writer, event);
    }

    @Test
    public void yaml_event_write_05() throws IOException {
        YamlEventReaderWriter<Graph, String> writer =
                new YamlEventReaderWriter<>(Serdes.GRAPH_DESERIALIZER, Serdes.STRING_DESERIALIZER,
                                            Serdes.GRAPH_SERIALIZER, Serdes.STRING_SERIALIZER
                );
        SimpleEvent<Graph, String> event =
                new SimpleEvent(List.of(new Header("Test", "test"), new Header("Foo", "bar")),
                                GraphFactory.createDefaultGraph(), "foo");

        verifyRoundTrip(writer, event);
    }

    private static <TKey, TValue> void verifyRoundTrip(FileEventReaderWriter<TKey, TValue> writer,
                                                       Event<TKey, TValue> event) throws IOException {
        File f = Files.createTempFile("yaml-event", ".yaml").toFile();
        try {
            writer.write(event, f);
            Assert.assertNotEquals(f.length(), 0L);

            Event<TKey, TValue> retrieved = writer.read(f);
            verifySameEvent(event, retrieved);
        } finally {
            f.delete();
        }
    }

    public static <TKey, TValue> void verifySameEvent(Event<TKey, TValue> event, Event<TKey, TValue> retrieved) {
        verifySame(event, retrieved, event.key(), retrieved.key());
        verifySame(event, retrieved, event.value(), retrieved.value());
        Assert.assertEquals(retrieved.headers().count(), event.headers().count());
    }

    public static <TKey, TValue, TItem> void verifySame(Event<TKey, TValue> event, Event<TKey, TValue> retrieved,
                                                        TItem expectedValue, TItem retrievedValue) {
        if (expectedValue != null && Graph.class.isAssignableFrom(expectedValue.getClass())) {
            if (retrievedValue == null) {
                Assert.assertTrue(((Graph) expectedValue).isEmpty());
            } else {
                Assert.assertTrue(((Graph) expectedValue).isIsomorphicWith((Graph) retrievedValue));
            }
        } else if (expectedValue != null && DatasetGraph.class.isAssignableFrom(expectedValue.getClass())) {
            DatasetGraph expected = (DatasetGraph) expectedValue;
            DatasetGraph actual = (DatasetGraph) retrievedValue;

            Assert.assertEquals(actual.size(), expected.size());
            expected.stream().forEach(actual::contains);
        } else {
            Assert.assertEquals(retrievedValue, expectedValue);
        }
    }

    @Test
    public void yaml_event_reader_writer_builder_01() throws IOException {
        //@formatter:off
        YamlEventReaderWriter<String, String> readerWriter =
                YamlEventReaderWriter.<String, String>create()
                                     .keySerializer(Serdes.STRING_SERIALIZER)
                                     .valueSerializer(Serdes.STRING_SERIALIZER)
                                     .keyDeserializer(Serdes.STRING_DESERIALIZER)
                                     .valueDeserializer(Serdes.STRING_DESERIALIZER).build();
        //@formatter:on
        Event<String, String> event = readerWriter.read(new File("test-data/event.yaml"));

        Assert.assertNotNull(event.key());
        Assert.assertNotNull(event.value());

        verifyRoundTrip(readerWriter, event);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void yaml_event_reader_writer_builder_02() {
        YamlEventReaderWriter.<String, String>create().build();
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void yaml_event_reader_writer_builder_03() {
        YamlEventReaderWriter.<String, String>create().keySerializer(Serdes.STRING_SERIALIZER).build();
    }
}
