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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.sources.file.rdf.RdfFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.file.text.PlainTextFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.FlakyKafkaRetryAnalyzerHelper;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;

public class DockerTestDebugCliCaptureCommand extends AbstractDockerDebugCliHelper {

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenCaptureDirectoryAndEmptyInput_whenRunningCaptureCommand_thenNothingIsCaptured() throws
            IOException {
        // Given
        File captureDir = Files.createTempDirectory("capture").toFile();

        // When
        DebugCli.main(new String[] {
                "capture",
                "--bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING",
                "--capture-dir",
                captureDir.getAbsolutePath(),
                "--no-health-probes"
        });

        // Then
        verifyCaptureCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        // Expecting a DEBUG statement from KafkaEventSource
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
        // Expecting NOT to receive a WARN statement from LiveReporterOptions
        Assert.assertFalse(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        // Expecting an INFO statement from LiveReporter
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
        // Expect no captured events
        File[] files = captureDir.listFiles();
        Assert.assertNotNull(files);
        Assert.assertEquals(files.length, 0, "Expected no events to be captured");
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenInputsAndCaptureDirectory_whenRunningCaptureCommand_thenEventsAreCaptured() throws IOException {
        // Given
        generateKafkaEvents("Event %,d");
        File captureDir = Files.createTempDirectory("capture").toFile();

        // When
        DebugCli.main(new String[] {
                "capture",
                "--bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING",
                "--capture-dir",
                captureDir.getAbsolutePath(),
                "--no-health-probes"
        });

        // Then
        verifyCaptureCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        // Expecting a DEBUG statement from KafkaEventSource
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
        // Expecting NOT to receive a WARN statement from LiveReporterOptions
        Assert.assertFalse(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        // Expecting an INFO statement from LiveReporter
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
        // Verify capture
        verifyCapturedEvents(captureDir, YamlFormat.EVENT_FORMAT_NAME, "Event %,d");
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenInputsAndCaptureDirectoryAndCaptureFormat_whenRunningCaptureCommand_thenEventsAreCapturedInCorrectFormat() throws
            IOException {
        // Given
        generateKafkaEvents("Event %,d");
        File captureDir = Files.createTempDirectory("capture").toFile();

        // When
        DebugCli.main(new String[] {
                "capture",
                "--bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING",
                "--capture-dir",
                captureDir.getAbsolutePath(),
                "--capture-format",
                PlainTextFormat.EVENT_FORMAT_NAME,
                "--no-health-probes"
        });

        // Then
        verifyCaptureCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        // Expecting a DEBUG statement from KafkaEventSource
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
        // Expecting NOT to receive a WARN statement from LiveReporterOptions
        Assert.assertFalse(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        // Expecting an INFO statement from LiveReporter
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
        // Verify capture
        verifyCapturedEvents(captureDir, PlainTextFormat.EVENT_FORMAT_NAME, "Event %,d");
    }

    @Test//(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenInputsAndCaptureDirectoryAndRdfFormat_whenRunningCaptureCommand_thenEventsAreCapturedInRdfFormat() throws IOException {
        // Given
        generateKafkaEvents("<http://subject> <http://predicate> \"%d\" .");
        File captureDir = Files.createTempDirectory("capture").toFile();

        // When
        DebugCli.main(new String[] {
                "capture",
                "--bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING",
                "--capture-dir",
                captureDir.getAbsolutePath(),
                "--capture-format",
                RdfFormat.EVENT_FORMAT_NAME,
                "--no-health-probes"
        });

        // Then
        verifyCaptureCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        // Expecting a DEBUG statement from KafkaEventSource
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
        // Expecting NOT to receive a WARN statement from LiveReporterOptions
        Assert.assertFalse(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        // Expecting an INFO statement from LiveReporter
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
        // Verify capture
        verifyCapturedEvents(captureDir, RdfFormat.EVENT_FORMAT_NAME, "<http://subject> <http://predicate> \"%d\" .");
    }
}
