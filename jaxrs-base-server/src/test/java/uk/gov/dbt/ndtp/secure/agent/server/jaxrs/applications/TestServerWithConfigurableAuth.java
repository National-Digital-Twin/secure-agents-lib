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

import io.jsonwebtoken.Jwts;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.ConfigurationSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.configuration.auth.AuthConstants;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.PropertiesSource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.JwtAuthInitializer;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.MockAuthInit;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.TestInit;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.DataResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.JwksResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtHttpConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.configuration.ConfigurationParameters;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verification.TestKeyUtils;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verifier.aws.AwsConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verifier.aws.AwsElbKeyUrlRegistry;

public class TestServerWithConfigurableAuth extends AbstractAppEntrypoint {
    private static final RandomPortProvider PORT = new RandomPortProvider(22334);

    private final Client client = ClientBuilder.newClient();

    private File secretKey;

    private final MockKeyServer keyServer = new MockKeyServer(12345);

    @BeforeClass
    public void setup() throws Exception {
        // Make the secret key available in a temporary file as we need that for our tests
        this.secretKey = TestKeyUtils.saveKeyToFile(Base64.getEncoder().encode(MockAuthInit.SIGNING_KEY.getEncoded()));

        // Start the Mock Key Server
        this.keyServer.start();
        this.keyServer.registerAsAwsRegion("custom");
    }


    @BeforeMethod
    public void testSetup() {
        DataResource.reset();
        TestInit.reset();
        Configurator.reset();
    }

