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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.RequestIdFilter;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.TestInit;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.StatusHealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestHealthServer extends AbstractAppEntrypoint {
    private static final RandomPortProvider PORT = new RandomPortProvider(14777);

    private final Client client = ClientBuilder.newClient();

    @BeforeClass
    public void setupServer() {
        this.run(false);
    }

    @AfterClass
    public void teardownServer() {
        this.server.shutdownNow();
        this.client.close();
    }

    @BeforeMethod
    public void setup() {
        TestInit.reset();
        StatusHealthResource.reset();
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create().application(MockHealthApplication.class)
                            // Use a different port for each test just in case one test is slow to teardown the server
                            .port(PORT.newPort()).displayName("Health Status Tests");
    }

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    @Test
    public void test_healthy_01() {
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        HealthStatus status = invocation.get(HealthStatus.class);

        Assert.assertTrue(status.isHealthy());
        Assert.assertTrue(status.reasons().isEmpty());
    }

    @Test
    public void test_healthy_02() {
        StatusHealthResource.IS_HEALTHY = false;

        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());

            HealthStatus status = response.readEntity(HealthStatus.class);
            Assert.assertFalse(status.isHealthy());
            Assert.assertTrue(status.reasons().isEmpty());
        }
    }

    @Test
    public void test_healthy_03() {
        StatusHealthResource.IS_HEALTHY = false;
        StatusHealthResource.REASONS.add("No foo");

        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());

            HealthStatus status = response.readEntity(HealthStatus.class);
            Assert.assertFalse(status.isHealthy());
            Assert.assertFalse(status.reasons().isEmpty());
            Assert.assertEquals(status.reasons().get(0), "No foo");
        }
    }

    @Test
    public void test_request_ids_01() {
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

        Set<String> requestIds = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            try (Response response = invocation.get()) {
                HealthStatus status = response.readEntity(HealthStatus.class);
                Assert.assertTrue(status.isHealthy());
                Assert.assertTrue(status.reasons().isEmpty());

                Assert.assertTrue(requestIds.add(response.getHeaderString(RequestIdFilter.REQUEST_ID)),
                                  "Every request should return a unique Request ID");
            }
        }
    }

    @Test
    public void test_request_ids_02() {
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation =
                target.request(MediaType.APPLICATION_JSON).header(RequestIdFilter.REQUEST_ID, "test");

        Set<String> requestIds = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            try (Response response = invocation.get()) {
                HealthStatus status = response.readEntity(HealthStatus.class);
                Assert.assertTrue(status.isHealthy());
                Assert.assertTrue(status.reasons().isEmpty());

                Assert.assertTrue(requestIds.add(response.getHeaderString(RequestIdFilter.REQUEST_ID)),
                                  "Every request should return a unique Request ID even when client supplies the same Request ID");
            }
        }
    }

    @Test
    public void test_request_ids_03() {
        WebTarget target = forServer(this.server, "/healthz");
        String tooLongId = "abc".repeat(RequestIdFilter.MAX_CLIENT_REQUEST_ID_LENGTH);
        String allowedId = tooLongId.substring(0, RequestIdFilter.MAX_CLIENT_REQUEST_ID_LENGTH);

        Invocation.Builder invocation =
                target.request(MediaType.APPLICATION_JSON).header(RequestIdFilter.REQUEST_ID, tooLongId);

        Set<String> requestIds = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            try (Response response = invocation.get()) {
                HealthStatus status = response.readEntity(HealthStatus.class);
                Assert.assertTrue(status.isHealthy());
                Assert.assertTrue(status.reasons().isEmpty());

                String requestId = response.getHeaderString(RequestIdFilter.REQUEST_ID);

                // Verify that the server truncated the supplied Request ID appropriately
                Assert.assertTrue(requestId.startsWith(allowedId));
                Assert.assertFalse(requestId.startsWith(tooLongId));
                Assert.assertEquals(requestId.charAt(RequestIdFilter.MAX_CLIENT_REQUEST_ID_LENGTH), '/');
                Assert.assertTrue(requestIds.add(requestId),
                                  "Every request should return a unique Request ID even when client supplies the same Request ID");
            }
        }
    }
}
