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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events;

import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.CollectorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.Sinks;

public class TestEventValueSink extends AbstractEventSinkCommonMethods {

    @Test
    public void givenSink_whenSendingEvents_thenOnlyValuesAreOutput() {
        // Given
        CollectorSink<String> collector = CollectorSink.of();
        try (EventValueSink<String, String> sink = new EventValueSink<>(collector)) {
            // When
            sendTestEvents(sink);

            // Then
            List<String> actual = collector.get();
            Assert.assertEquals(actual.size(), KEYS.size());
            Assert.assertNotEquals(actual, KEYS);
        }
    }

    @Test
    public void givenBuilder_whenBuilding_thenOK() {
        // Given and When
        try (EventValueSink<String, String> sink = EventValueSink.<String, String>create()
                                                                 .destination(Sinks.<String>collect().build())
                                                                 .build()) {
            // Then
            Assert.assertNotNull(sink);
        }
    }
}
