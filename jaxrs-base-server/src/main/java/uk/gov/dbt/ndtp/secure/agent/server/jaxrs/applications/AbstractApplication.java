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

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.errors.ConstraintViolationMapper;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.errors.FallbackExceptionMapper;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.errors.NotAllowedExceptionMapper;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.errors.NotFoundExceptionMapper;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.errors.ParamExceptionMapper;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.FailureLoggingFilter;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.RequestIdFilter;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.AbstractHealthResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.VersionInfoResource;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.writers.ProblemPlainTextWriter;
import uk.gov.dbt.ndtp.servlet.auth.jwt.jaxrs3.JwtAuthFilter;

/**
 * Abstract definition of our JAX-RS Search Application
 * <p>
 * This installs a set of common exception mappers and request/response filters.  Derived application classes should
 * override {@link #getClasses()}, ensuring they call this implementation of it and appending their application specific
 * classes to the list before returning it.
 * </p>
 */
@ApplicationPath("/")
public abstract class AbstractApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplication.class);

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new LinkedHashSet<>();
        // Exception Mappers
        // These translate the error responses into RFC 7807 compliant Problem JSON structures
        classes.add(ConstraintViolationMapper.class);
        classes.add(ParamExceptionMapper.class);
        classes.add(NotFoundExceptionMapper.class);
        classes.add(NotAllowedExceptionMapper.class);
        classes.add(FallbackExceptionMapper.class);

        // Request Filters
        if (this.isAuthEnabled()) {
            classes.add(JwtAuthFilter.class);
        }
        classes.add(RequestIdFilter.class);
        classes.add(FailureLoggingFilter.class);

        // Message Body Writers
        classes.add(ProblemPlainTextWriter.class);

        // Health Resource
        Class<? extends AbstractHealthResource> healthResourceClass = getHealthResourceClass();
        if (healthResourceClass != null) {
            classes.add(healthResourceClass);
        } else {
            LOGGER.warn("No standardised Health Resource available for application {}", this.getClass().getCanonicalName());
        }

        // Version Info Resource
        classes.add(VersionInfoResource.class);

        return classes;
    }

    /**
     * Indicates whether authentication should be enabled
     *
     * @return True if enabled, false otherwise
     */
    protected boolean isAuthEnabled() {
        return false;
    }

    /**
     * Gets the health resource class that should be used for the application
     * <p>
     * The derived application <strong>MAY</strong> choose to return {@code null} here to indicate that they don't want
     * to provide a {@code /healthz} endpoint, or that they are implementing their own endpoint without using
     * {@link AbstractHealthResource}.  However implementations <strong>SHOULD</strong> create their health endpoint by
     * deriving from {@link AbstractHealthResource} wherever possible as it handles common error conditions and DoS
     * mitigations for the endpoint.
     * </p>
     *
     * @return Health Resource class, or {@code null}
     */
    protected abstract Class<? extends AbstractHealthResource> getHealthResourceClass();
}
