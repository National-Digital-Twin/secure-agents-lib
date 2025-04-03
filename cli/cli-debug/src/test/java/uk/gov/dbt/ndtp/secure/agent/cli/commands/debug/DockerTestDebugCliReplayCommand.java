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
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlFormat;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.FlakyKafkaRetryAnalyzerHelper;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;

public class DockerTestDebugCliReplayCommand extends AbstractDockerDebugCliHelper {

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenEmptyCaptureDirectory_whenRunningReplayCommand_thenNothingIsReplayed() throws
            IOException {
        // Given
        File captureDir = Files.createTempDirectory("capture").toFile();

        // When
        DebugCli.main(new String[] {
                "replay",
                "--source-directory",
                captureDir.getAbsolutePath(),
                "--bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--no-health-probes"
        });

        // Then
        verifyReplayCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "all events have been exhausted"));
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenNonEmptyCaptureDirectory_whenRunningReplayCommand_thenEventsAreReplayed() throws IOException {
        // Given
        File captureDir = Files.createTempDirectory("capture").toFile();
        generateCapturedEvents(captureDir, new YamlFormat(), Collections.emptyList(), "Event %,d");

        // When
        DebugCli.main(new String[] {
                "replay",
                "--source-directory",
                captureDir.getAbsolutePath(),
                "--source-format",
                YamlFormat.EVENT_FORMAT_NAME,
                "--bootstrap-servers",
                this.kafka.getBootstrapServers(),
                "--topic",
                KafkaTestCluster.DEFAULT_TOPIC,
                "--max-stalls",
                "1",
                "--poll-timeout",
                "3",
                "--no-health-probes"
        });

        // Then
        verifyReplayCommandUsed();
        verifyReplayedEvents("Event %,d");
    }
}
