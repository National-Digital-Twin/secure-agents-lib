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
package uk.gov.dbt.ndtp.secure.agent.projectors.driver;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.NoOpProjector;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.Sinks;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.InMemoryEventSource;

public class TestProjectorDriverBuilder {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".* cannot be null")
    public void driver_builder_bad_01() {
        ProjectorDriver.create().build();
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".* cannot be null")
    public void driver_builder_bad_02() {
        ProjectorDriver.create().source(new InMemoryEventSource<>(Collections.emptyList())).build();
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = ".* cannot be null")
    public void driver_builder_bad_03() {
        ProjectorDriver.<Object, Object, Event<Object, Object>>create()
                       .source(new InMemoryEventSource<>(Collections.emptyList()))
                       .projector(new NoOpProjector<>())
                       .build();
    }

    @Test
    public void driver_builder_01() {
        // Poll timeouts have defaults set
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(new InMemoryEventSource<>(Collections.emptyList()))
                               .projector(new NoOpProjector<>())
                               .destination(NullSink.of())
                               .build();
        Assert.assertNotNull(driver);
    }

    @Test
    public void driver_builder_02() {
        // Set poll timeouts in various ways
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(new InMemoryEventSource<>(Collections.emptyList()))
                               .projector(new NoOpProjector<>())
                               .destination(NullSink.of())
                               .pollTimeout(Duration.ofSeconds(5))
                               .pollTimeout(5, ChronoUnit.SECONDS)
                               .build();
        Assert.assertNotNull(driver);
    }

    @Test
    public void driver_builder_03() {
        // Set limits in various ways
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(new InMemoryEventSource<>(Collections.emptyList()))
                               .projector(new NoOpProjector<>())
                               .destination(NullSink.of())
                               .unlimited()
                               .limit(100)
                               .unlimitedStalls()
                               .maxStalls(3)
                               .reportBatchSize(100)
                               .build();
        Assert.assertNotNull(driver);
    }

    @Test
    public void driver_builder_04() {
        // Supply destination sink in various ways
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(new InMemoryEventSource<>(Collections.emptyList()))
                               .projector(new NoOpProjector<>())
                               .destination(NullSink.of())
                               .destinationBuilder(Sinks.discard())
                               .destination(NullSink::of)
                               .build();
        Assert.assertNotNull(driver);
    }

}
