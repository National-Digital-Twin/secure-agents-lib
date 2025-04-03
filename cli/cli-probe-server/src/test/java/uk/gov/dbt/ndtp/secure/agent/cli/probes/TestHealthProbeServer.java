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
package uk.gov.dbt.ndtp.secure.agent.cli.probes;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.event.Level;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.cli.probes.resources.ReadinessResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications.AbstractAppEntrypoint;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.VersionInfo;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.AbstractHealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.RandomPortProvider;

public class TestHealthProbeServer {

    private final static RandomPortProvider TEST_PORT = new RandomPortProvider(1234);
    private final Client client = ClientBuilder.newClient();
    private final TestLogger logger = TestLoggerFactory.getTestLogger(AbstractAppEntrypoint.class);
    private final TestLogger readinessLogger = TestLoggerFactory.getTestLogger(ReadinessResource.class);
    private HealthProbeServer server;

    @AfterMethod
    public void testCleanup() {
        this.server.shutdown();
        this.logger.clearAll();
        this.readinessLogger.clearAll();
    }

    @AfterClass
    public void teardown() {
        this.client.close();
    }

    @Test
    public void givenNoDisplayName_whenStartingProbeServer_thenFails() {
        // Given
        this.server = new HealthProbeServer(null, TEST_PORT.newPort(), null);

        // When
        startServer();

        // Then
        verifyErrorsLogged();
    }

    @Test
    public void givenBadPort_whenStartingProbeServer_thenFails() {
        // Given
        this.server = new HealthProbeServer("Test", -100, null);

        // When
        startServer();

        // Then
        verifyErrorsLogged();
    }

    private void verifyErrorsLogged() {
        Assert.assertNotEquals(logger.getAllLoggingEvents().stream().filter(m -> m.getLevel() == Level.ERROR).count(),
                               0);
    }

    private void verifyNoErrorsLogged() {
        Assert.assertEquals(logger.getAllLoggingEvents().stream().filter(m -> m.getLevel() == Level.ERROR).count(), 0);
    }

    private void verifyReadinessWarningLogged() {
        Assert.assertTrue(readinessLogger.getAllLoggingEvents()
                                         .stream()
                                         .filter(m -> m.getLevel() == Level.WARN)
                                         .anyMatch(m -> StringUtils.containsIgnoreCase(m.getFormattedMessage(),
                                                                                       "unhealthy")));
    }

    private void verifyLiveness(HealthProbeServer server) {
        // Given
        WebTarget target = client.target(server.getLivenessUrl());
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);

        // When
        VersionInfo versionInfo = builder.get(VersionInfo.class);

        // Then
        Assert.assertNotNull(versionInfo);
    }

    private HealthStatus verifyReadiness(HealthProbeServer server, boolean expectedReadiness) {
        // Given
        WebTarget target = client.target(server.getReadinessUrl());
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);

        // When
        Response response = builder.get();

        // Then
        HealthStatus status = response.readEntity(HealthStatus.class);
        Assert.assertEquals(status.isHealthy(), expectedReadiness);
        if (!expectedReadiness) {
            Assert.assertFalse(status.reasons().isEmpty(), "When reporting unready then reasons should be non-empty");
        }
        return status;
    }

    @Test
    public void givenPartialConfiguration_whenRunningProbeServer_thenSuccess_andServerRespondsToRequests() {
        // Given
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(), null);

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyLiveness(server);
        verifyReadiness(server, false);
    }

    @Test
    public void givenAlwaysHealthyConfiguration_whenRunningProbeServer_thenSuccess_andServerRespondsToRequests() {
        // Given
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(),
                                            () -> HealthStatus.builder()
                                                              .healthy(true)
                                                              .build());

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyLiveness(server);
        verifyReadiness(server, true);
    }

    private void startServer() {
        server.run();

        // As the Health Probe Server runs on a background thread it won't be available immediately so if we proceed to
        // try and make HTTP requests to it straight away our tests can fail, thus for these tests want a brief wait
        // after starting the server
        try {
            Thread.sleep(250L);
        } catch (InterruptedException e) {
            // Ignored
        }
    }

    @Test
    public void givenAlwaysHealthyConfiguration_whenRunningProbeServer_thenSuccess_andServerRespondsToManyRequests() {
        // Given
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(),
                                            () -> HealthStatus.builder()
                                                              .healthy(true)
                                                              .build());

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyManyHealthProbesSucceed();
    }

    @Test
    public void givenSlowReadinessCheck_whenRunningProbeServer_thenSuccess_andServerRespondsToManyRequestsEventually() {
        // Given
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(),
                                            () -> {
                                                try {
                                                    Thread.sleep(250);
                                                } catch (InterruptedException e) {
                                                    // Ignore
                                                }
                                                return HealthStatus.builder()
                                                                   .healthy(true)
                                                                   .build();
                                            });

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyManyHealthProbesSucceed();
    }

    private void verifyManyHealthProbesSucceed() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                futures.add(executorService.submit(() -> {
                    verifyLiveness(this.server);
                    verifyReadiness(this.server, true);
                }));
            }
            List<Boolean> results = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get();
                    results.add(true);
                } catch (Throwable e) {
                    System.err.println(e.getMessage());
                    results.add(false);
                }
            }
            long failures = results.stream().filter(x -> !x).count();
            Assert.assertTrue(results.stream().allMatch(x -> x),
                              "Not all requests successfully completed (" + failures + " failed)");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void givenAlwaysUnhealthyConfiguration_whenRunningProbeServer_thenSuccess_andServerRespondsToRequests() {
        // Given
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(),
                                            () -> HealthStatus.builder()
                                                              .healthy(false)
                                                              .reasons(List.of("Something is wrong"))
                                                              .build());

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyLiveness(server);
        verifyReadiness(server, false);
        verifyReadinessWarningLogged();
    }

    @Test
    public void givenAlwaysNullReadinessCheck_whenRunningProbeServer_thenSuccess_andServerRespondsToRequests() {
        // Given
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(),
                                            () -> null);

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyLiveness(server);
        verifyReadiness(server, false);
        verifyReadinessWarningLogged();
    }

    @Test
    public void givenErrorThrowingReadinessCheck_whenRunningProbeServer_thenSuccess_andServerRespondsToRequests() {
        // Given
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(),
                                            () -> {throw new RuntimeException("Failed");});

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyLiveness(server);
        verifyReadiness(server, false);
        verifyReadinessWarningLogged();
    }

    @Test
    public void givenIntermittentlyHealthyConfiguration_whenRunningProbeServer_thenSuccess_andServerRespondsToRequests() {
        // Given
        AtomicInteger counter = new AtomicInteger(0);
        this.server = new HealthProbeServer("Test", TEST_PORT.newPort(), () -> {
            int value = counter.incrementAndGet();
            boolean healthy = value % 2 == 0;
            return HealthStatus.builder()
                               .healthy(healthy)
                               .reasons(healthy ? Collections.emptyList() : List.of("Currently unhealthy"))
                               .build();
        });

        // When
        startServer();

        // Then
        verifyNoErrorsLogged();

        // And
        verifyLiveness(server);
        verifyReadiness(server, false);
        AbstractHealthResource.invalidateCachedStatus();
        verifyReadiness(server, true);
        AbstractHealthResource.invalidateCachedStatus();
        verifyReadiness(server, false);
        AbstractHealthResource.invalidateCachedStatus();
        verifyReadinessWarningLogged();
    }
}
