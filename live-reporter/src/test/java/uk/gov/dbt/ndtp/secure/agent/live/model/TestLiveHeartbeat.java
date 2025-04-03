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
package uk.gov.dbt.ndtp.secure.agent.live.model;

import java.util.Date;
import java.time.Instant;
import java.util.UUID;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLiveHeartbeat {

    @Test
    public void live_heartbeat_equality_01() {
        LiveHeartbeat heartbeat =
                new LiveHeartbeat("equality-01", UUID.randomUUID().toString(), "Equality Test", "adapter",
                                  Date.from(Instant.now()), 15, new IODescriptor("input.txt", "file"),
                                  new IODescriptor("output-topic", "topic"), LiveStatus.STARTED);
        Assert.assertTrue(heartbeat == heartbeat);
        Assert.assertFalse(heartbeat == null);
        Assert.assertTrue(heartbeat.equals(heartbeat));
        Assert.assertFalse(heartbeat.equals(null));
        Assert.assertFalse(heartbeat.equals(new Object()));
    }

    @Test
    public void live_heartbeat_equality_02() {
        LiveHeartbeat a =
                new LiveHeartbeat("equality-02", UUID.randomUUID().toString(), "Equality Test", "adapter",
                                  Date.from(Instant.now()), 15, new IODescriptor("input.txt", "file"),
                                  new IODescriptor("output-topic", "topic"), LiveStatus.STARTED);
        LiveHeartbeat b = a.copy();

        verifyEquality(a, b);
    }

    @Test
    public void live_heartbeat_equality_03() {
        LiveHeartbeat a =
                new LiveHeartbeat("equality-03", UUID.randomUUID().toString(), "Equality Test", "adapter",
                                  Date.from(Instant.now()), 15, new IODescriptor("input.txt", "file"),
                                  new IODescriptor("output-topic", "topic"), LiveStatus.STARTED);
        LiveHeartbeat b = a.copy();

        verifyEquality(a, b);

        // Modify each field one by one
        b.setStatus(LiveStatus.COMPLETED);
        verifyInequality(a, b);
        b.setOutput(new IODescriptor("ElasticSearch", "secureagent"));
        verifyInequality(a, b);
        b.setInput(new IODescriptor("input-topic", "topic"));
        verifyInequality(a, b);
        b.setTimestamp(Date.from(Instant.now().plusSeconds(10)));
        verifyInequality(a, b);
        b.setComponentType("projector");
        verifyInequality(a, b);
        b.setName("Inequality Test");
        verifyInequality(a, b);
        b.setInstanceId(UUID.randomUUID().toString());
        verifyInequality(a, b);
        b.setId("inequality-03");
        verifyInequality(a, b);
        b.setReportingPeriod(60);
        verifyInequality(a, b);
    }

    private static void verifyEquality(LiveHeartbeat a, LiveHeartbeat b) {
        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    private static void verifyInequality(LiveHeartbeat a, LiveHeartbeat b) {
        Assert.assertFalse(a.equals(b));
        Assert.assertFalse(b.equals(a));
        Assert.assertNotEquals(a.hashCode(), b.hashCode());
    }
}
