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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.TestInit;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.BrokenStatusHealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.StatusHealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;

public class TestBrokenHealthServer extends AbstractAppEntrypoint {

    private static final RandomPortProvider PORT = new RandomPortProvider(15555);

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
        BrokenStatusHealthResource.reset();
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create()
                            .application(MockBrokenHealthApplication.class)
                            // Use a different port for each test just in case one test is slow to teardown the server
                            .port(PORT.newPort())
                            .displayName("Broken Health Status Tests");
    }

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    @Test
    public void test_healthy_01() {
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            HealthStatus status = response.readEntity(HealthStatus.class);

            Assert.assertFalse(status.isHealthy());
            Assert.assertFalse(status.reasons().isEmpty());
            Assert.assertEquals(status.reasons().size(), 2);
            Assert.assertEquals(status.reasons().get(0), StatusHealthResource.UNEXPECTED_ERROR_REASON);
            Assert.assertEquals(status.reasons().get(1), BrokenStatusHealthResource.DEFAULT_ERROR_MESSAGE);
        }
    }

    @Test
    public void test_healthy_02() {
        BrokenStatusHealthResource.ERROR_MESSAGE = "Bar";

        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            HealthStatus status = response.readEntity(HealthStatus.class);

            Assert.assertFalse(status.isHealthy());
            Assert.assertFalse(status.reasons().isEmpty());
            Assert.assertEquals(status.reasons().size(), 2);
            Assert.assertEquals(status.reasons().get(0), StatusHealthResource.UNEXPECTED_ERROR_REASON);
            Assert.assertEquals(status.reasons().get(1), "Bar");
        }
    }
}
