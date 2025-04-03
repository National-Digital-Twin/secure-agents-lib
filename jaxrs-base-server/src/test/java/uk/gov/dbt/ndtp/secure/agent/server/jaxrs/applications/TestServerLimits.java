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

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class TestServerLimits {
    private static final RandomPortProvider PORT = new RandomPortProvider(21212);

    private final Client client = ClientBuilder.newClient();

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    @AfterClass
    public void teardown() {
        this.client.close();
    }

    private Invocation.Builder addLargeHeader(Invocation.Builder invocation, String header) {
        return invocation.header(header,
                                 StringUtils.repeat("a", HttpServerFilter.DEFAULT_MAX_HTTP_PACKET_HEADER_SIZE + 1));
    }

    private Invocation.Builder addManyHeaders(Invocation.Builder invocation, int numHeaders) {
        int i = 0;
        while (i++ < numHeaders) {
            invocation = invocation.header("Header" + i, Integer.toString(i));

        }
        return invocation;
    }

    @Test
    public void givenDefaultServerConfiguration_whenMakingSimpleRequest_thenOk() throws
            IOException {
        // Given
        Server server = buildCommon().build();
        try {
            server.start();
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // When
            Response response = invocation.get();

            // Then
            Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    public void givenDefaultServerConfiguration_whenMakingRequestWithVeryLargeHeader_thenBadRequest() throws
            IOException {
        // Given
        Server server = buildCommon().build();
        try {
            server.start();
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = addLargeHeader(target.request(MediaType.APPLICATION_JSON), "Test");

            // When
            Response response = invocation.get();

            // Then
            Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    public void givenDefaultServerConfiguration_whenMakingRequestWithManyHeaders_thenBadRequest() throws
            IOException {
        // Given
        Server server = buildCommon().build();
        try {
            server.start();
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation =
                    addManyHeaders(target.request(MediaType.APPLICATION_JSON), MimeHeaders.MAX_NUM_HEADERS_DEFAULT + 1);

            // When
            Response response = invocation.get();

            // Then
            Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    public void givenCustomServerConfiguration_whenMakingRequestWithVeryLargeHeader_thenOk() throws IOException {
        // Given
        Server server = buildCommon().maxHttpHeaderSize(16 * 1024).build();
        try {
            server.start();
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = addLargeHeader(target.request(MediaType.APPLICATION_JSON), "Test");

            // When
            Response response = invocation.get();

            // Then
            Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    public void givenCustomServerConfiguration_whenMakingRequestWithManyHeaders_thenOk() throws
            IOException {
        // Given
        Server server = buildCommon().maxHttpRequestHeaders(150).build();
        try {
            server.start();
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation =
                    addManyHeaders(target.request(MediaType.APPLICATION_JSON), MimeHeaders.MAX_NUM_HEADERS_DEFAULT + 1);

            // When
            Response response = invocation.get();

            // Then
            Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    public void givenCustomServerConfiguration_whenMakingSimpleRequest_thenBadRequest() throws
            IOException {
        // Given
        Server server = buildCommon().maxHttpResponseHeaders(1).build();
        try {
            server.start();
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            try {
                // When
                invocation.get();
                Assert.fail("Expected Grizzly to abort the response in this test");
            } catch (ProcessingException e) {
                // Then
                Assert.assertTrue(StringUtils.contains(e.getMessage(), "Unexpected end of file"));
            }
        } finally {
            server.stop();
        }
    }

    private static ServerBuilder buildCommon() {
        return ServerBuilder.create()
                            .port(PORT.newPort())
                            .application(MockHealthApplication.class)
                            .displayName("Limit Tests");
    }
}
