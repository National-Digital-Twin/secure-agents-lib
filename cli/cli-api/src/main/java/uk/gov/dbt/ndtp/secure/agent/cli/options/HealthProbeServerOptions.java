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
package uk.gov.dbt.ndtp.secure.agent.cli.options;

import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Port;
import com.github.rvesse.airline.annotations.restrictions.PortType;
import java.util.function.Supplier;
import uk.gov.dbt.ndtp.secure.agent.cli.probes.HealthProbeServer;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options that provide a Health Probe Server that can be used to provide HTTP based liveness and readiness probes for
 * CLIs that aren't otherwise HTTP applications
 */
public class HealthProbeServerOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthProbeServerOptions.class);

    @Option(name = { "--health-probe-port" }, description = "Provides a port that the health probe server will run on to offer a minimal HTTP server that supports liveness and readiness probes.  Defaults to 10101.")
    @Port(acceptablePorts = { PortType.USER, PortType.DYNAMIC })
    private int healthProbePort = 10101;

    @Option(name = {
            "--health-probes",
            "--no-health-probes"
    }, description = "Sets whether the Health Probe Server is enabled/disabled.")
    private boolean enableHealthProbeServer = true;

    private HealthProbeServer healthProbes;

    /**
     * Sets up the health probe server
     *
     * @param displayName       Display Name for the server
     * @param readinessSupplier Readiness supplier
     * @param libraries         Libraries whose version information will be incorporated into the liveness probe return
     */
    public void setupHealthProbeServer(String displayName, Supplier<HealthStatus> readinessSupplier,
                                       String... libraries) {
        if (!this.enableHealthProbeServer) {
            LOGGER.warn("Health Probe Server explicitly disabled by user");
            return;
        }
        this.healthProbes = new HealthProbeServer(displayName, this.healthProbePort, readinessSupplier, libraries);
        this.healthProbes.run();
    }

    /**
     * Tears down the health probe server
     */
    public void teardownHealthProbeServer() {
        if (this.healthProbes != null) {
            this.healthProbes.shutdown();
        }
    }
}
