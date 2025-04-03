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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.cli.probes.resources.ReadinessResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications.AbstractAppEntrypoint;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications.CorsConfigurationBuilder;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications.ServerBuilder;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.ServerRuntimeInfo;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;

/**
 * A Health Probe Server
 */
public class HealthProbeServer extends AbstractAppEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthProbeServer.class);

    private final String displayName;
    private final int port;
    private final Supplier<HealthStatus> readinessSupplier;
    private final String[] libraries;

    // Executor service that has a single thread AND makes that thread a daemon thread so it doesn't prevent application
    // shutdown
    private final ExecutorService executorService = Executors.newFixedThreadPool(1, r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });
    private Future<?> future;

    /**
     * Creates a new Health Probe Server
     *
     * @param displayName       Display Name
     * @param port              Port to run on
     * @param readinessSupplier Supplier that can determine whether the application this server is hosted in is
     *                          currently healthy
     * @param libraries         Library versions to report in the liveness check
     */
    public HealthProbeServer(String displayName, int port, Supplier<HealthStatus> readinessSupplier,
                             String... libraries) {
        this.displayName = displayName;
        this.port = port;
        this.readinessSupplier = readinessSupplier;
        this.libraries = libraries;
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create()
                            .application(HealthProbeApplication.class)
                            .withCors(CorsConfigurationBuilder::withDefaults)
                            .displayName(this.displayName)
                            .port(this.port)
                            .allInterfaces()
                            .withListener(ServerRuntimeInfo.class)
                            .withContextAttribute(ReadinessResource.class.getCanonicalName(), this.readinessSupplier)
                            .withMaxThreads(3)
                            .withVersionInfo("cli-probe-server")
                            .withVersionInfo(libraries);
    }

    /**
     * Runs the server and registers a shutdown hook for cleaning up the server
     */
    public void run() {
        if (this.future != null) {
            throw new IllegalStateException("Health Probe Server is already running");
        }
        LOGGER.info("Starting Health Probe Server on port {}", this.port);

        // Launch this on a background daemon thread
        this.future = this.executorService.submit(() -> {
            try {
                Thread.currentThread().setName("HealthProbeServer");
            } catch (Throwable e) {
                // Ignore if unable to set thread name
            }

            this.run(true);
        });

        // Wire up a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shutdown()));
    }

    /**
     * Shuts down the server
     */
    public void shutdown() {
        if (this.future != null) {
            LOGGER.info("Stopping Health Probe Server on port {}", this.port);
            this.future.cancel(true);
            if (this.server != null) {
                this.server.shutdownNow();
            }
            resolveFuture();
        }
    }

    /**
     * Resolves the future that was created when the server was starting by submitting it to the executor service that
     * provides a thread for it to run on
     */
    private void resolveFuture() {
        if (this.future != null) {
            try {
                this.future.get();
            } catch (Throwable e) {
                // Ignore any errors while shutting down
            } finally {
                this.future = null;
            }
        }
    }

    /**
     * Gets the base URL
     *
     * @return Base URL
     */
    public String getBaseUrl() {
        return String.format("http://localhost:%d", this.port);
    }

    /**
     * Gets the liveness probe URL
     *
     * @return Liveness probe URL
     */
    public String getLivenessUrl() {
        return String.format("%s/%s", this.getBaseUrl(), "version-info");
    }

    /**
     * Gets the readiness probe URL
     *
     * @return Readiness probe URL
     */
    public String getReadinessUrl() {
        return String.format("%s/%s", this.getBaseUrl(), "healthz");
    }
}
