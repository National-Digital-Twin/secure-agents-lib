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
package uk.gov.dbt.ndtp.secure.agent.cli.probes.resources;

import jakarta.servlet.ServletContext;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.AbstractHealthResource;

public class ReadinessResource extends AbstractHealthResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadinessResource.class);

    @Override
    protected HealthStatus determineStatus(ServletContext context) {
        try {
            Supplier<HealthStatus> supplier =
                    (Supplier<HealthStatus>) context.getAttribute(ReadinessResource.class.getCanonicalName());
            if (supplier == null) {
                LOGGER.warn("Application considered unhealthy as no readiness supplier configured");
                return HealthStatus.builder()
                                   .healthy(false)
                                   .reasons(List.of("No HealthStatus supplier configured for the server"))
                                   .build();
            } else {
                HealthStatus status = supplier.get();
                if (status != null) {
                    if (!status.isHealthy()) {
                        LOGGER.warn("Application indicates it is unhealthy due to {} reasons: {}",
                                    status.reasons().size(),
                                    StringUtils.join(status.reasons(), ", "));
                    }
                    return status;
                } else {
                    LOGGER.warn("Application considered unhealthy as readiness supplier produced a null status");
                    return HealthStatus.builder()
                                       .healthy(false)
                                       .reasons(List.of("HealthStatus supplier failed to produce a non-null status"))
                                       .build();
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Application considered unhealthy as readiness supplier produced an error", e);
            return HealthStatus.builder().healthy(false).reasons(List.of(e.getMessage())).build();
        }
    }
}
