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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract application entrypoint that allows running the application using an embedded Grizzly HTTP Server without
 * needing any additional Java Servlet container.
 */
public abstract class AbstractAppEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAppEntrypoint.class);

    /**
     * The server instance
     */
    protected Server server;

    /**
     * Builds the application server
     *
     * @return Server builder
     */
    protected abstract ServerBuilder buildServer();

    /**
     * Attempts to run the server.
     * <p>
     * Derived classes should call this from their {@code main(String[] args)} method assuming they have appropriately
     * implemented {@link #buildServer()} to build their desired application server
     * </p>
     *
     * @param block Whether to block until the server is terminated by an interrupt signal.  If {@code true} then
     *              calling this method will block, if {@code false} then this method starts the server and returns,
     *              calling code is then responsible for terminating the server.
     */
    protected final void run(boolean block) {

        try {
            ServerBuilder builder = buildServer();
            this.server = builder.build();
            // Try to rename the thread to the servers display name, otherwise the default JVM thread name is super long
            // and ugly
            try {
                Thread.currentThread().setName(this.server.getDisplayName());
            } catch (Throwable t) {
                // Ignore failures to change thread name, just use the default JVM thread name
            }
            LOGGER.info("Attempting to start {}...", server.getDisplayName());

            this.server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdownNow()));

            LOGGER.info("{} started, check prior logs for any application startup errors.", server.getDisplayName());
            LOGGER.info("{} available at {}", server.getDisplayName(), server.getBaseUri());
            LOGGER.info("Stop the server by sending an interrupt to this process e.g. using CTRL+C");

            if (block) {
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException e) {
                    // There are 2 scenarios in which this occurs:
                    // 1 - The JVM is shutting down, in which case our shutdown hook might already have fired and called
                    //     shutdown but no harm in calling it again
                    // 2 - The server was started on a background thread that has been interrupted (e.g. integration
                    //     testing) in which case we need the server to be shutdown to free up the port and not leak
                    //     resources
                    LOGGER.info("Server process/thread interrupted, shutting down now...");
                    this.server.shutdownNow();
                }
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Bad server configuration: {}", e.getMessage());
        } catch (IllegalStateException e) {
            LOGGER.error("Bad/insufficient server configuration: {}", e.getMessage());
        } catch (Throwable e) {
            LOGGER.error("Unexpected error:", e);
        }
    }
}
