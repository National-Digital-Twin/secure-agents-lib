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
import jakarta.servlet.ServletContextListener;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Servlet Context listener that automatically discovers other listeners that conform to the {@link ServerConfigInit}
 * interface and have a suitable {@code META-INF/services/uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init.ServerConfigInit}
 * file present
 */
public class ServiceLoadedServletContextInitialiser implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoadedServletContextInitialiser.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        List<ServerConfigInit> inits = ServerConfigInit.getConfigInitialisers();
        LOGGER.info("Discovered {} ServerConfigInit instances via ServiceLoader", inits.size());
        for (ServerConfigInit init : inits) {
            LOGGER.info("Initialising {} (Priority {})...", init.getName(), init.priority());
            init.contextInitialized(sce);
        }
        LOGGER.info("All ServerConfigInit instances were successfully initialised");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        List<ServerConfigInit> inits = ServerConfigInit.getConfigInitialisers();
        // Reverse the order so that we destroy them in the reverse order to which we initialised them
        Collections.reverse(inits);
        LOGGER.info("Discovered {} ServerConfigInit instances via ServiceLoader", inits.size());
        for (ServerConfigInit init : inits) {
            LOGGER.info("Destroying {} (Priority {})...", init.getName(), init.priority());
            init.contextDestroyed(sce);
        }
        LOGGER.info("All ServerConfigInit instances were successfully destroyed");
    }
}
