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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.AbstractCommandHelper;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.live.LiveReporter;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.FlakyKafkaRetryAnalyzerHelper;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.MutualTlsKafkaTestCluster;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class DockerTestDebugCliMutualTlsKafka extends AbstractCommandHelper {

    private static final String CLIENT_PROPERTIES_FILE = new File("test-certs/client.properties").getAbsolutePath();
    private final MutualTlsKafkaTestCluster kafka = new MutualTlsKafkaTestCluster();

    @BeforeClass
    @Override
    public void setup() {
        if (StringUtils.contains(System.getProperty("os.name"), "Windows")) {
            throw new SkipException(
                    "These tests cannot run on Windows because the SSL certificates generator script assumes a Posix compatible OS");
        }

        // Uncomment for easier debugging in IDE
        //SecureAgentCommandTester.TEE_TO_ORIGINAL_STREAMS = true;
        super.setup();
        setupLogging();
        this.kafka.setup();
    }

    private void setupLogging() {
        TestLogUtil.enableSpecificLogging(KafkaEventSource.class, Level.DEBUG);
        TestLogUtil.enableSpecificLogging(LiveReporter.class, Level.INFO);
    }

    private void teardownLogging() {
        TestLogUtil.enableSpecificLogging(KafkaEventSource.class, Level.OFF);
        TestLogUtil.enableSpecificLogging(LiveReporter.class, Level.OFF);
    }


    @AfterMethod
    @Override
    public void testCleanup() throws InterruptedException {
        super.testCleanup();
        this.kafka.resetTestTopic();

        // Occasionally we can get random test errors because the test topic(s) don't get recreated in time which can
        // lead to incomplete test data being generated in the subsequent test.  A short sleep makes that unlikely to
        // occur.
        Thread.sleep(500);
    }

    @AfterClass
    @Override
    public void teardown() {
        this.kafka.teardown();
        teardownLogging();
        super.teardown();
    }

    private void generateKafkaEvents(String format) {
        generateKafkaEvents(Collections.emptyList(), format);
    }

    private void generateKafkaEvents(Collection<Header> headers, String format) {
        try (KafkaSink<String, String> sink = KafkaSink.<String, String>create()
                                                       .keySerializer(StringSerializer.class)
                                                       .valueSerializer(StringSerializer.class)
                                                       .bootstrapServers(this.kafka.getBootstrapServers())
                                                       .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                       .producerConfig(this.kafka.getClientProperties())
                                                       .lingerMs(5)
                                                       .build()) {
            for (int i = 1; i <= 1_000; i++) {
                sink.send(new SimpleEvent<>(headers, Integer.toString(i),
                                            String.format(format, i)));
            }
        }
    }


    private void runDumpCommand(String dump, String... extraArgs) {
        List<String> args = new ArrayList<>(List.of(dump,
                                                    "--bootstrap-servers",
                                                    this.kafka.getBootstrapServers(),
                                                    "--topic",
                                                    KafkaTestCluster.DEFAULT_TOPIC,
                                                    "--kafka-properties",
                                                    CLIENT_PROPERTIES_FILE,
                                                    "--max-stalls",
                                                    "1",
                                                    "--poll-timeout",
                                                    "5",
                                                    "--read-policy",
                                                    "BEGINNING",
                                                    "--no-live-reporter",
                                                    "--no-health-probes"));
        if (extraArgs != null && extraArgs.length > 0) {
            args.addAll(Arrays.asList(extraArgs));
        }
        DebugCli.main(args.toArray(new String[0]));

    }

    @Test
    public void givenEmptyTopic_whenDumpingEvents_thenNothingDumped() {
        // Given and When
        runDumpCommand("dump");

        // Then
        AbstractDockerDebugCliHelper.verifyDumpCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNonEmptyTopic_whenDumpingEvents_thenEventsAreDumped() {
        // Given
        generateKafkaEvents("Event %,d");

        // When
        runDumpCommand("dump");

        // Then
        AbstractDockerDebugCliHelper.verifyDumpCommandUsed();
        AbstractDockerDebugCliHelper.verifyEvents("Event %,d");
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNonEmptyTopic_whenDumpingRdfEvents_thenEventsAreDumped() {
        // Given
        generateKafkaEvents("<http://subject> <http://predicate> \"%d\" .");

        // When
        runDumpCommand("rdf-dump");

        // Then
        AbstractDockerDebugCliHelper.verifyRdfDumpCommandUsed();
        AbstractDockerDebugCliHelper.verifyEvents("\"%d\"");
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNonEmptyTopic_whenDumpingRdfEvents_thenEventsAreDumped_andLiveReporterHeartbeatsAreGenerated() {
        // Given
        generateKafkaEvents("<http://subject> <http://predicate> \"%d\" .");

        // When
        runDumpCommand("rdf-dump", "--live-reporter");

        // Then
        AbstractDockerDebugCliHelper.verifyRdfDumpCommandUsed();
        AbstractDockerDebugCliHelper.verifyEvents("\"%d\"");

        // And
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "LiveReporter"));
    }
}
