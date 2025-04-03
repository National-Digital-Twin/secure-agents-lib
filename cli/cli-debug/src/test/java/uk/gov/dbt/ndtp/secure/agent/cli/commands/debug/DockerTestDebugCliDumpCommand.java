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
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.FlakyKafkaRetryAnalyzerHelper;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.file.YamlOffsetStore;

public class DockerTestDebugCliDumpCommand extends AbstractDockerDebugCliHelper {

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenEmptyInput_whenRunningDumpCommand_thenNothingIsDumped() {
        // Given
        // No inputs

        // When
        DebugCli.main(new String[] {
                "dump",
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
                "--no-health-probes"
        });

        // Then
        verifyDumpCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
        Assert.assertFalse(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenInputs_whenRunningDumpCommand_thenEventsAreDumped() {
        // Given
        generateKafkaEvents("Event %,d");

        // When
        DebugCli.main(new String[] {
                "dump",
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
                "--no-health-probes"
        });

        // Then
        verifyDumpCommandUsed();
        verifyEvents("Event %,d");
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Currently no new events available"));
        Assert.assertFalse(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNoInputsAndNonKafkaSource_whenRunningDumpCommand_thenNothingIsDumped_andNoLiveHeartbeats() {
        // Given
        // No input events, source directory that does not contain any events

        // When
        DebugCli.main(new String[] {
                "dump",
                "--source-directory",
                "target",
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING",
                "--no-health-probes"
        });

        // Then
        verifyDumpCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "all events have been exhausted"));

        // And
        Assert.assertTrue(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
        verifyHeartbeats(false);
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNoInputsAndLiveHeartbeatsEnabled_whenRunningDumpCommand_thenNothingIsDumped_andLiveHeartbeatsAreProduced() {
        // Given
        // No input events, source directory that does not contain any events

        // When
        DebugCli.main(new String[] {
                "dump",
                "--source-directory",
                "target",
                "--live-bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--read-policy",
                "BEGINNING",
                "--no-health-probes"
        });

        // Then
        verifyDumpCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "all events have been exhausted"));

        // And
        Assert.assertFalse(StringUtils.contains(stdErr, "live heartbeats are not being reported anywhere"));
        Assert.assertTrue(StringUtils.contains(stdErr, "Background Live Reporter thread started"));
        verifyHeartbeats(true);
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenOffsetsFile_whenDumpingTopic_thenOffsetsAreStoredInFile() throws IOException {
        // Given
        generateKafkaEvents("Event %,d");
        File offsetsFile = File.createTempFile("offsets", ".test");

        // When
        DebugCli.main(new String[] {
                "dump",
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
                "--no-live-reporter",
                "--no-health-probes",
                // Specify our offsets file
                "--offsets-file",
                offsetsFile.getAbsolutePath(),
                // Read at most 10 events so that we'll have a predictable offset written to our offsets file
                "--limit",
                "10"
        });

        // Then
        verifyDumpCommandUsed();
        YamlOffsetStore store = new YamlOffsetStore(offsetsFile);
        Assert.assertEquals(store.<Long>loadOffset(
                KafkaEventSource.externalOffsetStoreKey(KafkaTestCluster.DEFAULT_TOPIC, 0, "dump")), 10L);
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "no persistent offsets"));
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenUnusableOffsetsFile_whenDumpingTopic_thenOffsetsAreNotStored() throws IOException {
        // Given
        generateKafkaEvents("Event %,d");
        File offsetsFile = new File("/no/such/path/to/unusable/offsets.file");

        // When
        DebugCli.main(new String[] {
                "dump",
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
                "--no-live-reporter",
                "--no-health-probes",
                // Specify our offsets file
                "--offsets-file",
                offsetsFile.getAbsolutePath(),
                // Read at most 10 events so that we'll have a predictable offset written to our offsets file
                "--limit",
                "10"
        });

        // Then
        verifyDumpCommandUsed();
        YamlOffsetStore store = new YamlOffsetStore(offsetsFile);
        Assert.assertNull(
                store.loadOffset(KafkaEventSource.externalOffsetStoreKey(KafkaTestCluster.DEFAULT_TOPIC, 0, "dump")));
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "failed to store offsets"));
    }
}
