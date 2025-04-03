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
package uk.gov.dbt.ndtp.secure.agent.live;

import java.time.Duration;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveHeartbeat;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveStatus;
import uk.gov.dbt.ndtp.secure.agent.live.serializers.LiveHeartbeatDeserializer;
import uk.gov.dbt.ndtp.secure.agent.live.serializers.LiveHeartbeatSerializer;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.KafkaTestCluster;

public class DockerTestLiveReporter extends AbstractDockerLiveTests<LiveHeartbeat> {

    @DataProvider(name = "stopStatuses")
    public Object[][] stopStatuses() {
        return new Object[][] {
                { LiveStatus.COMPLETED },
                { LiveStatus.TERMINATED },
                { LiveStatus.ERRORING }
        };
    }

    @Test(dataProvider = "stopStatuses")
    public void live_reporter_kafka_01(LiveStatus stopStatus) throws InterruptedException {
        LiveReporter reporter = new LiveReporter(createSink(), Duration.ofMillis(100), "test-01", "Test 01", "mapper",
                                                 new IODescriptor("input.txt", "file"),
                                                 new IODescriptor("raw", "topic"));
        reporter.start();
        Thread.sleep(1000);
        reporter.stop(stopStatus);
        verifyHeartbeats(stopStatus);
    }

    private void verifyHeartbeats(LiveStatus stopStatus) {
        //@formatter:off
        KafkaEventSource<Bytes, LiveHeartbeat> source
            = KafkaEventSource.<Bytes, LiveHeartbeat>create()
                              .bootstrapServers(this.kafka.getBootstrapServers())
                              .fromBeginning()
                              .topic(KafkaTestCluster.DEFAULT_TOPIC)
                              .consumerGroup("heartbeat-reporter")
                              .autoCommit()
                              .keyDeserializer(BytesDeserializer.class)
                              .valueDeserializer(LiveHeartbeatDeserializer.class)
                              .build();
        //@formatter:on

        Event<Bytes, LiveHeartbeat> event;
        LiveHeartbeat heartbeat = null;
        boolean first = true;
        do {
            event = source.poll(Duration.ofSeconds(3));
            heartbeat = event != null ? event.value() : heartbeat;
            Assert.assertNotNull(heartbeat);
            if (first) {
                // First heartbeat should be the start status
                Assert.assertEquals(heartbeat.getStatus(), LiveStatus.STARTED);
                first = false;
            } else if (source.availableImmediately()) {
                // Intermediate heartbeats should be running status
                Assert.assertEquals(heartbeat.getStatus(), LiveStatus.RUNNING);
            } else {
                // Final heartbeat should be the stop status
                Assert.assertEquals(heartbeat.getStatus(), stopStatus);
            }
        } while (event != null);
        source.close();
    }

    @Test(dataProvider = "stopStatuses")
    public void live_reporter_kafka_02(LiveStatus stopStatus) throws InterruptedException {
        LiveReporter reporter = LiveReporter.create()
                                            .toKafka(k -> k.bootstrapServers(this.kafka.getBootstrapServers())
                                                           .topic(KafkaTestCluster.DEFAULT_TOPIC))
                                            .reportingPeriod(Duration.ofMillis(100))
                                            .id("test-02")
                                            .name("Test 02")
                                            .componentType("mapper")
                                            .input("raw", "topic")
                                            .output("clean", "topic")
                                            .build();

        reporter.start();
        Thread.sleep(1000);
        reporter.stop(stopStatus);
        verifyHeartbeats(stopStatus);
    }

    @Override
    protected Class<? extends Serializer<LiveHeartbeat>> getSerializerClass() {
        return LiveHeartbeatSerializer.class;
    }
}
