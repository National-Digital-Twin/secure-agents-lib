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
package uk.gov.dbt.ndtp.secure.agent.sources;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.InMemoryEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestCapturingEventSource extends AbstractEventSourceTests<Integer, String> {
    @Override
    protected EventSource<Integer, String> createEmptySource() {
        return new CapturingEventSource<>(new InMemoryEventSource<>(Collections.emptyList()), NullSink.of());
    }

    @Override
    protected EventSource<Integer, String> createSource(Collection<Event<Integer, String>> events) {
        return new CapturingEventSource<>(new InMemoryEventSource<>(events), NullSink.of());
    }

    @Override
    protected Collection<Event<Integer, String>> createSampleData(int size) {
        AtomicInteger counter = new AtomicInteger(0);
        return createSampleStrings(size).stream()
                                        .map(s -> new SimpleEvent<>(Collections.emptyList(), counter.incrementAndGet(),
                                                                    s, new CapturingEventSource(
                                                new InMemoryEventSource(Collections.emptyList()), NullSink.of())))
                                        .collect(Collectors.toList());
    }

    @Override
    public boolean guaranteesImmediateAvailability() {
        return true;
    }

    @Override
    public boolean isUnbounded() {
        return false;
    }
}
