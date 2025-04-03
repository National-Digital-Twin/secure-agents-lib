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
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.RequestIdFilter;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.TestInit;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.StatusHealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestWithoutCors extends AbstractAppEntrypoint {
    private static final RandomPortProvider PORT = new RandomPortProvider(16999);
    public static final String EXAMPLE_ORIGIN = "https://example.org";
    public static final String BAD_ORIGIN = "https://bad-origin";

    private final Client client = ClientBuilder.newClient();

    @BeforeClass
    public void setupServer() {
        allowRestrictedHeaders();
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
        //@formatter:off
        return ServerBuilder.create()
                            .application(MockHealthApplication.class)
                            // Use a different port for each test just in case one test is slow to teardown the server
                            .port(PORT.newPort())
                            // Explicitly disable CORS
                            .withoutCors()
                            .displayName("CORS Disabled Tests");
        //@formatter:on
    }

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    /**
     * Sets the necessary System Property that allows setting the Origin header
     */
    private static void allowRestrictedHeaders() {
        // Needed to allow setting the Origin header so CORS requests can be made
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    @Test
    public void test_without_cors_preflight_01() {
        // A CORS pre-flight request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", EXAMPLE_ORIGIN)
                                              .header("Access-Control-Request-Method", "GET");

        try (Response response = invocation.options()) {
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_without_cors_preflight_02() {
        // A CORS pre-flight request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", BAD_ORIGIN)
                                              .header("Access-Control-Request-Method", "GET");

        try (Response response = invocation.options()) {
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_without_cors_simple_01() {
        // A simple request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN).header("Origin", EXAMPLE_ORIGIN);

        try (Response response = invocation.get()) {
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_without_cors_non_simple_01() {
        // A non-simple request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", EXAMPLE_ORIGIN)
                                              .header(RequestIdFilter.REQUEST_ID, "cors-03");

        try (Response response = invocation.get()) {
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_cors_non_simple_02() {
        // A non-simple request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", EXAMPLE_ORIGIN)
                                              .header(RequestIdFilter.REQUEST_ID, "cors-03");

        // PUT is not a simple method for CORS
        try (Response response = invocation.put(Entity.text("test"))) {
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }
}
