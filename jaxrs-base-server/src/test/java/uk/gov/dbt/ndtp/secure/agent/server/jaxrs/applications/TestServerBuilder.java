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
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class TestServerBuilder {

    private final Client client = ClientBuilder.newClient();

    @AfterClass
    public void teardown() {
        this.client.close();
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*application class.*")
    public void build_server_bad_01() {
        ServerBuilder.create().build();
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*port.*")
    public void build_server_bad_02() {
        ServerBuilder.create().application(MockApplication.class).build();
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*display name.*")
    public void build_server_bad_03() {
        ServerBuilder.create().application(MockApplication.class).port(1234).build();
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*valid URI")
    public void build_server_bad_04() {
        ServerBuilder.create()
                     .application(MockApplication.class)
                     .port(1234)
                     .displayName("Test")
                     .hostname("foo bar")
                     .build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*valid port.*")
    public void build_server_bad_port_01() {
        ServerBuilder.create().port(-100);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*valid port.*")
    public void build_server_bad_port_02() {
        ServerBuilder.create().port(1234567);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Context path.*")
    public void build_server_bad_context_path_01() {
        ServerBuilder.create().contextPath("foo");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Context path.*")
    public void build_server_bad_context_path_02() {
        ServerBuilder.create().contextPath("foo");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Context path.*")
    public void build_server_bad_context_path_03() {
        ServerBuilder.create().contextPath("/foo/");
    }

    private void verifyHealthy(Server server) throws IOException {
        server.start();
        WebTarget target = this.client.target(server.getBaseUri()).path("/healthz");
        Assert.assertTrue(StringUtils.startsWith(target.getUri().toString(), server.getBaseUri()));
        try (Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode());
        } finally {
            server.shutdownNow();
        }
    }

    @Test
    public void build_server_01() throws IOException {
        Server server =
                ServerBuilder.create().application(MockApplication.class).port(1234).displayName("Test").build();
        Assert.assertEquals(server.getDisplayName(), "Test");
        Assert.assertEquals(server.getHostname(), ServerBuilder.DEFAULT_HOSTNAME);
        Assert.assertEquals(server.getPort(), 1234);
        Assert.assertEquals(server.getBaseUri(), "http://localhost:1234/");

        verifyHealthy(server);
    }

    @Test
    public void build_server_02() throws IOException {
        Server server =
                ServerBuilder.create()
                             .application(MockApplication.class)
                             .port(56780)
                             .contextPath(ServerBuilder.ROOT_CONTEXT)
                             .displayName("Test")
                             .build();
        Assert.assertEquals(server.getDisplayName(), "Test");
        Assert.assertEquals(server.getHostname(), ServerBuilder.DEFAULT_HOSTNAME);
        Assert.assertEquals(server.getPort(), 56780);
        Assert.assertEquals(server.getBaseUri(), "http://localhost:56780/");

        verifyHealthy(server);
    }

    @Test
    public void build_server_03() throws IOException {
        Server server =
                ServerBuilder.create()
                             .application(MockApplication.class)
                             .port(56780)
                             .contextPath("/a/unit/test")
                             .displayName("Test")
                             .build();
        Assert.assertEquals(server.getDisplayName(), "Test");
        Assert.assertEquals(server.getHostname(), ServerBuilder.DEFAULT_HOSTNAME);
        Assert.assertEquals(server.getPort(), 56780);
        Assert.assertEquals(server.getBaseUri(), "http://localhost:56780/a/unit/test");

        verifyHealthy(server);
    }

    @Test
    public void build_server_04() throws IOException {
        Server server =
                ServerBuilder.create()
                             .application(MockApplication.class)
                             .port(56780)
                             .contextPath("/a/unit/test")
                             .rootContextPath()
                             .displayName("Test")
                             .build();
        Assert.assertEquals(server.getDisplayName(), "Test");
        Assert.assertEquals(server.getHostname(), ServerBuilder.DEFAULT_HOSTNAME);
        Assert.assertEquals(server.getPort(), 56780);
        Assert.assertEquals(server.getBaseUri(), "http://localhost:56780/");

        verifyHealthy(server);
    }
}
