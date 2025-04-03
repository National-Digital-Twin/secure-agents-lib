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

import ch.qos.logback.classic.Level;
import com.github.rvesse.airline.parser.ParseResult;
import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.Capture;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.Dump;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.RdfDump;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.Replay;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.sys.JenaSystem;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.AbstractCommandHelper;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.live.LiveReporter;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveHeartbeat;
import uk.gov.dbt.ndtp.secure.agent.live.serializers.LiveHeartbeatDeserializer;
import uk.gov.dbt.ndtp.secure.agent.projectors.driver.ProjectorDriver;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events.file.EventCapturingSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormatProvider;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormats;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.BasicKafkaTestCluster;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.file.AbstractJacksonOffsetStore;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.options.LiveReporterOptions;

public class AbstractDockerDebugCliHelper extends AbstractCommandHelper {

    static {
        JenaSystem.init();
    }

    protected final KafkaTestCluster kafka = new BasicKafkaTestCluster();

    public static void verifyEvents(String format) {
        String stdOut = SecureAgentCommandTester.getLastStdOut();
        for (int i = 1; i <= 1_000; i++) {
            boolean eventFound = StringUtils.contains(stdOut, String.format(format, i));
            if (!eventFound) {
                SecureAgentCommandTester.printToOriginalStdOut(
                        "Missing expected event, command standard error is displayed below:");
                SecureAgentCommandTester.printToOriginalStdOut(SecureAgentCommandTester.getLastStdErr());
                SecureAgentCommandTester.printToOriginalStdOut("\n\n");
            }
            Assert.assertTrue(eventFound, "Missing event " + i);
        }
    }

    public void verifyReplayedEvents(String format) {
        KafkaEventSource<String, String> source = null;
        try {
            source = KafkaEventSource.<String, String>create()
                                     .bootstrapServers(
                                             this.kafka.getBootstrapServers())
                                     .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                     .consumerGroup("replay-verification")
                                     .readPolicy(KafkaReadPolicies.fromBeginning())
                                     .keyDeserializer(StringDeserializer.class)
                                     .valueDeserializer(StringDeserializer.class)
                                     .build();
            for (int i = 1; i <= 1_000; i++) {
                Event<String, String> event = source.poll(Duration.ofSeconds(3));
                Assert.assertNotNull(event, "Missing event " + i);
                Assert.assertTrue(StringUtils.contains(event.value(), String.format(format, i)),
                                  "Wrong value for event " + i);
            }
        } finally {
            if (source != null) {
                source.close();
            }
        }
    }

    protected static void verifyCapturedEvents(File captureDir, String captureFormat, String format) {
        FileEventFormatProvider provider = FileEventFormats.get(captureFormat);
        Assert.assertNotNull(provider);

        FileEventSource<Bytes, String> source =
                provider.createSource(new BytesDeserializer(), new StringDeserializer(), captureDir);
        for (int i = 1; i <= 1_000; i++) {
            Event<Bytes, String> event = source.poll(Duration.ofSeconds(1));
            Assert.assertNotNull(event, "Missing event " + i);
            Assert.assertTrue(Objects.equals(event.value(), String.format(format, i)), "Wrong event " + i);
        }
        Assert.assertNull(source.poll(Duration.ofSeconds(1)));
        Assert.assertTrue(source.isExhausted());
        source.close();
    }

    public static void printStdErrIfFailedUnexpectedly() {
        if (SecureAgentCommandTester.getLastExitStatus() != 0) {
            SecureAgentCommandTester.printToOriginalStdOut(
                    "Command exited with " + SecureAgentCommandTester.getLastExitStatus() + " when 0 was expected, standard error is displayed below:");
            SecureAgentCommandTester.printToOriginalStdOut(SecureAgentCommandTester.getLastStdErr());
            SecureAgentCommandTester.printToOriginalStdOut("\n\n");
        }
    }

    public static void verifyDumpCommandUsed() {
        verifyCommandUsed(Dump.class);

        AbstractDockerDebugCliHelper.printStdErrIfFailedUnexpectedly();
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
    }

