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

import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.FlakyKafkaRetryAnalyzerHelper;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;

public class DockerTestDebugRdfDumpCommand extends AbstractDockerDebugCliHelper {

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenInputEvents_whenRunningRdfDumpCommand_thenEventsAreDumped()  {
        // Given
        generateKafkaEvents("<http://subject> <http://predicate> \"%d\" .");

        // When
        DebugCli.main(new String[] {
                "rdf-dump",
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
        verifyRdfDumpCommandUsed();
        verifyEvents("\"%d\"");
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenInputEventsAndBadOutputLanguage_whenRunningRdfDumpCommand_thenEventsAreDumpedInDefaultFormat() {
        // Given
        generateKafkaEvents("<http://subject> <http://predicate> \"%d\" .");

        // When
        DebugCli.main(new String[] {
                "rdf-dump",
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
                "--output-language",
                "no-such-language",
                "--no-health-probes"
        });

        // Then
        verifyRdfDumpCommandUsed();
        verifyEvents("\"%d\"");
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenRdfPatchInputs_whenRunningRdfDumpCommand_thenEventsAreDumped() {
        // Given
        generateKafkaEvents(List.of(new Header(HttpNames.hContentType, WebContent.ctPatch.getContentTypeStr())),
                            "A <http://subject> <http://predicate> \"%d\" .");

        // When
        DebugCli.main(new String[] {
                "rdf-dump",
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
        verifyRdfDumpCommandUsed();
        verifyEvents("\"%d\"");
    }

    @Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
    public void givenMalformedRdfEvents_whenDumpingRdf_thenMalformedEventsAreNoted_andKafkaOffsetsAreUpdated() {
        // Given
        // Add some sample malformed events to Kafka
        generateKafkaEvents("<http://malformed> \"%d\" .");
        String consumerGroup = "no-head-of-line-blocking";

        // When
        DebugCli.main(new String[] {
                "rdf-dump",
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
                "--group",
                consumerGroup,
                "--no-health-probes"
        });

        // Then
        verifyRdfDumpCommandUsed();
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "Ignored malformed RDF event"));
        Assert.assertEquals(StringUtils.countMatches(stdErr, "Ignored malformed RDF event"), 1_000);

        // And
        KafkaEventSource<Bytes, Bytes> source = KafkaEventSource.<Bytes, Bytes>create()
                                                                .bootstrapServers(this.kafka.getBootstrapServers())
                                                                .consumerGroup(consumerGroup)
                                                                .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                                .keyDeserializer(BytesDeserializer.class)
                                                                .valueDeserializer(BytesDeserializer.class)
                                                                .build();
        try {
            Assert.assertNull(source.poll(Duration.ofSeconds(1)),
                              "Consumer Group should be at end of topic despite malformed RDF events");
        } finally {
            source.close();
        }
    }
}
