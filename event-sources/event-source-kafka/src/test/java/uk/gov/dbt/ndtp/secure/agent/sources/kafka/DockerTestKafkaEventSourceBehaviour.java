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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.AbstractEventSourceTests;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

@Test(retryAnalyzer = FlakyKafkaRetryAnalyzerHelper.class)
public class DockerTestKafkaEventSourceBehaviour extends AbstractEventSourceTests<Integer, String> {

    private final AtomicInteger consumerGroupId = new AtomicInteger(0);
    private final KafkaTestCluster kafka = new BasicKafkaTestCluster();

    @BeforeClass
    public void setup() {
        Utils.logTestClassStarted(DockerTestKafkaEventSourceBehaviour.class);
        this.kafka.setup();
    }

    @AfterMethod
    public void testCleanup() throws InterruptedException {
        this.kafka.resetTestTopic();
        Thread.sleep(500);
    }

    @AfterClass
    public void teardown() {
        this.kafka.teardown();
        Utils.logTestClassFinished(DockerTestKafkaEventSourceBehaviour.class);
    }

    @Override
    protected EventSource<Integer, String> createEmptySource() {
        return KafkaEventSource.<Integer, String>create()
                               .fromBeginning()
                               .topic(KafkaTestCluster.DEFAULT_TOPIC)
                               .bootstrapServers(this.kafka.getBootstrapServers())
                               .autoCommit()
                               .consumerGroup("behaviour-tests-" + this.consumerGroupId.incrementAndGet())
                               .keyDeserializer(IntegerDeserializer.class)
                               .valueDeserializer(StringDeserializer.class)
                               .build();
    }

    @Override
    protected EventSource<Integer, String> createSource(Collection<Event<Integer, String>> events) {
        try (KafkaSink<Integer, String> sink = KafkaSink.<Integer, String>create()
                                                        .keySerializer(IntegerSerializer.class)
                                                        .valueSerializer(StringSerializer.class)
                                                        .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                        .bootstrapServers(this.kafka.getBootstrapServers()).build()) {
            for (Event<Integer, String> event : events) {
                sink.send(event);
            }
        }

        return createEmptySource();
    }

    @Override
    protected Collection<Event<Integer, String>> createSampleData(int size) {
        AtomicInteger counter = new AtomicInteger(0);
        return createSampleStrings(size).stream()
                                        .map(s -> new SimpleEvent<>(Collections.emptyList(), counter.getAndIncrement(),
                                                                    s)).collect(Collectors.toList());
    }

    @Override
    public boolean guaranteesImmediateAvailability() {
        return false;
    }

    @Override
    public boolean isUnbounded() {
        return true;
    }

    @DataProvider(name = "sample-data-sizes")
    @Override
    public Object[][] getTestSizes() {
        // NB - Tone down the test data sizes when running against a Kafka Test cluster as the large test data sizes
        //      our base class uses take too long to bring up and tear down with Kafka
        return new Object[][] {
                { 100 },
                { 2_500 },
                { 10_000 }
        };
    }

}