    public static void verifyCommandUsed(Class<?> expectedCommandClass) {
        ParseResult<SecureAgentCommand> result = SecureAgentCommandTester.getLastParseResult();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.wasSuccessful());
        SecureAgentCommand command = result.getCommand();
        Assert.assertNotNull(command);
        Assert.assertEquals(command.getClass(), expectedCommandClass);
    }

    public static void verifyRdfDumpCommandUsed() {
        verifyCommandUsed(RdfDump.class);

        AbstractDockerDebugCliHelper.printStdErrIfFailedUnexpectedly();
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
    }

    public static void verifyCaptureCommandUsed() {
        verifyCommandUsed(Capture.class);

        AbstractDockerDebugCliHelper.printStdErrIfFailedUnexpectedly();
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
    }

    public static void verifyReplayCommandUsed() {
        verifyCommandUsed(Replay.class);

        AbstractDockerDebugCliHelper.printStdErrIfFailedUnexpectedly();
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
    }

    private void setUpLogging() {
        TestLogUtil.enableSpecificLogging(KafkaEventSource.class, Level.DEBUG);
        TestLogUtil.enableSpecificLogging(LiveReporterOptions.class, Level.WARN);
        TestLogUtil.enableSpecificLogging(LiveReporter.class, Level.INFO);
        TestLogUtil.enableSpecificLogging(ProjectorDriver.class, Level.INFO);
        TestLogUtil.enableSpecificLogging(AbstractJacksonOffsetStore.class, Level.DEBUG);
    }

    @BeforeClass
    @Override
    public void setup() {
        // Uncomment for easier debugging in IDE
        //SecureAgentCommandTester.TEE_TO_ORIGINAL_STREAMS = true;
        super.setup();

        this.kafka.setup();
        setUpLogging();
    }

    @AfterMethod
    @Override
    public void testCleanup() throws InterruptedException {
        super.testCleanup();

        this.kafka.resetTestTopic();
        this.kafka.resetTopic(LiveReporter.DEFAULT_LIVE_TOPIC);

        // Occasionally we can get random test errors because the test topic(s) don't get recreated in time which can
        // lead to incomplete test data being generated in the subsequent test.  A short sleep makes that unlikely to
        // occur.
        Thread.sleep(500);
    }

    private void teardownLogging() {
        TestLogUtil.enableSpecificLogging(KafkaEventSource.class, Level.OFF);
        TestLogUtil.enableSpecificLogging(LiveReporterOptions.class, Level.OFF);
        TestLogUtil.enableSpecificLogging(LiveReporter.class, Level.OFF);
        TestLogUtil.enableSpecificLogging(ProjectorDriver.class, Level.OFF);
        TestLogUtil.enableSpecificLogging(AbstractJacksonOffsetStore.class, Level.OFF);
    }

    @AfterClass
    @Override
    public void teardown() {
        this.kafka.teardown();
        teardownLogging();
        super.teardown();
    }

    protected void generateKafkaEvents(String format) {
        generateKafkaEvents(Collections.emptyList(), format);
    }

    protected void generateKafkaEvents(Collection<Header> headers, String format) {
        try (KafkaSink<String, String> sink = KafkaSink.<String, String>create()
                                                       .keySerializer(StringSerializer.class)
                                                       .valueSerializer(StringSerializer.class)
                                                       .bootstrapServers(this.kafka.getBootstrapServers())
                                                       .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                       .lingerMs(5)
                                                       .build()) {
            for (int i = 1; i <= 1_000; i++) {
                sink.send(new SimpleEvent<>(headers, Integer.toString(i), String.format(format, i)));
            }
        }
    }

    protected void generateCapturedEvents(File captureDir, FileEventFormatProvider provider, Collection<Header> headers,
                                          String format) {
        try (EventCapturingSink<String, String> sink = EventCapturingSink.<String, String>create()
                                                                         .directory(captureDir)
                                                                         .extension(provider.defaultFileExtension())
                                                                         .writer(provider.createWriter(
                                                                                 new StringSerializer(),
                                                                                 new StringSerializer()))
                                                                         .discard()
                                                                         .build()) {
            for (int i = 1; i <= 1_000; i++) {
                sink.send(new SimpleEvent<>(headers, Integer.toString(i), String.format(format, i)));
            }
        }
    }

    protected void verifyHeartbeats(boolean kafkaHeartbeatsExpected) {
        //@formatter:off
        KafkaEventSource<Bytes, LiveHeartbeat> source =
                KafkaEventSource.<Bytes, LiveHeartbeat>create().bootstrapServers(this.kafka.getBootstrapServers())
                                .topic(LiveReporter.DEFAULT_LIVE_TOPIC)
                                .keyDeserializer(BytesDeserializer.class)
                                .valueDeserializer(LiveHeartbeatDeserializer.class)
                                .consumerGroup("debug-cli-tests")
                                .readPolicy(KafkaReadPolicies.fromBeginning())
                                .build();
        //@formatter:on
        try {
            if (kafkaHeartbeatsExpected) {
                // Make sure that some heartbeats were emitted
                Assert.assertNotEquals(source.remaining(), 0L);
            } else {
                // No heartbeats expected
                Assert.assertNull(source.remaining());
            }
        } finally {
            source.close();
        }
    }
}
