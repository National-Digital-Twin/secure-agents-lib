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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestNullSink extends AbstractSinkHelper {

    @Test
    public void givenNoItems_whenSendingToNull_thenCountIsZero() {
        // Given, When and Then
        verifyNullSink(Collections.emptyList());
    }

    @Test
    public void givenItems_whenSendingToNull_thenCountIsCorrect() {
        // Given, When and Then
        verifyNullSink(Arrays.asList("a", "b", "c"));
    }

    protected void verifyNullSink(List<String> values) {
        // When
        try (NullSink<String> sink = new NullSink<>()) {
            values.forEach(sink::send);

            // Then
            Assert.assertEquals(sink.count(), values.size());

            // And
            // After close() the counter should be reset
            sink.close();
            Assert.assertEquals(sink.count(), 0);
        }
    }
}
