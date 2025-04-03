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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.jena.sparql.core.DatasetGraph;
import uk.gov.dbt.ndtp.secure.agent.payloads.RdfPayload;
import uk.gov.dbt.ndtp.secure.agent.sources.AbstractEventSourceTests;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.policies.KafkaReadPolicies;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.TestDatasetDeserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers.TestPayloadDeserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestKafkaRdfPayloadSource extends AbstractEventSourceTests<Integer, RdfPayload> {

    public static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String TEST_TOPIC = "test";
    public static final String TEST_GROUP = "test-group";

    @Override
    protected EventSource<Integer, RdfPayload> createEmptySource() {
        return createSource(Collections.emptyList());
    }

    @Override
    protected EventSource<Integer, RdfPayload> createSource(Collection<Event<Integer, RdfPayload>> events) {
        return new MockKafkaRdfPayloadSource(DEFAULT_BOOTSTRAP_SERVERS, Set.of(TEST_TOPIC), TEST_GROUP, 100,
                                             KafkaReadPolicies.fromBeginning(), true, events);
    }

    @Override
    protected Collection<Event<Integer, RdfPayload>> createSampleData(int size) {
        List<RdfPayload> payloads = new ArrayList<>();
        DatasetGraph g = TestDatasetDeserializer.createTestDataset(2, 100);
        for (int i = 0; i < size; i++) {
            payloads.add(RdfPayload.of(TestPayloadDeserializer.datasetToPatch(g)));
        }
        AtomicInteger counter = new AtomicInteger(0);
        return payloads.stream()
                       .map(graph -> new SimpleEvent<>(Collections.emptyList(), counter.incrementAndGet(),
                                                       graph))
                       .collect(Collectors.toList());
    }

    @Override
    public boolean guaranteesImmediateAvailability() {
        return false;
    }

    @Override
    public boolean isUnbounded() {
        return true;
    }
}
