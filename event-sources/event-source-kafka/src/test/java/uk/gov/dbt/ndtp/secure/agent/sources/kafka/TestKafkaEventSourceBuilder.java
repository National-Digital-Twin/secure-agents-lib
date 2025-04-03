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

import java.time.Duration;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;

public class TestKafkaEventSourceBuilder {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*bootstrapServers cannot be null")
    public void kafka_builder_bad_01() {
        KafkaEventSource.create().build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*topic.* cannot be null")
    public void kafka_builder_bad_02() {
        KafkaEventSource.create().bootstrapServers("localhost:9092").build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*groupID cannot be null")
    public void kafka_builder_bad_03() {
        KafkaEventSource.create().bootstrapServers("localhost:9092").topic("test").build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*keyDeserializerClass cannot be null")
    public void kafka_builder_bad_04() {
        KafkaEventSource.create().bootstrapServers("localhost:9092").topic("test").consumerGroup("consumer").build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*valueDeserializerClass cannot be null")
    public void kafka_builder_bad_05() {
        KafkaEventSource.create()
                        .bootstrapServers("localhost:9092")
                        .topic("test")
                        .consumerGroup("consumer")
                        .keyDeserializer(
                                BytesDeserializer.class)
                        .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*maxPollRecords.*>= 1")
    public void kafka_builder_bad_06() {
        KafkaEventSource.create()
                        .bootstrapServers("localhost:9092")
                        .topic("test")
                        .consumerGroup("consumer")
                        .keyDeserializer(BytesDeserializer.class)
                        .valueDeserializer(BytesDeserializer.class)
                        .maxPollRecords(-1)
                        .build();
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".*readPolicy cannot be null")
    public void kafka_builder_bad_07() {
        KafkaEventSource.create()
                        .bootstrapServers("localhost:9092")
                        .topic("test")
                        .consumerGroup("consumer")
                        .keyDeserializer(BytesDeserializer.class)
                        .valueDeserializer(BytesDeserializer.class)
                        .maxPollRecords(10)
                        .readPolicy(null)
                        .build();
    }

    @Test
    public void kafka_builder_01() {
        KafkaEventSource<Bytes, Bytes> source
                = KafkaEventSource.<Bytes, Bytes>create()
                                  .bootstrapServers("localhost:9092")
                                  .topic("test")
                                  .consumerGroup("consumer")
                                  .keyDeserializer(BytesDeserializer.class)
                                  .valueDeserializer(BytesDeserializer.class)
                                  .maxPollRecords(1000)
                                  .build();
        Assert.assertNotNull(source);
        Assert.assertNull(source.remaining());
        Assert.assertEquals(source.toString(), "localhost:9092/test");
    }

    @Test
    public void kafka_builder_02() {
        KafkaEventSource<Bytes, DatasetGraph> source
                = KafkaDatasetGraphSource.<Bytes>createGraph()
                                         .bootstrapServers("localhost:9092")
                                         .topic("test")
                                         .consumerGroup("consumer")
                                         .keyDeserializer(BytesDeserializer.class)
                                         .maxPollRecords(1000)
                                         .build();
        Assert.assertNotNull(source);
        Assert.assertNull(source.remaining());
        Assert.assertEquals(source.toString(), "localhost:9092/test");
    }

    @Test
    public void kafka_builder_03() {
        KafkaEventSource<Bytes, Bytes> source
                = KafkaEventSource.<Bytes, Bytes>create()
                                  .bootstrapServers("localhost:9092")
                                  .topic("test")
                                  .consumerGroup("consumer")
                                  .keyDeserializer(BytesDeserializer.class)
                                  .valueDeserializer(BytesDeserializer.class)
                                  .maxPollRecords(1000)
                                  .lagReportInterval(Duration.ofSeconds(30))
                                  .fromBeginning()
                                  .fromEarliest()
                                  .fromLatest()
                                  .fromEnd()
                                  .readPolicy(KafkaReadPolicies.manualFromBeginning())
                                  .autoCommit()
                                  .autoCommit(false)
                                  .commitOnProcessed()
                                  .build();
        Assert.assertNotNull(source);
        Assert.assertNull(source.remaining());
        Assert.assertEquals(source.toString(), "localhost:9092/test");
    }

    @Test
    public void kafka_builder_04() {
        KafkaEventSource<Bytes, Bytes> source
                = KafkaEventSource.<Bytes, Bytes>create()
                                  .bootstrapServers("localhost:9092", "localhost:9093", "localhost:9094")
                                  .topic("test")
                                  .consumerGroup("consumer")
                                  .keyDeserializer(BytesDeserializer.class)
                                  .valueDeserializer(BytesDeserializer.class)
                                  .maxPollRecords(1000)
                                  .build();
        Assert.assertNotNull(source);
        Assert.assertNull(source.remaining());
        Assert.assertEquals(source.toString(), "localhost:9092,localhost:9093,localhost:9094/test");
    }
}
