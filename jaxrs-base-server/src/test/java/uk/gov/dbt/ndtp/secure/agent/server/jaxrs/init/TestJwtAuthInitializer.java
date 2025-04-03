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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import java.util.Properties;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.ConfigurationSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.configuration.auth.AuthConstants;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.PropertiesSource;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtServletConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.errors.AuthenticationConfigurationError;

public class TestJwtAuthInitializer {
    @BeforeMethod
    public void testSetup() {
        Configurator.reset();
    }

    @AfterClass
    public void cleanup() {
        Configurator.reset();
    }

    private static void verifyDestruction(ServletContextEvent sce, ServletContext context,
                                          JwtAuthInitializer initializer) {
        // When
        initializer.contextDestroyed(sce);

        // Then
        verify(context, times(1)).removeAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER));
        verify(context, times(1)).removeAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_ENGINE));
    }

    private static JwtAuthInitializer verifyNotConfigured(ServletContextEvent sce, ServletContext context) {
        // Given
        JwtAuthInitializer initializer = new JwtAuthInitializer();

        // When
        initializer.contextInitialized(sce);

        // Then
        verify(context, never()).setAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_ENGINE), any());
        verify(context, never()).setAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER), any());
        verify(context, never()).removeAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER));
        verify(context, never()).removeAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_ENGINE));

        return initializer;
    }

    private static JwtAuthInitializer verifyInitialisation(ServletContextEvent sce, ServletContext context,
                                                           boolean expectVerifierConfigured) {
        // Given
        JwtAuthInitializer initializer = new JwtAuthInitializer();

        // When
        initializer.contextInitialized(sce);

        // Then
        verify(context, expectVerifierConfigured ? never() : times(1)).setAttribute(
                eq(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER), any());
        verify(context, times(1)).setAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_ENGINE), any());
        verify(context, never()).removeAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER));
        verify(context, never()).removeAttribute(eq(JwtServletConstants.ATTRIBUTE_JWT_ENGINE));

        return initializer;
    }

    @Test
    public void givenDisabledAuthMode_whenInitialising_thenNothingIsConfigured() {
        // Given
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       AuthConstants.AUTH_DISABLED);
        Configurator.setSingleSource(new PropertiesSource(properties));
        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        // When and Then
        JwtAuthInitializer initializer = verifyNotConfigured(sce, context);
        verifyDestruction(sce, context, initializer);
    }

    @Test(expectedExceptions = AuthenticationConfigurationError.class)
    public void givenDevelopmentAuthMode_whenInitialising_thenError() {
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       AuthConstants.AUTH_DEVELOPMENT);
        Configurator.setSingleSource(new PropertiesSource(properties));

        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        // When
        verifyInitialisation(sce, context, false);
    }

    @Test
    public void givenAwsAuthMode_whenInitialising_thenConfigured() {
        // Given
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       AuthConstants.AUTH_PREFIX_AWS + "eu-west-2");
        Configurator.setSingleSource(new PropertiesSource(properties));
        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        // When and Then
        JwtAuthInitializer initializer = verifyInitialisation(sce, context, false);
        verifyDestruction(sce, context, initializer);
    }

    @Test
    public void givenValidJwksUrl_whenInitialising_thenConfigured() {
        // Given
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL),
                       "https://example.org/openid-connect/keys");
        Configurator.setSingleSource(new PropertiesSource(properties));
        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        // When and Then
        JwtAuthInitializer initializer = verifyInitialisation(sce, context, false);
        verifyDestruction(sce, context, initializer);
    }

    @Test
    public void givenInvalidJwksUrl_whenInitialising_thenNoVerifierConfigured() {
        // Given
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_JWKS_URL), "not a valid url");
        Configurator.setSingleSource(new PropertiesSource(properties));
        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        // When and Then
        JwtAuthInitializer initializer = verifyInitialisation(sce, context, true);
        verifyDestruction(sce, context, initializer);
    }

}
