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
package uk.gov.dbt.ndtp.secure.agent.sources.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.AbstractEventSourceTests;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSourceException;
import uk.gov.dbt.ndtp.secure.agent.sources.file.rdf.RdfFileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlFileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestFileEventSource extends AbstractEventSourceTests<Integer, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFileEventSource.class);

    private final Map<Integer, File> generatedEventSources = new HashMap<>();

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".* cannot be null")
    public void file_event_source_bad_01() {
        new FileEventSource<String, String>(null, null, null, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".* cannot be null")
    public void file_event_source_bad_02() {
        new FileEventSource<String, String>(new File("."), null, null, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".* cannot be null")
    public void file_event_source_bad_03() {
        new FileEventSource<String, String>(new File("."), f -> false, null, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".* cannot be null")
    public void file_event_source_bad_04() {
        new FileEventSource<String, String>(new File("."), f -> false, new NumericFilenameComparator(), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".* not a directory")
    public void file_event_source_bad_05() {
        new FileEventSource<>(new File("pom.xml"), f -> false, new NumericFilenameComparator(),
                              Serdes.YAML_STRING_STRING);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "No such directory.*")
    public void file_event_source_bad_06() {
        new FileEventSource<>(new File("no-such-file"), f -> false, new NumericFilenameComparator(),
                              Serdes.YAML_STRING_STRING);
    }

    @Test
    public void file_event_source_01() {
        EventSource<String, String> source =
                new FileEventSource<>(new File("test-data"), f -> false, new NumericFilenameComparator(),
                                      Serdes.YAML_STRING_STRING);
        Assert.assertFalse(source.isClosed());
        Assert.assertFalse(source.availableImmediately());
        Assert.assertTrue(source.isExhausted());
        Assert.assertNull(source.poll(Duration.ofSeconds(1)));
        Assert.assertEquals(source.remaining(), 0L);

        source.processed(Collections.emptyList());

        source.close();
        Assert.assertTrue(source.isClosed());
        Assert.assertTrue(source.isExhausted());
        Assert.assertFalse(source.availableImmediately());
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Event source is closed")
    public void file_event_source_02() {
        EventSource<String, String> source =
                new FileEventSource<>(new File("test-data"), f -> false, new NumericFilenameComparator(),
                                      Serdes.YAML_STRING_STRING);
        source.close();
        source.poll(Duration.ofSeconds(1));
    }

    @Test
    public void file_event_source_03() {
        EventSource<String, String> source =
                new YamlFileEventSource<>(new File("test-data"), Serdes.STRING_DESERIALIZER,
                                          Serdes.STRING_DESERIALIZER);
        Assert.assertTrue(source.availableImmediately());
        Assert.assertFalse(source.isExhausted());
        Assert.assertEquals(source.remaining(), 4L);
    }

    @Test(expectedExceptions = EventSourceException.class, expectedExceptionsMessageRegExp = "Invalid Event.*")
    public void file_event_source_04() {
        EventSource<String, String> source =
                new YamlFileEventSource<>(new File("test-data"), Serdes.STRING_DESERIALIZER,
                                          Serdes.STRING_DESERIALIZER);
        source.poll(Duration.ofSeconds(1));
    }

    @Test(expectedExceptions = EventSourceException.class, expectedExceptionsMessageRegExp = "Failed to parse.*")
    public void file_event_source_05() {
        EventSource<String, String> source =
                new FileEventSource<>(new File("test-data"), f -> Objects.equals(f.getName(), "malformed2.yaml"),
                                      new NumericFilenameComparator(), Serdes.YAML_STRING_STRING);
        source.poll(Duration.ofSeconds(1));
    }

    @Test
    public void file_event_source_06() throws IOException {
        YamlEventReaderWriter<Integer, String> yaml = Serdes.YAML_INTEGER_STRING;
        File tempDir = Files.createTempDirectory("yaml-events").toFile();
        for (int i = 0; i < 1_000; i++) {
            SimpleEvent<Integer, String> event = new SimpleEvent<>(Collections.emptyList(), i, "Value " + i);
            yaml.write(event, new File(tempDir, "event" + StringUtils.leftPad(Integer.toString(i), 6, '0') + ".yaml"));
        }

        EventSource<Integer, String> source =
                new YamlFileEventSource<>(tempDir, Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER);
        Assert.assertEquals(source.remaining(), 1_000L);
        Assert.assertTrue(source.availableImmediately());
        Assert.assertFalse(source.isExhausted());

        int expectedKey = 0;
        while (!source.isExhausted()) {
            Assert.assertTrue(source.availableImmediately());
            Event<Integer, String> event = source.poll(Duration.ofSeconds(1));
            Assert.assertEquals(event.key(), expectedKey);

            expectedKey++;
        }
        Assert.assertFalse(source.availableImmediately());
    }

    @Test
    public void file_event_source_07() {
        EventSource<String, String> source =
                new YamlFileEventSource<>(new File("test-data/bad-dir"), Serdes.STRING_DESERIALIZER,
                                          Serdes.STRING_DESERIALIZER);
        Assert.assertFalse(source.isClosed());
        Assert.assertFalse(source.availableImmediately());
        Assert.assertTrue(source.isExhausted());
        Assert.assertNull(source.poll(Duration.ofSeconds(1)));
        Assert.assertEquals(source.remaining(), 0L);

        source.close();
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void file_event_source_08a() {
        EventSource<String, String> source =
                new YamlFileEventSource<>(new File("test-data/rdf/"), false, Serdes.STRING_DESERIALIZER,
                                          Serdes.STRING_DESERIALIZER);
        Assert.assertTrue(source.availableImmediately());
        Assert.assertFalse(source.isExhausted());
        Assert.assertEquals(source.remaining(), 4L);
    }

    @Test
    public void file_event_source_08b() {
        EventSource<String, String> source =
                new YamlFileEventSource<>(new File("test-data/rdf/"), true, Serdes.STRING_DESERIALIZER,
                                          Serdes.STRING_DESERIALIZER);
        Assert.assertTrue(source.availableImmediately());
        Assert.assertFalse(source.isExhausted());
        Assert.assertEquals(source.remaining(), 4L);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*cannot be null")
    public void single_file_event_source_bad_01() {
        new SingleFileEventSource<>(new File("test-data"), Serdes.YAML_STRING_STRING);
    }

    @Test(expectedExceptions = EventSourceException.class, expectedExceptionsMessageRegExp = "Failed to parse an Event from file.*")
    public void single_file_event_source_bad_02() {
        EventSource<String, String> source =
                new SingleFileEventSource<>(new File(new File("test-data", "rdf"), "rdf1.txt"),
                                            Serdes.YAML_STRING_STRING);
        source.poll(Duration.ofSeconds(1));
    }

    @Test
    public void single_file_event_source_01() {
        EventSource<String, String> source =
                new SingleFileEventSource<>(new File("test-data", "event.yaml"), Serdes.YAML_STRING_STRING);
        Assert.assertTrue(source.availableImmediately());
        Assert.assertFalse(source.isExhausted());
        Assert.assertEquals(source.remaining(), 1L);

        Event<String, String> event = source.poll(Duration.ofSeconds(1));
        Assert.assertNotNull(event);
        Assert.assertFalse(source.availableImmediately());
        Assert.assertTrue(source.isExhausted());
    }

    @Test
    public void rdf_file_event_source_01() {
        EventSource<String, DatasetGraph> source =
                new RdfFileEventSource<>(new File("test-data", "rdf"), Serdes.STRING_DESERIALIZER,
                                         Serdes.DATASET_GRAPH_DESERIALIZER);
        Assert.assertTrue(source.availableImmediately());
        Assert.assertFalse(source.isExhausted());
        Assert.assertEquals(source.remaining(), 4);

        while (!source.isExhausted()) {
            Assert.assertNotNull(source.poll(Duration.ofSeconds(1)));
        }
    }

    @DataProvider(name = "sample-data-sizes")
    @Override
    public Object[][] getTestSizes() {
        return new Object[][] {
                { 100 }, { 500 }, { 1_000 }
        };
    }

    @Override
    protected EventSource<Integer, String> createEmptySource() {
        try {
            File emptyDir = Files.createTempDirectory("empty").toFile();
            return new YamlFileEventSource<>(emptyDir, Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected EventSource<Integer, String> createSource(Collection<Event<Integer, String>> events) {
        try {
            File existingDir = generatedEventSources.get(events.size());
            if (existingDir != null) {
                return new YamlFileEventSource<>(existingDir, Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER);
            }

            File tempDir = Files.createTempDirectory("yaml-source").toFile();
            LOGGER.info("Writing a test event source directory with {} events to {}", events.size(),
                        tempDir.getAbsolutePath());
            YamlEventReaderWriter<Integer, String> readerWriter = Serdes.YAML_INTEGER_STRING;
            int counter = 0;
            for (Event<Integer, String> event : events) {
                readerWriter.write(event, new File(tempDir, "event-" + (++counter) + ".yaml"));
            }
            LOGGER.info("Wrote a test event source directory with {} events to {}", events.size(),
                        tempDir.getAbsolutePath());
            generatedEventSources.put(events.size(), tempDir);
            return new YamlFileEventSource<>(tempDir, Serdes.INTEGER_DESERIALIZER, Serdes.STRING_DESERIALIZER);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Collection<Event<Integer, String>> createSampleData(int size) {
        List<Event<Integer, String>> events = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            events.add(new SimpleEvent<>(Collections.emptyList(), i, String.format("Event #%,d", i)));
        }
        return events;
    }

    @Override
    public boolean guaranteesImmediateAvailability() {
        return true;
    }

    @Override
    public boolean isUnbounded() {
        return false;
    }
}
