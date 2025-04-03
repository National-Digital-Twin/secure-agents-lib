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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.debug;

import com.github.rvesse.airline.parser.ParseResult;
import com.github.rvesse.airline.parser.errors.ParseException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.AbstractCommandHelper;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.payloads.RdfPayload;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events.file.EventCapturingSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormatProvider;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormats;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.rdf.RdfFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.file.text.PlainTextFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.RdfPayloadDeserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.HelpCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;

public class TestDebugCli extends AbstractCommandHelper {

    @Test
    public void debug_cli_01() {
        DebugCli.main(new String[0]);

        ParseResult<SecureAgentCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.wasSuccessful());
        SecureAgentCommand command = result.getCommand();
        Assert.assertNotNull(command);
        Assert.assertTrue(command instanceof HelpCommand);

        String stdOut = SecureAgentCommandTester.getLastStdOut();
        Assert.assertFalse(StringUtils.isBlank(stdOut));
        Assert.assertTrue(StringUtils.contains(stdOut, "Commands are:"));
    }

    @Test
    public void debug_cli_file_event_formats_bad_01() throws IOException {
        File sourceFile = Files.createTempFile("event-source", ".yaml").toFile();
        sourceFile.deleteOnExit();
        DebugCli.main(
                new String[] {
                        "dump",
                        "--source-file",
                        sourceFile.getAbsolutePath(),
                        "--source-format",
                        "no-such-format"
                });

        ParseResult<SecureAgentCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.wasSuccessful());

        List<ParseException> errors = new ArrayList<>(result.getErrors());
        Assert.assertFalse(errors.isEmpty());
        Assert.assertTrue(errors.stream()
                                .anyMatch(e -> StringUtils.contains(e.getMessage(),
                                                                    "not in the list of allowed values")));
    }

    @Test
    public void debug_cli_file_event_formats_bad_02() throws IOException {
        File sourceFile = Files.createTempFile("event-source", ".yaml").toFile();
        sourceFile.deleteOnExit();
        DebugCli.main(
                new String[] {
                        "dump",
                        "--source-file",
                        sourceFile.getAbsolutePath(),
                        "--source-format",
                        YamlFormat.EVENT_FORMAT_NAME.toUpperCase(
                                Locale.ROOT)
                });

        ParseResult<SecureAgentCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertNotNull(result);
        Assert.assertFalse(result.wasSuccessful());

        List<ParseException> errors = new ArrayList<>(result.getErrors());
        Assert.assertFalse(errors.isEmpty());
        Assert.assertTrue(errors.stream()
                                .anyMatch(e -> StringUtils.contains(e.getMessage(),
                                                                    "not in the list of allowed values")));
    }

    @Test
    public void debug_cli_capture_bad_01() throws IOException {
        File directory = Files.createTempDirectory("event-source").toFile();
        DebugCli.main(
                new String[] {
                        "dump",
                        "--source-dir",
                        directory.getAbsolutePath(),
                        "--source-format",
                        YamlFormat.EVENT_FORMAT_NAME,
                        "--capture-dir",
                        directory.getAbsolutePath(),
                        "--capture-format",
                        YamlFormat.EVENT_FORMAT_NAME
                });

        ParseResult<SecureAgentCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertNotEquals(SecureAgentCommandTester.getLastExitStatus(), 0);

        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Cannot specify the same"));
    }

    @Test
    public void debug_cli_capture_bad_02() throws IOException {
        File directory = Files.createTempDirectory("event-source").toFile();
        DebugCli.main(
                new String[] {
                        "capture",
                        "--source-dir",
                        directory.getAbsolutePath(),
                        "--source-format",
                        YamlFormat.EVENT_FORMAT_NAME
                });

        ParseResult<SecureAgentCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertNotEquals(SecureAgentCommandTester.getLastExitStatus(), 0);

        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Failed to specify sufficient options"));
    }


    /*
    NB - Kafka requiring tests live in DockerTestDebugCli
     */

    @Test
    public void debug_dump_01() throws IOException {
        // Add some sample events to a directory
        File sourceDir = Files.createTempDirectory("dump-events-input").toFile();
        try (EventCapturingSink<String, String> sink = EventCapturingSink.<String, String>create()
                                                                         .directory(sourceDir)
                                                                         .extension(".yaml")
                                                                         .writeYaml(y -> y.keySerializer(
                                                                                                  new StringSerializer())
                                                                                          .valueSerializer(
                                                                                                  new StringSerializer()))
                                                                         .build()) {
            for (int i = 1; i <= 1_000; i++) {
                sink.send(
                        new SimpleEvent<>(Collections.emptyList(), Integer.toString(i), String.format("Event %,d", i)));
            }
        }

        DebugCli.main(new String[] {
                "dump",
                "--source-dir",
                sourceDir.getAbsolutePath(),
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING"
        });

        AbstractDockerDebugCliHelper.verifyDumpCommandUsed();

        verifyEventsDumped("Event %,d");
    }

    @Test
    public void debug_dump_02() throws IOException {
        // Add a single sample events to a file
        File sourceFile = Files.createTempFile("single-event", ".yaml").toFile();
        FileEventFormatProvider format = FileEventFormats.get(YamlFormat.EVENT_FORMAT_NAME);
        FileEventWriter<String, String> writer = format.createWriter(new StringSerializer(), new StringSerializer());
        writer.write(new SimpleEvent<>(Collections.emptyList(), Integer.toString(1), "Event 1"), sourceFile);

        DebugCli.main(new String[] {
                "dump",
                "--source-file",
                sourceFile.getAbsolutePath(),
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING"
        });

        AbstractDockerDebugCliHelper.verifyDumpCommandUsed();

        String stdOut = SecureAgentCommandTester.getLastStdOut();
        Assert.assertTrue(StringUtils.contains(stdOut, "Event 1"));
    }

    @Test
    public void debug_rdf_dump_01() throws IOException {
        // Add some sample events to a directory
        File sourceDir = Files.createTempDirectory("dump-events-input").toFile();
        try (EventCapturingSink<String, String> sink = EventCapturingSink.<String, String>create()
                                                                         .directory(sourceDir)
                                                                         .extension(".yaml")
                                                                         .writeYaml(y -> y.keySerializer(
                                                                                                  new StringSerializer())
                                                                                          .valueSerializer(
                                                                                                  new StringSerializer()))
                                                                         .build()) {
            for (int i = 1; i <= 1_000; i++) {
                sink.send(
                        new SimpleEvent<>(Collections.emptyList(), Integer.toString(i),
                                          String.format("<http://subject> <http://predicate> \"%d\" .", i)));
            }
        }

        DebugCli.main(new String[] {
                "rdf-dump",
                "--source-dir",
                sourceDir.getAbsolutePath(),
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING"
        });

        AbstractDockerDebugCliHelper.verifyRdfDumpCommandUsed();

        verifyEventsDumped("\"%d\"");
    }

    @Test
    public void debug_rdf_dump_02() throws IOException {
        // Add a single event to a file
        File sourceFile = Files.createTempFile("single-event", ".yaml").toFile();
        FileEventFormatProvider format = FileEventFormats.get(YamlFormat.EVENT_FORMAT_NAME);
        FileEventWriter<String, String> writer = format.createWriter(new StringSerializer(), new StringSerializer());
        writer.write(
                new SimpleEvent<>(Collections.emptyList(), Integer.toString(1),
                                  "<http://subject> <http://predicate> \"1\" ."), sourceFile);


        DebugCli.main(new String[] {
                "rdf-dump",
                "--source-file",
                sourceFile.getAbsolutePath(),
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING"
        });

        AbstractDockerDebugCliHelper.verifyRdfDumpCommandUsed();

        String stdOut = SecureAgentCommandTester.getLastStdOut();
        Assert.assertTrue(StringUtils.contains(stdOut, "\"1\""));
    }

    @Test
    public void debug_rdf_dump_capture_01() throws IOException {
        File sourceFile = Files.createTempFile("capture-source", ".nt").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile))) {
            writer.write("<http://subject> <http://predicate> \"1\" .");
        }
        File captureDir = Files.createTempDirectory("capture-target").toFile();

        DebugCli.main(new String[] {
                "rdf-dump",
                "--source-file",
                sourceFile.getAbsolutePath(),
                "--source-format",
                RdfFormat.EVENT_FORMAT_NAME,
                "--capture-dir",
                captureDir.getAbsolutePath(),
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                });

        AbstractDockerDebugCliHelper.verifyRdfDumpCommandUsed();
        verifyCapturedRdfEvent(captureDir, YamlFormat.EVENT_FORMAT_NAME);
    }

    @Test
    public void debug_rdf_dump_capture_02() throws IOException {
        File sourceFile = Files.createTempFile("capture-source", ".nt").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile))) {
            writer.write("<http://subject> <http://predicate> \"1\" .");
        }
        File captureDir = Files.createTempDirectory("capture-target").toFile();
        Assert.assertTrue(captureDir.delete());

        DebugCli.main(new String[] {
                "rdf-dump",
                "--source-file",
                sourceFile.getAbsolutePath(),
                "--source-format",
                RdfFormat.EVENT_FORMAT_NAME,
                "--capture-dir",
                captureDir.getAbsolutePath(),
                "--capture-format",
                PlainTextFormat.EVENT_FORMAT_NAME,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                });

        AbstractDockerDebugCliHelper.verifyRdfDumpCommandUsed();
        verifyCapturedRdfEvent(captureDir, PlainTextFormat.EVENT_FORMAT_NAME);
    }

    @Test
    public void debug_rdf_dump_capture_bad_01() throws IOException {
        String os = System.getProperty("os.name");
        if (StringUtils.contains(os, "Windows")) {
            throw new SkipException("Test not suitable for Windows");
        }

        File sourceFile = Files.createTempFile("capture-source", ".nt").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sourceFile))) {
            writer.write("<http://subject> <http://predicate> \"1\" .");
        }
        File captureDir = new File("/should/not/be/able/to/create");

        DebugCli.main(new String[] {
                "rdf-dump",
                "--source-file",
                sourceFile.getAbsolutePath(),
                "--source-format",
                RdfFormat.EVENT_FORMAT_NAME,
                "--capture-dir",
                captureDir.getAbsolutePath(),
                "--capture-format",
                PlainTextFormat.EVENT_FORMAT_NAME,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                });

        ParseResult<SecureAgentCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertNotEquals(SecureAgentCommandTester.getLastExitStatus(), 0);

        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Failed to create"));
    }

    private static void verifyCapturedRdfEvent(File captureDir, String captureFormat) {
        FileEventFormatProvider provider = FileEventFormats.get(captureFormat);
        EventSource<Bytes, RdfPayload> source =
                provider.createSource(new BytesDeserializer(), new RdfPayloadDeserializer(), captureDir);
        Assert.assertEquals(source.remaining(), 1);

        Event<Bytes, RdfPayload> event = source.poll(Duration.ofSeconds(1));
        Assert.assertNotNull(event);
        Assert.assertNull(event.key());
        Assert.assertNotNull(event.value());
        Assert.assertEquals(event.lastHeader(HttpNames.hContentType), WebContent.contentTypeNQuads);

        RdfPayload payload = event.value();
        Assert.assertTrue(payload.isDataset());
        Assert.assertNotNull(payload.getDataset());

        Assert.assertTrue(source.isExhausted());
    }

    @Test
    public void debug_dump_capture_transpose_01() throws IOException {
        // Add some sample events to a directory
        File sourceDir = Files.createTempDirectory("dump-events-input").toFile();
        try (EventCapturingSink<String, String> sink = EventCapturingSink.<String, String>create()
                                                                         .directory(sourceDir)
                                                                         .extension(".yaml")
                                                                         .writeYaml(y -> y.keySerializer(
                                                                                                  new StringSerializer())
                                                                                          .valueSerializer(
                                                                                                  new StringSerializer()))
                                                                         .build()) {
            for (int i = 1; i <= 1_000; i++) {
                sink.send(
                        new SimpleEvent<>(Collections.emptyList(), Integer.toString(i), String.format("Event %,d", i)));
            }
        }

        DebugCli.main(new String[] {
                "dump",
                "--source-dir",
                sourceDir.getAbsolutePath(),
                "--source-format",
                YamlFormat.EVENT_FORMAT_NAME,
                "--capture-dir",
                sourceDir.getAbsolutePath(),
                "--capture-format",
                PlainTextFormat.EVENT_FORMAT_NAME,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                });

        AbstractDockerDebugCliHelper.verifyDumpCommandUsed();
        verifyEventsDumped("Event %,d");

        SecureAgentCommandTester.resetTestState();
        DebugCli.main(new String[] {
                "dump",
                "--source-dir",
                sourceDir.getAbsolutePath(),
                "--source-format",
                PlainTextFormat.EVENT_FORMAT_NAME,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                });

        AbstractDockerDebugCliHelper.verifyDumpCommandUsed();
        verifyEventsDumped("Event %,d");
    }

    private static void verifyEventsDumped(String format) {
        String stdOut = SecureAgentCommandTester.getLastStdOut();
        for (int i = 1; i < 1_000; i++) {
            Assert.assertTrue(StringUtils.contains(stdOut, String.format(format, i)));
        }
    }
}
