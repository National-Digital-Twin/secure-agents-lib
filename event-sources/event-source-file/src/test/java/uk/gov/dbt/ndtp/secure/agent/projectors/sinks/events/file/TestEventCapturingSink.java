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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormatProvider;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormats;
import uk.gov.dbt.ndtp.secure.agent.sources.file.Serdes;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlFileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestEventCapturingSink {

    @DataProvider(name = "captureSettings")
    public Object[][] captureSettings() {
        return new Object[][] {
                { "test-", ".yaml", 4, YamlFileEventSource.YAML_FILTER },
                { null, "yaml", 3, YamlFileEventSource.YAML_FILTER },
                { "foo-", ".bar", 10, (FileFilter) pathname -> StringUtils.endsWith(pathname.getName(), ".bar") },
                { null, null, 0, (FileFilter) pathname -> true }
        };
    }

    @Test(dataProvider = "captureSettings")
    public void test_event_capturing_sink_01(String prefix, String extension, int padding, FileFilter filter) throws
            IOException {
        File targetDir = Files.createTempDirectory("yaml-events").toFile();
        EventCapturingSink<Integer, String> sink = EventCapturingSink.<Integer, String>create()
                                                                     .directory(targetDir)
                                                                     .extension(extension)
                                                                     .prefix(prefix)
                                                                     .padding(padding)
                                                                     .writer(new YamlEventReaderWriter<>(
                                                                             Serdes.INTEGER_SERIALIZER,
                                                                             Serdes.STRING_SERIALIZER))
                                                                     .writeYaml(y -> y.keySerializer(
                                                                                              Serdes.INTEGER_SERIALIZER)
                                                                                      .valueSerializer(
                                                                                              Serdes.STRING_SERIALIZER))
                                                                     .build();

        generateEvents(sink, 100);
        verifyExpectedFiles(targetDir, 100, padding, prefix, extension, filter);
        sink.close();

        // After closing the sink the event counter is reset so sending some more events should not create more files
        // than already existed
        generateEvents(sink, 1);
        verifyExpectedFiles(targetDir, 100, padding, prefix, extension, filter);
    }

    @Test(dataProvider = "captureSettings")
    public void test_event_capturing_sink_01b(String prefix, String extension, int padding, FileFilter filter) throws
            IOException {
        File targetDir = Files.createTempDirectory("yaml-events").toFile();
        EventCapturingSink<Integer, String> sink = EventCapturingSink.<Integer, String>create()
                                                                     .collect()
                                                                     .directory(targetDir)
                                                                     .extension(extension)
                                                                     .prefix(prefix)
                                                                     .padding(padding)
                                                                     .writer(new YamlEventReaderWriter<>(
                                                                             Serdes.INTEGER_SERIALIZER,
                                                                             Serdes.STRING_SERIALIZER))
                                                                     .writeYaml(y -> y.keySerializer(
                                                                                              Serdes.INTEGER_SERIALIZER)
                                                                                      .valueSerializer(
                                                                                              Serdes.STRING_SERIALIZER))
                                                                     .build();

        generateEvents(sink, 100);
        verifyExpectedFiles(targetDir, 100, padding, prefix, extension, filter);
        sink.close();

        // After closing the sink the event counter is reset so sending some more events should not create more files
        // than already existed
        generateEvents(sink, 1);
        verifyExpectedFiles(targetDir, 100, padding, prefix, extension, filter);
    }

    private static void generateEvents(EventCapturingSink<Integer, String> sink, int total) {
        for (int i = 0; i < total; i++) {
            SimpleEvent<Integer, String> event = new SimpleEvent<>(Collections.emptyList(), i, "Item " + i);
            sink.send(event);
        }
    }

    private static void verifyExpectedFiles(File targetDir, int expectedFiles, int padding, String prefix,
                                            String extension, FileFilter yamlFilter) {
        List<File> created = new ArrayList<>();
        Assert.assertTrue(targetDir.exists());
        Assert.assertTrue(targetDir.isDirectory());
        File[] actualFiles = targetDir.listFiles(yamlFilter);
        Assert.assertNotNull(actualFiles);
        for (File f : actualFiles) {
            created.add(f);
            f.deleteOnExit();
            Assert.assertTrue(f.length() > 0);
            if (padding > 0) {
                Assert.assertEquals(StringUtils.getDigits(f.getName()).length(), padding);
            } else {
                Assert.assertFalse(StringUtils.getDigits(f.getName()).isEmpty());
            }
            if (StringUtils.isNotBlank(prefix)) {
                Assert.assertTrue(StringUtils.startsWith(f.getName(), prefix));
            } else {
                Assert.assertTrue(Character.isDigit(f.getName().charAt(0)));
            }
            if (StringUtils.isNotBlank(extension)) {
                Assert.assertTrue(StringUtils.contains(f.getName(), "."));
                Assert.assertTrue(StringUtils.endsWith(f.getName(), extension));
            } else {
                Assert.assertFalse(StringUtils.contains(f.getName(), "."));
            }
        }
        Assert.assertEquals(created.size(), expectedFiles);
    }

    @Test
    public void test_event_capturing_sink_02() {
        try (EventCapturingSink<Integer, String> sink = EventCapturingSink.<Integer, String>create()
                                                                          .directory(new File("no-such-dir"))
                                                                          .extension(".yaml")
                                                                          .prefix("test-")
                                                                          .padding(4)
                                                                          .writer(new YamlEventReaderWriter<>(
                                                                                  Serdes.INTEGER_SERIALIZER,
                                                                                  Serdes.STRING_SERIALIZER))
                                                                          .writeYaml(y -> y.keySerializer(
                                                                                                   Serdes.INTEGER_SERIALIZER)
                                                                                           .valueSerializer(
                                                                                                   Serdes.STRING_SERIALIZER))
                                                                          .build()) {
            sink.send(null);
        }
    }

    @Test(expectedExceptions = SinkException.class)
    public void test_event_capturing_sink_03() {
        try (EventCapturingSink<Integer, String> sink = EventCapturingSink.<Integer, String>create()
                                                                          .directory(new File("no-such-dir"))
                                                                          .extension(".yaml")
                                                                          .prefix("test-")
                                                                          .padding(4)
                                                                          .writer(new YamlEventReaderWriter<>(
                                                                                  Serdes.INTEGER_SERIALIZER,
                                                                                  Serdes.STRING_SERIALIZER))
                                                                          .writeYaml(y -> y.keySerializer(
                                                                                                   Serdes.INTEGER_SERIALIZER)
                                                                                           .valueSerializer(
                                                                                                   Serdes.STRING_SERIALIZER))
                                                                          .build()) {
            sink.send(new SimpleEvent<>(Collections.emptyList(), 1, "test"));
        }
    }

    @Test
    public void test_event_capturing_sink_04() throws IOException {
        File targetDir = Files.createTempDirectory("yaml-events").toFile();
        try (EventCapturingSink<Integer, String> sink = EventCapturingSink.<Integer, String>create()
                                                                          .directory(targetDir)
                                                                          .extension(".yaml")
                                                                          .prefix("test-")
                                                                          .padding(4)
                                                                          .writer(new YamlEventReaderWriter<>(
                                                                                  Serdes.INTEGER_SERIALIZER,
                                                                                  Serdes.STRING_SERIALIZER))
                                                                          .writeYaml(y -> y.keySerializer(
                                                                                                   Serdes.INTEGER_SERIALIZER)
                                                                                           .valueSerializer(
                                                                                                   Serdes.STRING_SERIALIZER))
                                                                          .addHeader("Foo", "Bar")
                                                                          .addHeader(new Header("Test", "4"))
                                                                          .addHeader(null)
                                                                          .generateHeaders(e -> new Header("ID",
                                                                                                           Integer.toString(
                                                                                                                   e.key())))
                                                                          .generateHeaders(e -> e.key() % 2 == 0 ?
                                                                                                new Header(
                                                                                                        "Even-Number",
                                                                                                        "true") : null)
                                                                          .generateHeaders(
                                                                                  (Function<Event<Integer, String>, Header>) null)
                                                                          .build()) {
            sink.send(new SimpleEvent<>(Collections.emptyList(), 1, "test"));
            sink.send(new SimpleEvent<>(Collections.emptyList(), 2, "test"));
        }

        verifyModifiedCapturedEvents(targetDir);
    }

    @Test
    public void test_event_capturing_sink_05() throws IOException {
        File targetDir = Files.createTempDirectory("yaml-events").toFile();
        //@formatter:off
        try (EventCapturingSink<Integer, String> sink
                     = EventCapturingSink.<Integer, String>create()
                                         .directory(targetDir)
                                         .extension(".yaml")
                                         .prefix("test-")
                                         .padding(4)
                                         .writer(new YamlEventReaderWriter<>(
                                                  Serdes.INTEGER_SERIALIZER,
                                                  Serdes.STRING_SERIALIZER))
                                         .writeYaml(y -> y.keySerializer(
                                                                   Serdes.INTEGER_SERIALIZER)
                                                           .valueSerializer(
                                                                   Serdes.STRING_SERIALIZER))
                                         .addHeaders(List.of(new Header("Foo", "Bar"),
                                                              new Header("Test", "4")))
                                         .addHeaders(null)
                                         .generateHeaders(List.of(e -> new Header("ID",
                                                                                  Integer.toString(
                                                                                          e.key())),
                                                                  e -> e.key() % 2 == 0 ?
                                                                       new Header(
                                                                               "Even-Number",
                                                                               "true") :
                                                                       null))
                                         .generateHeaders((List<Function<Event<Integer, String>, Header>>) null)
                                         .build()) {
            sink.send(new SimpleEvent<>(Collections.emptyList(), 1, "test"));
            sink.send(new SimpleEvent<>(Collections.emptyList(), 2, "test"));
        }
        //@formatter:on

        verifyModifiedCapturedEvents(targetDir);
    }

    private static void verifyModifiedCapturedEvents(File targetDir) {
        FileEventFormatProvider format = FileEventFormats.get(YamlFormat.EVENT_FORMAT_NAME);
        EventSource<Integer, String> source =
                format.createSource(new IntegerDeserializer(), new StringDeserializer(), targetDir);
        Assert.assertEquals(source.remaining(), 2);

        Event<Integer, String> first = source.poll(Duration.ofSeconds(1));
        Event<Integer, String> second = source.poll(Duration.ofSeconds(1));
        Assert.assertNotNull(first);
        Assert.assertNotNull(second);
        Assert.assertNotEquals(first, second);

        // Both events should have Foo header added
        Assert.assertEquals(first.lastHeader("Foo"), "Bar");
        Assert.assertEquals(second.lastHeader("Foo"), "Bar");
        // Both events should have Test header added
        Assert.assertEquals(first.lastHeader("Test"), "4");
        Assert.assertEquals(second.lastHeader("Test"), "4");
        // Both events should have an ID header generated with their key values
        Assert.assertEquals(first.lastHeader("ID"), "1");
        Assert.assertEquals(second.lastHeader("ID"), "2");
        // Only one event should have an Even-Number header
        Assert.assertNull(first.lastHeader("Even-Number"));
        Assert.assertNotNull(second.lastHeader("Even-Number"));
    }
}
