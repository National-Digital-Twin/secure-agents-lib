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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.projection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.AbstractCommandHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.BasicKafkaTestCluster;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;

public class DockerTestDeadLetterQueue extends AbstractCommandHelper {

    public static final String DEAD_LETTER_TOPIC = "dead-letters";
    private static final int TEST_DATA_SIZE = 1_000;

    /**
     * Intentionally protected so we can extend this test class and run it against different test clusters
     */
    protected KafkaTestCluster kafka = new BasicKafkaTestCluster();

    @BeforeClass
    @Override
    public void setup() {
        this.kafka.setup();
        this.kafka.resetTopic(DEAD_LETTER_TOPIC);
        generateKafkaEvents(Collections.emptyList(), "Example message %,d");

        super.setup();
    }

    @AfterMethod
    @Override
    public void testCleanup() throws InterruptedException {
        super.testCleanup();

        this.kafka.resetTopic(DEAD_LETTER_TOPIC);
    }

    @AfterClass
    @Override
    public void teardown() {
        this.kafka.teardown();

        super.teardown();
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
            for (int i = 1; i <= TEST_DATA_SIZE; i++) {
                sink.send(new SimpleEvent<>(headers, Integer.toString(i), String.format(format, i)));
            }
        }
    }

    private void verifyDeadLetters(int expected) {
        KafkaEventSource<Bytes, Bytes> deadLetters = KafkaEventSource.<Bytes, Bytes>create()
                                                                     .bootstrapServers(this.kafka.getBootstrapServers())
                                                                     .topic(DEAD_LETTER_TOPIC)
                                                                     .consumerGroup(DEAD_LETTER_TOPIC)
                                                                     .readPolicy(KafkaReadPolicies.fromBeginning())
                                                                     .keyDeserializer(BytesDeserializer.class)
                                                                     .valueDeserializer(BytesDeserializer.class)
                                                                     .consumerConfig(this.kafka.getClientProperties())
                                                                     .build();
        try {
            int found = 0;
            while (true) {
                Event<Bytes, Bytes> event = deadLetters.poll(Duration.ofSeconds(3));
                if (event == null) {
                    break;
                }
                found++;
            }

            Assert.assertEquals(found, expected);
        } finally {
            deadLetters.close();
        }
    }

    protected void runCommand(Class<? extends SecureAgentCommand> commandClass, String deadLetterTopic,
                              Integer deadLetterFrequency) throws IOException {
        List<String> args = new ArrayList<>();
        //@formatter:off
        CollectionUtils.addAll(args,
                               "--bootstrap-servers",
                               this.kafka.getBootstrapServers(),
                               "--topic",
                               KafkaTestCluster.DEFAULT_TOPIC,
                               "--max-stalls",
                               "1",
                               "--poll-timeout",
                               "5",
                               "--read-policy",
                               "BEGINNING");
        if (StringUtils.isNotBlank(deadLetterTopic)) {
            CollectionUtils.addAll(args, "--dlq-topic", deadLetterTopic);
        }
        if (deadLetterFrequency != null) {
            CollectionUtils.addAll(args, "--dead-letter-frequency", Integer.toString(deadLetterFrequency));
        }

        // If there are Kafka Properties needed pass those in via a temporary file
        Properties properties = this.kafka.getClientProperties();
        File configFile = null;
        if (!properties.isEmpty()) {
            configFile = Files.createTempFile("kafka", ".properties").toFile();
            try (FileOutputStream output = new FileOutputStream(configFile)) {
                properties.store(output, null);
            }
            CollectionUtils.addAll(args,"--kafka-properties", configFile.getAbsolutePath());
        }

        SecureAgentCommand.runAsSingleCommand(commandClass, args.toArray(new String[0]));

        if (configFile != null) {
            configFile.delete();
        }
    }


    @Test
    public void givenCommandWithNoDLQ_whenProjecting_thenNoDeadLetters()throws IOException {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, null, null);

        // Then
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(0);
    }

    @Test
    public void givenCommandWithDLQConfigured_whenProjecting_thenDeadLettersAreCreated()throws IOException {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, DEAD_LETTER_TOPIC, null);

        // Then
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(TEST_DATA_SIZE / 10);
    }

    @Test
    public void givenTransformingCommandWithDLQConfigured_whenProjecting_thenDeadLettersAreCreated()throws IOException {
        // Given
        // Data generated once in setup()

        // When
        runCommand(TransformingProjectorCommand.class, DEAD_LETTER_TOPIC, null);

        // Then
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(TEST_DATA_SIZE / 10);
    }

    @Test
    public void givenBadTransformingCommandWithDLQConfigured_whenProjecting_thenFails()throws IOException {
        // Given
        // Data generated once in setup()

        // When
        runCommand(BadTransformingProjectorCommand.class, DEAD_LETTER_TOPIC, null);

        // Then
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 1);
        String stdErr = SecureAgentCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "ClassCastException"));
    }

    @Test
    public void givenCommandWithDLQForEverything_whenProjecting_thenEverythingIsDeadLettered()throws IOException {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, DEAD_LETTER_TOPIC, 1);

        // Then
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(TEST_DATA_SIZE);
    }

    @Test
    public void givenCommandWithDLQForNothing_whenProjecting_thenNothingIsDeadLettered()throws IOException {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, DEAD_LETTER_TOPIC, 100_000);

        // Then
        Assert.assertEquals(SecureAgentCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(0);
    }
}