    @AfterClass
    public void teardown() {
        this.client.close();
        Configurator.reset();

        // Clean up temporary keys
        this.secretKey.delete();
        this.keyServer.stop();
        AwsElbKeyUrlRegistry.reset();
        JwksResource.reset();
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create().application(MockApplicationWithAuth.class)
                            // Use a different port for each test just in case one test is slow to teardown the server
                            .port(PORT.newPort()).displayName("Test");
    }

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    private String createAwsToken(String keyId, String username) {
        return Jwts.builder()
                   .header()
                   .keyId(keyId)
                   .and()
                   .subject(username)
                   .expiration(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)))
                   .signWith(this.keyServer.getPrivateKey(keyId))
                   .compact();
    }

    @DataProvider(name = "keyIds")
    public Object[][] keyIds() {
        return this.keyServer.getKeyIds();
    }

    @Test
    public void givenServerWithAuthNotConfigured_whenMakingRequests_thenInternalServerError() throws IOException {
        // Given
        ServerBuilder builder = buildServer();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

            // Then
            // No auth configured so 500 Internal Server Error
            TestServer.verifyError(invocation, SyncInvoker::get, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            server.shutdownNow();
        }
    }

    @Test
    public void givenServerWithAuthConfigured_whenMakingRequests_thenAuthenticationIsRequired_andAuthenticatedRequestSucceed() throws
            IOException {
        // Given
        ServerBuilder builder = buildServer().withListener(JwtAuthInitializer.class);
        configureAuthentication();
        try (Server server = builder.build()) {
            server.start();

            // When and Then
            verifyAuthenticationRequired(server, JwtHttpConstants.HEADER_AUTHORIZATION,
                                         String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                       MockAuthInit.createToken("test")));


            server.shutdownNow();
        }
    }

    @Test
    public void givenServerWithAuthConfiguredWithCustomHeader_whenMakingRequests_thenAuthenticatedRequestSucceed_andRequestsWithNormalHeaderFail_andRequestsWithoutPrefixFail() throws
            IOException {
        // Given
        ServerBuilder builder = buildServer().withListener(JwtAuthInitializer.class);
        Properties properties = configureAuthentication();
        configureCustomAuthHeader(properties);
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget target = forServer(server, "/healthz");
            Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON)
                                                  .header(MockAuthInit.X_CUSTOM_AUTH,
                                                          String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                                        MockAuthInit.createToken("test")));

            // Then
            TestServer.verifyResponse(invocation.get(), Response.Status.NO_CONTENT);

            // And
            // Since we configured only a custom authentication header the standard header is ignored
            invocation = target.request(MediaType.APPLICATION_JSON)
                               .header(JwtHttpConstants.HEADER_AUTHORIZATION,
                                       String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                     MockAuthInit.createToken("test")));
            TestServer.verifyError(invocation, SyncInvoker::get, Response.Status.FORBIDDEN.getStatusCode());

            // And
            // Our custom header requires a prefix so passing just the token also fails
            invocation = target.request(MediaType.APPLICATION_JSON)
                               .header(JwtHttpConstants.HEADER_AUTHORIZATION,
                                       MockAuthInit.createToken("test"));
            TestServer.verifyError(invocation, SyncInvoker::get, Response.Status.FORBIDDEN.getStatusCode());

            server.shutdownNow();
        }
    }

    private Properties configureAuthentication() {
        Properties properties = new Properties();
        properties.put(ConfigurationParameters.PARAM_SECRET_KEY, this.secretKey.getAbsolutePath());
        Configurator.setSingleSource(new PropertiesSource(properties));
        return properties;
    }

    @Test
    public void givenServerWithAuthConfiguredWithPathExclusions_whenMakingRequests_thenAuthenticationIsNotRequiredForExcludedPaths_andOtherPathsRequireAuthentication() throws
            IOException {
        // Given
        ServerBuilder builder = buildServer().withListener(JwtAuthInitializer.class).withAuthExclusion("/healthz");
        configureAuthentication();
        try (Server server = builder.build()) {
            server.start();

            // When
            WebTarget healthz = forServer(server, "/healthz");
            Invocation.Builder invocation = healthz.request(MediaType.APPLICATION_JSON);

            // Then
            // As we added /healthz to exclusions we can hit this without any authentication
            TestServer.verifyResponse(invocation.get(), Response.Status.NO_CONTENT);

            // And
            // Other paths should continue to require authentication
            WebTarget target = forServer(server, "/data/test");
            invocation = target.request(MediaType.APPLICATION_JSON);
            TestServer.verifyError(invocation, SyncInvoker::get, Response.Status.FORBIDDEN.getStatusCode());

            server.shutdownNow();
        }
    }

    @Test(dataProvider = "keyIds")
    public void givenServerWithAwsAuthConfigured_whenMakingRequests_thenAuthenticationIsRequired_andAuthenticatedRequestSucceed(
            String keyId) throws IOException {
        ensureECKeyForAwsTests(keyId);

        // Given
        ServerBuilder builder = buildServer().withListener(JwtAuthInitializer.class);
        Properties properties = new Properties();
        configureAwsElbAuth(properties);
        Configurator.setSingleSource(new PropertiesSource(properties));
        try (Server server = builder.build()) {
            server.start();

            // When and Then
            verifyAuthenticationRequired(server, AwsConstants.HEADER_DATA, createAwsToken(keyId, "test"));

            server.shutdownNow();
        }
    }

    private void ensureECKeyForAwsTests(String keyId) {
        Key privateKey = this.keyServer.getPrivateKey(keyId);
        if (!(privateKey instanceof ECPrivateKey)) {
            throw new SkipException("AWS ELB only uses EC Keys");
        }
    }

    @Test(dataProvider = "keyIds")
    public void givenServerWithAwsAuthConfiguredWithCustomHeader_whenMakingRequests_thenAuthenticationIsRequired_andAuthenticatedRequestSucceed(
            String keyId) throws IOException {
        ensureECKeyForAwsTests(keyId);

        // Given
        ServerBuilder builder = buildServer().withListener(JwtAuthInitializer.class);
        Properties properties = new Properties();
        configureAwsElbAuth(properties);
        configureCustomAuthHeader(properties);
        Configurator.setSingleSource(new PropertiesSource(properties));
        try (Server server = builder.build()) {
            server.start();

            // When and Then
            verifyAuthenticationRequired(server, MockAuthInit.X_CUSTOM_AUTH,
                                         String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                       createAwsToken(keyId, "test")));

            server.shutdownNow();
        }
    }

    private static void configureAwsElbAuth(Properties properties) {
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       AuthConstants.AUTH_PREFIX_AWS + "custom");
    }

    @Test(dataProvider = "keyIds")
    public void givenServerWithJwksAuthConfigured_whenMakingRequests_thenAuthenticationIsRequired_andAuthenticatedRequestSucceed(
            String keyId) throws IOException {
        // Given
        ServerBuilder builder = buildServer().withListener(JwtAuthInitializer.class);
        Properties properties = new Properties();
        configureJwksAuth(properties);
        Configurator.setSingleSource(new PropertiesSource(properties));
        try (Server server = builder.build()) {
            server.start();

            // When and Then
            verifyAuthenticationRequired(server, AwsConstants.HEADER_DATA, createAwsToken(keyId, "test"));

            server.shutdownNow();
        }
    }

    private void configureJwksAuth(Properties properties) {
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       this.keyServer.getJwksUrl());
    }

    @Test(dataProvider = "keyIds")
    public void givenServerWithJwksAuthConfiguredWithCustomHeader_whenMakingRequests_thenAuthenticationIsRequired_andAuthenticatedRequestSucceed(
            String keyId) throws IOException {
        // Given
        ServerBuilder builder = buildServer().withListener(JwtAuthInitializer.class);
        Properties properties = new Properties();
        configureJwksAuth(properties);
        configureCustomAuthHeader(properties);
        Configurator.setSingleSource(new PropertiesSource(properties));
        try (Server server = builder.build()) {
            server.start();

            // When and Then
            verifyAuthenticationRequired(server, MockAuthInit.X_CUSTOM_AUTH,
                                         String.format("%s %s", JwtHttpConstants.AUTH_SCHEME_BEARER,
                                                       createAwsToken(keyId, "test")));

            server.shutdownNow();
        }
    }

    private void verifyAuthenticationRequired(Server server, String authHeader, String authHeaderValue) {
        // When
        WebTarget target = forServer(server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);

        // Then
        // Properly configured auth but no credentials so 403 Forbidden
        TestServer.verifyError(invocation, SyncInvoker::get, Response.Status.FORBIDDEN.getStatusCode());

        // And
        // After adding credentials should get a success response
        invocation = invocation.header(authHeader, authHeaderValue);
        TestServer.verifyResponse(invocation.get(), Response.Status.NO_CONTENT);
    }

    private static void configureCustomAuthHeader(Properties properties) {
        properties.put(ConfigurationParameters.PARAM_HEADER_NAMES, MockAuthInit.X_CUSTOM_AUTH);
        properties.put(ConfigurationParameters.PARAM_HEADER_PREFIXES, JwtHttpConstants.AUTH_SCHEME_BEARER);
    }
}
