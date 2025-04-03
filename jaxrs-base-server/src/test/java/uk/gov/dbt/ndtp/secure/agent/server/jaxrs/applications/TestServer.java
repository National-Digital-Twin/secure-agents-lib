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

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.web.HttpNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.observability.LibraryVersion;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.MockAuthInit;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.ServerRuntimeInfo;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.TestInit;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.MockData;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.VersionInfo;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.DataResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtHttpConstants;

public class TestServer extends AbstractAppEntrypoint {

    private static final RandomPortProvider PORT = new RandomPortProvider(1366);

    private final Client client = ClientBuilder.newClient().register(ProblemCustomReaderWriter.class);

    @BeforeMethod
    public void setup() {
        DataResource.reset();
        TestInit.reset();
    }

    @AfterClass
    public void teardown() {
        this.client.close();
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create().application(MockApplication.class)
                            // Use a different port for each test just in case one test is slow to teardown the server
                            .port(PORT.newPort()).displayName("Test");
    }

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    private void verifyHealthy(Server server) throws IOException {
        server.start();

        pingHealthz(server);

        server.shutdownNow();
    }

    private void pingHealthz(Server server) {
        WebTarget target = forServer(server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        verifyResponse(invocation.get(), Response.Status.NO_CONTENT);
    }

    @Test
    public void server_01() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            verifyHealthy(server);
        }
    }

    @Test
    public void server_01a() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.hostname("127.0.0.1");
        try (Server server = builder.build()) {
            verifyHealthy(server);
        }
    }

    @Test
    public void server_01b() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.hostname("0.0.0.0");
        try (Server server = builder.build()) {
            verifyHealthy(server);
        }
    }

    @Test
    public void server_01c() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.contextPath("/app");
        try (Server server = builder.build()) {
            verifyHealthy(server);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void server_01d() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.localhost();
        try (Server server = builder.build()) {
            verifyHealthy(server);
        }
    }

    @Test
    public void server_02() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/data/test");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            verifyResponse(invocation.get(), Response.Status.NOT_FOUND);

            invocation = target.queryParam("value", "foo").request(MediaType.APPLICATION_JSON);
            MockData data = invocation.post(Entity.json(""), MockData.class);
            Assert.assertEquals(data.key(), "test");
            Assert.assertEquals(data.value(), "foo");

            server.shutdownNow();
        }
    }

    @Test
    public void server_03() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/data/test");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            verifyPreconditionFailed(invocation);

            invocation = target.queryParam("value", "foo").request(MediaType.APPLICATION_JSON);
            MockData data = invocation.post(Entity.json(""), MockData.class);
            Assert.assertEquals(data.key(), "test");
            Assert.assertEquals(data.value(), "foo");

            invocation = target.queryParam("value", "bar").request(MediaType.APPLICATION_JSON);
            verifyPreconditionFailed(invocation);

            invocation = target.queryParam("value", "foo").request(MediaType.APPLICATION_JSON);
            verifyResponse(invocation.delete(), Response.Status.NO_CONTENT);

            server.shutdownNow();
        }
    }

    public static void verifyResponse(Response response, Response.Status expectedStatus) {
        Assert.assertEquals(response.getStatus(), expectedStatus.getStatusCode());
        response.close();
    }

    public static void verifyPreconditionFailed(Invocation.Builder invocation) {
        verifyError(invocation, Response.Status.PRECONDITION_FAILED.getStatusCode());
    }

    public static void verifyError(Invocation.Builder invocation, int errorStatus) {
        verifyError(invocation, SyncInvoker::delete, errorStatus);
    }

    public static void verifyError(Invocation.Builder invocation, Function<Invocation.Builder, Response> action,
                                   int errorStatus) {
        try {
            Response response = action.apply(invocation);
            Assert.assertEquals(response.getStatus(), errorStatus);
            response.close();
        } catch (ClientErrorException e) {
            Assert.assertTrue(errorStatus >= 400 && errorStatus <= 499);
            Assert.assertEquals(e.getResponse().getStatus(), errorStatus);
        } catch (ServerErrorException e) {
            Assert.assertTrue(errorStatus >= 500);
            Assert.assertEquals(e.getResponse().getStatus(), errorStatus);
        }
    }

    public static Problem verifyProblem(Invocation.Builder invocation, Function<Invocation.Builder, Response> action,
                                        int errorStatus) {
        Response response = action.apply(invocation);
        Assert.assertEquals(response.getStatus(), errorStatus);
        return response.readEntity(Problem.class);
    }

    @Test
    public void server_entrypoint_01() {
        this.run(false);
        Assert.assertTrue(this.server.isRunning());
        this.server.shutdownNow();
    }

    @Test
    public void server_entrypoint_02() {
        this.run(false);
        Assert.assertTrue(this.server.isRunning());
        this.server.close();
    }

    @Test
    public void server_lifecycle_01() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();
            Assert.assertThrows(IllegalStateException.class, server::start);
        }
    }

    @Test
    public void server_lifecycle_02() {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            Assert.assertThrows(IllegalStateException.class, server::stop);
        }
    }

    @Test
    public void server_lifecycle_03() {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            Assert.assertThrows(IllegalStateException.class, () -> server.stop(1, TimeUnit.MINUTES));
        }
    }

    // Appears to be a bug in Grizzly 2 that it never calls shutdownNow() even after stuff is shutdown
    // https://github.com/eclipse-ee4j/grizzly/issues/2158
    @Test(enabled = false)
    public void server_lifecycle_04() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();
            server.stop();
            Assert.assertThrows(IllegalStateException.class, server::stop);
        }
    }

    // This variation of the test passes because we use a non-graceful shutdown
    @Test
    public void server_lifecycle_04b() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();
            server.shutdownNow();
            Assert.assertThrows(IllegalStateException.class, server::stop);
        }
    }

    @Test
    public void server_lifecycle_05() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.withAutoConfigInitialisation();
        try (Server server = builder.build()) {
            server.start();
            server.shutdownNow();
        }
    }

    @Test
    public void server_lifecycle_06() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> this.run(true));
        Thread.sleep(250);
        pingHealthz(this.server);
        future.cancel(true);
        Assert.assertTrue(future.isCancelled());
        Thread.sleep(250);
        try {
            future.get();
            Assert.fail("Expected the future to have been cancelled");
        } catch (CancellationException e) {
            // Expected
        } catch (Throwable e) {
            Assert.fail("Unexpected error: " + e);
        }

        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request().accept(MediaType.APPLICATION_JSON_TYPE);
        try {
            invocation.get();
        } catch (ProcessingException e) {
            Assert.assertTrue(e.getCause() instanceof ConnectException, "Expected server to no longer be running");
        }
    }


    @Test
    public void server_with_auth_01() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.application(MockApplicationWithAuth.class);
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            // No auth configured so 500 Internal Server Error
            verifyError(invocation, SyncInvoker::get, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            server.shutdownNow();
        }
    }

    @Test
    public void server_with_auth_02() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.application(MockApplicationWithAuth.class).withListener(MockAuthInit.class);
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            // Properly configured auth but no credentials so 403 Forbidden
            verifyError(invocation, SyncInvoker::get, Response.Status.FORBIDDEN.getStatusCode());

            // After adding credentials should get a success response
            invocation = invocation.header(JwtHttpConstants.HEADER_AUTHORIZATION,
                                           String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                         MockAuthInit.createToken("test")));
            verifyResponse(invocation.get(), Response.Status.NO_CONTENT);


            server.shutdownNow();
        }
    }

    @Test
    public void server_with_auth_03() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.application(MockApplicationWithAuth.class)
                         .withListener(MockAuthInit.class)
                         .withAuthExclusion("/healthz");
        try (Server server = builder.build()) {
            server.start();

            // As we added /healthz to exclusions we can hit this without any authentication
            pingHealthz(server);

            // Other paths should continue to require authentication
            WebTarget target = forServer(server, "/data/test");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            // Properly configured auth but no credentials so 403 Forbidden
            verifyError(invocation, SyncInvoker::get, Response.Status.FORBIDDEN.getStatusCode());

            server.shutdownNow();
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void server_with_auth_04() {
        ServerBuilder builder = buildServer();
        builder.application(MockApplicationWithAuth.class).withListener(MockAuthInit.class)
               // An Auth exclusion that bypasses auth for all paths
               .withAuthExclusion("/*");
    }

    @Test
    public void server_with_auth_05() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.application(MockApplicationWithAuth.class).withListener(MockAuthInit.class)
                         // An Auth exclusion that bypasses auth for some paths
                         .withAuthExclusions("/healthz", "/data/*");
        try (Server server = builder.build()) {
            server.start();

            // As we added /healthz to exclusions we can hit this without any authentication
            pingHealthz(server);

            // /data/* paths can be accessed without authentication
            WebTarget target = forServer(server, "/data/test");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            verifyError(invocation, SyncInvoker::get, Response.Status.NOT_FOUND.getStatusCode());

            // Other paths still require authentication
            target = forServer(server, "/params/mode");
            invocation = target.request(MediaType.APPLICATION_JSON);
            verifyError(invocation, SyncInvoker::get, Response.Status.FORBIDDEN.getStatusCode());

            server.shutdownNow();
        }
    }

    @Test
    public void server_with_auth_06() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.application(MockApplicationWithAuth.class).withListener(MockAuthInit.class);
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/healthz");
            // Specifying multiple headers with credentials, first one is invalid, second is valid, so the overall
            // authentication should succeed
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON)
                                                  .header(JwtHttpConstants.HEADER_AUTHORIZATION,
                                                          String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                                        "invalid-token"))
                                                  .header(MockAuthInit.X_CUSTOM_AUTH, MockAuthInit.createToken("test"));
            verifyResponse(invocation.get(), Response.Status.NO_CONTENT);

            server.shutdownNow();
        }
    }

    @Test
    public void server_with_auth_07() throws IOException {
        ServerBuilder builder = buildServer();
        builder = builder.application(MockApplicationWithAuth.class).withListener(MockAuthInit.class);
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/healthz");
            // Specifying multiple headers with credentials, both invalid so failure expected
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON)
                                                  .header(JwtHttpConstants.HEADER_AUTHORIZATION,
                                                          String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                                        "invalid-token"))
                                                  .header(MockAuthInit.X_CUSTOM_AUTH, "invalid-token");
            verifyResponse(invocation.get(), Response.Status.FORBIDDEN);

            server.shutdownNow();
        }
    }

    @Test
    public void givenServer_whenRequestingNonExistingPath_then404NotFound() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/no/such/resource");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            verifyError(invocation, SyncInvoker::get, Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void givenServer_whenSupplyingBlankParameter_then400BadRequest() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/data/%20");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            verifyError(invocation, SyncInvoker::get, Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void givenServer_whenSupplyingBlankBodyField_then400BadRequest() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/data/test");
            Invocation.Builder invocation = target.queryParam("value", "   ").request(MediaType.APPLICATION_JSON);

            // Then
            verifyError(invocation, i -> i.post(Entity.json("")), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void givenServer_whenUsingWrongHttpVerb_then405MethodNotAllowed() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/data/test");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            verifyError(invocation, i -> i.put(Entity.json("")), Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
        }
    }

    @Test
    public void givenServer_whenSupplyingInvalidParameterValueForCustomConversion_then400BadRequest() throws
            IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/params/mode");
            Invocation.Builder invocation = target.queryParam("foo").request(MediaType.APPLICATION_JSON);

            // Then
            verifyError(invocation, i -> i.post(Entity.json("")), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void givenServer_whenSupplyingExternalParameter_then400BadRequest_andMessageIdentifiesParameterCorrectly() throws
            IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/params/external/test");
            Invocation.Builder invocation =
                    target.queryParam("query", "%20").request(Problem.MEDIA_TYPE);
            Form form = new Form();
            form.param("form", "test");

            // Then
            Problem problem = verifyProblem(invocation, i -> i.post(Entity.form(form)),
                                            Response.Status.BAD_REQUEST.getStatusCode());
            Assert.assertNotNull(problem);

            // And
            Assert.assertTrue(StringUtils.contains(problem.getDetail(), "'query'"));
        }
    }

    @Test
    public void givenServer_whenRequestIsNotCurrentlyValid_then409Conflict() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/data/actions/destroy");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            verifyError(invocation, SyncInvoker::delete, Response.Status.CONFLICT.getStatusCode());
        }
    }

    @Test
    public void server_version_info_01() throws IOException {
        LibraryVersion.resetCaches();
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/version-info");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            try (Response response = invocation.get()) {
                Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
                VersionInfo info = response.readEntity(VersionInfo.class);

                Assert.assertTrue(info.getLibraryVersions().isEmpty());
            }
        }
    }

    @Test
    public void server_version_info_02() throws IOException {
        LibraryVersion.resetCaches();
        ServerBuilder builder = buildServer().withVersionInfo("observability-lib", "jaxrs-base-server");
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/version-info");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            try (Response response = invocation.get()) {
                Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
                VersionInfo info = response.readEntity(VersionInfo.class);

                Assert.assertFalse(info.getLibraryVersions().isEmpty());
                Assert.assertTrue(info.getLibraryVersions().containsKey("observability-lib"));
                Assert.assertTrue(info.getLibraryVersions().containsKey("jaxrs-base-server"));
            }
        }
    }

    @Test
    public void server_runtime_info_02() throws IOException {
        LibraryVersion.resetCaches();
        ServerBuilder builder = buildServer().withVersionInfo("observability-lib", "jaxrs-base-server")
                                             .withListener(ServerRuntimeInfo.class);
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/version-info");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
            try (Response response = invocation.get()) {
                Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
                VersionInfo info = response.readEntity(VersionInfo.class);

                Assert.assertFalse(info.getLibraryVersions().isEmpty());
                Assert.assertTrue(info.getLibraryVersions().containsKey("observability-lib"));
                Assert.assertTrue(info.getLibraryVersions().containsKey("jaxrs-base-server"));
            }
        }
    }

    @Test
    public void givenServer_whenGeneratingAProblemResponse_thenProblemIsReturned() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = createProblemTarget(server);
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            try (Response response = invocation.get()) {
                Problem problem =
                        verifyProblemResponse(response, Response.Status.BAD_REQUEST.getStatusCode(),
                                              MediaType.APPLICATION_JSON);

                verifyProblemContent(problem, "Test Error", "Test", "Something went wrong");
            }
        }
    }

    @Test
    public void givenServer_whenGeneratingAProblemResponseAsJsonSubType_thenProblemIsReturned() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = createProblemTarget(server);
            Invocation.Builder invocation = target.request(Problem.MEDIA_TYPE);

            // Then
            try (Response response = invocation.get()) {
                Problem problem = verifyProblemResponse(response, Response.Status.BAD_REQUEST.getStatusCode(),
                                                        Problem.MEDIA_TYPE);

                verifyProblemContent(problem, "Test Error", "Test", "Something went wrong");
            }
        }
    }

    @Test
    public void givenServer_whenGeneratingAProblemResponseWithComplexAcceptHeader_thenProblemIsReturnedWithPreferredContentType() throws
            IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = createProblemTarget(server);
            Invocation.Builder invocation =
                    target.request("application/custom;q=0.1", "application/problem+json;q=0.5", "text/plain;q=0.3",
                                   "application/json");

            // Then
            try (Response response = invocation.get()) {
                Problem problem =
                        verifyProblemResponse(response, Response.Status.BAD_REQUEST.getStatusCode(),
                                              MediaType.APPLICATION_JSON);

                verifyProblemContent(problem, "Test Error", "Test", "Something went wrong");
            }
        }
    }

    public static void verifyProblemContent(Problem problem, String expectedTitle, String expectedType,
                                             String expectedDetail) {
        Assert.assertEquals(problem.getTitle(), expectedTitle);
        Assert.assertEquals(problem.getType(), expectedType);
        Assert.assertEquals(problem.getDetail(), expectedDetail);
    }

    @Test
    public void givenServer_whenGeneratingAProblemResponseWithNoAcceptHeader_thenProblemIsReturnedInProblemContentType() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = createProblemTarget(server);
            Invocation.Builder invocation = target.request();

            // Then
            try (Response response = invocation.get()) {
                Problem problem = verifyProblemResponse(response, Response.Status.BAD_REQUEST.getStatusCode(),
                                                        Problem.MEDIA_TYPE);

                verifyProblemContent(problem, "Test Error", "Test", "Something went wrong");
            }
        }
    }

    @Test
    public void givenServer_whenGeneratingAProblemResponseWithWildcardAcceptHeader_thenProblemIsReturnedInProblemContentType() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = createProblemTarget(server);
            Invocation.Builder invocation = target.request(MediaType.WILDCARD);

            // Then
            try (Response response = invocation.get()) {
                Problem problem = verifyProblemResponse(response, Response.Status.BAD_REQUEST.getStatusCode(),
                                                        Problem.MEDIA_TYPE);

                verifyProblemContent(problem, "Test Error", "Test", "Something went wrong");
            }
        }
    }

    private WebTarget createProblemTarget(Server server) {
        return forServer(server, "/problems").queryParam("status", 400)
                                             .queryParam("type", "Test")
                                             .queryParam("title", "Test Error")
                                             .queryParam("detail", "Something went wrong");
    }

    @Test
    public void givenServer_whenGeneratingAProblemResponseAsCustomType_thenProblemIsReturned() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = createProblemTarget(server);
            Invocation.Builder invocation = target.request("application/custom");
            try (Response response = invocation.get()) {
                Problem problem = verifyProblemResponse(response, Response.Status.BAD_REQUEST.getStatusCode(),
                                                        "application/custom");

                verifyProblemContent(problem, "Test Error", "Test", "Something went wrong");
            }
        }
    }

    private static Problem verifyProblemResponse(Response response, int expectedStatus, String expectedContentType) {
        Assert.assertEquals(response.getStatus(), expectedStatus);
        Assert.assertEquals(response.getHeaderString(HttpNames.hContentType), expectedContentType);
        Problem problem = response.readEntity(Problem.class);
        return problem;
    }

    @Test
    public void givenServer_whenGeneratingAProblemResponseAsPlainText_thenProblemIsReturned() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = createProblemTarget(server);
            Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN);
            try (Response response = invocation.get()) {
                Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
                Assert.assertEquals(response.getHeaderString(HttpNames.hContentType), MediaType.TEXT_PLAIN);
                String body = response.readEntity(String.class);

                Assert.assertTrue(StringUtils.contains(body, "400"));
                Assert.assertTrue(StringUtils.contains(body, "Test Error"));
                Assert.assertTrue(StringUtils.contains(body, "Something went wrong"));
            }
        }
    }

    @Test
    public void givenServer_whenThrowingAnError_thenProblemIsReturned() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/problems/throw").queryParam("message", "Something went wrong");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            try (Response response = invocation.get()) {
                Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                Problem problem = response.readEntity(Problem.class);

                verifyProblemContent(problem, "Unexpected Error", "InternalServerError", "Something went wrong");
            }
        }
    }

    @Test
    public void givenServer_whenNotFoundIsHandled_thenProblemIsReturned() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/problems/no-such-path");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            try (Response response = invocation.get()) {
                Problem problem =
                        verifyProblemResponse(response, Response.Status.NOT_FOUND.getStatusCode(),
                                              MediaType.APPLICATION_JSON);
                Assert.assertTrue(StringUtils.contains(problem.getDetail(), "not a valid URL"));
            }
        }
    }

    @Test
    public void givenServer_whenNotFoundIsHandledWithoutAcceptHeader_thenProblemIsReturned() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/problems/no-such-path");
            Invocation.Builder invocation = target.request();

            // Then
            try (Response response = invocation.get()) {
                Problem problem =
                        verifyProblemResponse(response, Response.Status.NOT_FOUND.getStatusCode(),
                                              Problem.MEDIA_TYPE);
                Assert.assertTrue(StringUtils.contains(problem.getDetail(), "not a valid URL"));
            }
        }
    }

    @Test
    public void givenServer_whenNotFoundIsHandledWithCustomAcceptHeader_thenProblemIsReturned() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/problems/no-such-path");
            Invocation.Builder invocation = target.request("application/custom");

            // Then
            try (Response response = invocation.get()) {
                Assert.assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
                Assert.assertNull(response.getHeaderString(HttpNames.hContentType));

                // NB - JAX-RS treats null Content-Type as application/octet-stream so can't deserialize this as a
                //      Problem as no suitable MessageBodyReader is registered, just read as String instead
                String problem = response.readEntity(String.class);
                Assert.assertTrue(StringUtils.contains(problem, "not a valid URL"));
            }
        }
    }


    /*
    The following tests were intended to test a fix for #41

    However, for reasons I cannot explain, when written as a unit test like this these result in a 406 Not Acceptable
    Error because the Jersey router fails to map these to the intended DataResource.  However running the debug() method
    after these tests does show that the server correctly responses to such requests
     */

    @Test(enabled = false)
    public void server_with_encoded_slashes_01() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/data/encoded%2Fslash");
            Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN);
            verifyError(invocation, SyncInvoker::get, Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test(enabled = false)
    public void server_with_encoded_slashes_02() throws IOException {
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            WebTarget target = forServer(server, "/data/encoded%2Fslash");
            Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN);
            verifyError(invocation, SyncInvoker::get, Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    // Can be useful to run the server in blocking mode for debugging, but commented out by default as otherwise the IDE
    // won't try to run the class as a Test class by default

    /*
    public static void main(String[] args) {
        TestServer test = new TestServer();
        ServerBuilder builder = test.buildServer().withVersionInfo("observability-lib", "jaxrs-base-server");
        try (Server server = builder.build()) {
            server.start();
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }*/
}
