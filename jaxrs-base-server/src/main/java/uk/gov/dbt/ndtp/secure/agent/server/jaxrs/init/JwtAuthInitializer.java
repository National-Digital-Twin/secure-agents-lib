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
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.configuration.auth.AuthConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtServletConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.configuration.AutomatedConfiguration;
import uk.gov.dbt.ndtp.servlet.auth.jwt.errors.AuthenticationConfigurationError;

/**
 * A servlet context initializer for initializing a JWT Verifier for use by
 * {@link uk.gov.dbt.ndtp.servlet.auth.jwt.jaxrs3.JwtAuthFilter}
 */
public class JwtAuthInitializer implements ServerConfigInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Attempting to initialize JSON Web Token (JWT) authentication...");

        String jwksUrl = Configurator.get(AuthConstants.ENV_JWKS_URL);
        if (AuthConstants.AUTH_DISABLED.equalsIgnoreCase(jwksUrl)) {
            LOGGER.warn("JWT Authentication explicitly disabled, not configuring it");
            return;
        } else if (AuthConstants.AUTH_DEVELOPMENT.equalsIgnoreCase(jwksUrl)) {
            // Insecure Development Mode (for demos and developer testing)
            LOGGER.error("Authentication in development mode no longer supported!!");
            throw new AuthenticationConfigurationError("Development authentication mode no longer supported");
        }

        // Defer to JWT Servlet Auth libraries automatic configuration mechanism providing our own config adaptor
        AutomatedConfiguration.configure(new ServletConfigurationAdaptor(sce.getServletContext()));
        Object verifier = sce.getServletContext().getAttribute(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER);
        if (verifier != null) {
            LOGGER.info("Successfully configured JWT Authentication with verifier {}", verifier);
        } else {
            LOGGER.warn(
                    "JWT authentication not configured, server is running in secure mode BUT all requests will be rejected as unauthenticated");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER);
        sce.getServletContext().removeAttribute(JwtServletConstants.ATTRIBUTE_JWT_ENGINE);
    }

    @Override
    public String getName() {
        return "JWT Authentication";
    }

    @Override
    public int priority() {
        return 100;
    }
}
