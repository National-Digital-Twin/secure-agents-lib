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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestHealthStatus {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    public void health_status_01() throws JsonProcessingException {
        HealthStatus bad = new HealthStatus(false);
        String json = this.json.writeValueAsString(bad);
        Assert.assertTrue(json.contains("false"));
        Assert.assertFalse(json.contains("reasons"));
        Assert.assertFalse(json.contains("config"));

        verifyRoundTrip(bad, json);
    }

    private void verifyRoundTrip(HealthStatus bad, String json) throws JsonProcessingException {
        HealthStatus retrieved = this.json.readValue(json, HealthStatus.class);
        Assert.assertEquals(retrieved, bad);
        Assert.assertEquals(retrieved.hashCode(), bad.hashCode());
        Assert.assertEquals(retrieved.toString(), bad.toString());
    }

    @Test
    public void health_status_02() throws JsonProcessingException {
        HealthStatus bad = new HealthStatus(true);
        String json = this.json.writeValueAsString(bad);
        Assert.assertTrue(json.contains("true"));
        Assert.assertFalse(json.contains("reasons"));
        Assert.assertFalse(json.contains("config"));

        verifyRoundTrip(bad, json);
    }

    @Test
    public void health_status_03() throws JsonProcessingException {
        HealthStatus bad = new HealthStatus(false, List.of("Ian did it"), Collections.emptyMap());
        String json = this.json.writeValueAsString(bad);
        Assert.assertTrue(json.contains("false"));
        Assert.assertTrue(json.contains("reasons"));
        Assert.assertTrue(json.contains("Ian did it"));
        Assert.assertFalse(json.contains("config"));

        verifyRoundTrip(bad, json);
    }

    @Test
    public void health_status_04() throws JsonProcessingException {
        HealthStatus bad = new HealthStatus(false, List.of("Ian did it", "But also Rob"), Collections.emptyMap());
        String json = this.json.writeValueAsString(bad);
        Assert.assertTrue(json.contains("false"));
        Assert.assertTrue(json.contains("reasons"));
        Assert.assertTrue(json.contains("Ian did it"));
        Assert.assertTrue(json.contains("But also Rob"));
        Assert.assertFalse(json.contains("config"));

        verifyRoundTrip(bad, json);
    }

    @Test
    public void health_status_05() throws JsonProcessingException {
        HealthStatus bad = new HealthStatus(true, Collections.emptyList(), Map.of("foo", "bar"));
        String json = this.json.writeValueAsString(bad);
        Assert.assertTrue(json.contains("true"));
        Assert.assertFalse(json.contains("reasons"));
        Assert.assertTrue(json.contains("config"));
        Assert.assertTrue(json.contains("foo"));
        Assert.assertTrue(json.contains("bar"));

        verifyRoundTrip(bad, json);
    }

    @Test
    public void health_status_06() throws JsonProcessingException {
        HealthStatus bad = new HealthStatus(false, Collections.emptyList(), Collections.emptyMap());
        String json = this.json.writeValueAsString(bad);
        Assert.assertTrue(json.contains("false"));
        Assert.assertFalse(json.contains("reasons"));
        Assert.assertFalse(json.contains("config"));

        verifyRoundTrip(bad, json);
    }

    @Test
    public void health_status_07() throws JsonProcessingException {
        HealthStatus bad = new HealthStatus(false, null, null);
        String json = this.json.writeValueAsString(bad);
        Assert.assertTrue(json.contains("false"));
        Assert.assertFalse(json.contains("reasons"));
        Assert.assertFalse(json.contains("config"));

        verifyRoundTrip(bad, json);
    }

    @Test
    public void health_status_08() {
        HealthStatus status = new HealthStatus();
        status.setReasons(null);
        Assert.assertTrue(status.reasons().isEmpty());
        status.setConfig(null);
        Assert.assertTrue(status.getConfig().isEmpty());

        status.setConfig(Map.of("foo", true, "bar", false));
        Assert.assertTrue(status.toString().contains("foo=true"));
        Assert.assertTrue(status.toString().contains("bar=false"));
    }

    @Test
    public void health_status_09() {
        HealthStatus status = new HealthStatus();
        Assert.assertTrue(status.equals(status));
        Assert.assertFalse(status.equals(null));
        Assert.assertFalse(status.equals(new ObjectMapper()));

        status = new HealthStatus(true, List.of("Working well", "Nothing to see here"),
                                  Map.of("foo", true, "bar", false));
        HealthStatus otherStatus = new HealthStatus(true, List.of("Working well", "Nothing to see here"),
                                                    Map.of("foo", true, "bar", false));
        Assert.assertEquals(status, otherStatus);
        Assert.assertTrue(status.equals(otherStatus));

        otherStatus.getConfig().clear();
        Assert.assertFalse(status.equals(otherStatus));
        otherStatus.reasons().clear();
        Assert.assertFalse(status.equals(otherStatus));
        otherStatus.setHealthy(false);
        Assert.assertFalse(status.equals(otherStatus));
    }
}
