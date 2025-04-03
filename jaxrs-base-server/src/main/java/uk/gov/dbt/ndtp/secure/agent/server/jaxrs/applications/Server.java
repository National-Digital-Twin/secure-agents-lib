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

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;

/**
 * A wrapper that encapsulates a Jersey Grizzly 2 embedded Java web server with a single JAX-RS web application deployed
 * upon it.  See {@link ServerBuilder} for building an instance.
 */
public class Server implements AutoCloseable {

    private final HttpServer server;
    private final WebappContext webapp;
    private final URI baseUri, localhostUri;

    private final String displayName;

    /**
     * Package private constructor, build an instance via {@link ServerBuilder#create()}
     *
     * @param server      HTTP Server
     * @param baseUri     Base URI at which the server will be listening
     * @param webapp      Web application context
     * @param displayName Display name for the application
     */
    Server(HttpServer server, URI baseUri, WebappContext webapp, String displayName) {
        this.server = server;
        this.webapp = webapp;
        this.baseUri = baseUri;
        this.displayName = displayName;

        if (Objects.equals(this.baseUri.getHost(), ServerBuilder.DEFAULT_HOSTNAME)) {
            this.localhostUri = URI.create(String.format("http://localhost:%d", this.baseUri.getPort()));
        } else {
            this.localhostUri = null;
        }
    }

    /**
     * Gets whether the server is currently running
     *
     * @return True if currently running, false otherwise
     */
    public boolean isRunning() {
        return this.server.isStarted();
    }

    /**
     * Starts the server
     *
     * @throws IOException           Thrown if the server cannot be started
     * @throws IllegalStateException Thrown if the server is already started
     */
    public void start() throws IOException {
        if (server.isStarted()) {
            throw new IllegalStateException("Server already running");
        }
        server.start();
        this.webapp.deploy(server);
    }

    /**
     * Stops the server, allowing 10 seconds for it to gracefully terminate
     *
     * @throws IOException Thrown if there is a problem stopping the server
     */
    public void stop() throws IOException {
        this.stop(10, TimeUnit.SECONDS);
    }

    /**
     * Stops the server with a configurable graceful termination period
     *
     * @param gracePeriod Grace period
     * @param unit        Time unit for the grace period
     * @throws IOException Thrown if there is a problem stopping the server
     */
    public void stop(long gracePeriod, TimeUnit unit) throws IOException {
        if (!server.isStarted()) {
            // Appears to be a bug in Grizzly 2 that unless you call shutdownNow() it never actually reaches a fully
            // stopped state so the above if statement always succeeds
            // https://github.com/eclipse-ee4j/grizzly/issues/2158
            throw new IllegalStateException("Server not running");
        }
        GrizzlyFuture<?> future = this.server.shutdown(gracePeriod, unit);
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while stopping the server");
        } catch (ExecutionException e) {
            throw new IOException("Failed to stop the server", e);
        }
    }

    /**
     * Shuts the server down immediately, ignoring any potential errors that may occur as a result
     */
    public void shutdownNow() {
        this.server.shutdownNow();
    }

    /**
     * Gets the display name of the web application deployed on this server instance
     *
     * @return Display Name
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Gets the hostname/IP address upon which this server is listening
     *
     * @return Hostname/IP address
     */
    public String getHostname() {
        return this.baseUri.getHost();
    }

    /**
     * Gets the port number upon which this server is listening
     *
     * @return Port number
     */
    public int getPort() {
        return this.baseUri.getPort();
    }

    /**
     * Gets the Base URI against which requests to the web application should be made
     *
     * @return Base URI
     */
    public String getBaseUri() {
        String base = this.localhostUri != null ? this.localhostUri.toString() : this.baseUri.toString();
        return base + this.webapp.getContextPath();
    }

    /**
     * Closes the server which causes it to be shutdown if it was still running
     */
    @Override
    public void close() {
        if (this.server.isStarted()) {
            this.server.shutdownNow();
        }
    }
}
