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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init;

import jakarta.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.observability.RuntimeInfo;

/**
 * A Servlet context initialiser that will print information about the servers runtime environment at startup
 */
public class ServerRuntimeInfo implements ServerConfigInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRuntimeInfo.class);

    @Override
    public String getName() {
        return "Server Runtime Environment Information";
    }

    @Override
    public int priority() {
        // Highest priorities get called first and always want to dump runtime environment information first
        return Integer.MAX_VALUE;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // General runtime environment information - Memory, Java Version and OS
        RuntimeInfo.printRuntimeInfo(LOGGER);

        // Also dump LibraryVersion information
        RuntimeInfo.printLibraryVersions(LOGGER);
    }
}
