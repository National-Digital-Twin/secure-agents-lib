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

import jakarta.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A {@code ServletContextListener} that will be auto-discovered via {@link ServiceLoader#load(Class)} and whose
 * initialisation order is controlled via its declared {@link #priority()} value
 */
public interface ServerConfigInit extends ServletContextListener {

    /**
     * Gets the configuration initialisers that are detected via {@link ServiceLoader#load(Class)}
     *
     * @return Detected initialisers
     */
    static List<ServerConfigInit> getConfigInitialisers() {
        ServiceLoader<ServerConfigInit> loader = ServiceLoader.load(ServerConfigInit.class);

        List<ServerConfigInit> loadedInits = new ArrayList<>();
        for (ServerConfigInit serverConfigInit : loader) {
            loadedInits.add(serverConfigInit);
        }

        loadedInits.sort(InitPriorityComparator.INSTANCE);

        return loadedInits;
    }

    /**
     * Gets the name for the initialiser
     *
     * @return Name
     */
    String getName();

    /**
     * Gets the priority for the initialiser
     *
     * @return Priority, higher values indicate higher priority i.e. the initialiser should run sooner
     */
    int priority();
}
