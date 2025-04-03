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

import uk.gov.dbt.ndtp.jena.abac.lib.AttributesStore;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import java.util.Properties;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.configuration.auth.AuthConstants;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.ConfigurationSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.PropertiesSource;
import uk.gov.dbt.ndtp.servlet.auth.jwt.errors.AuthenticationConfigurationError;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestUserAttributesInitializer {

    private void verifyHierarchyUrlCalculation(String attributesUrl, String expectedHierarchyUrl) {
        String actualHierarchyUrl = UserAttributesInitializer.calculateHierarchyLookupUrl(attributesUrl);
        Assert.assertEquals(actualHierarchyUrl, expectedHierarchyUrl);
    }

    @BeforeMethod
    public void testSetup() {
        Configurator.reset();
    }

    @AfterClass
    public void cleanup() {
        Configurator.reset();
    }

    @Test
    public void calculate_hierarchy_url_01() {
        verifyHierarchyUrlCalculation(null, null);
    }

    @Test
    public void calculate_hierarchy_url_02() {
        verifyHierarchyUrlCalculation("", "");
    }

    @Test
    public void calculate_hierarchy_url_03() {
        verifyHierarchyUrlCalculation("foo", "foo");
    }

    @Test
    public void calculate_hierarchy_url_04() {
        verifyHierarchyUrlCalculation("/users/lookup/{user}", "/hierarchies/lookup/{name}");
    }

    @Test
    public void calculate_hierarchy_url_05() {
        verifyHierarchyUrlCalculation("/user/lookup/{user}", "/hierarchies/lookup/{name}");
    }

    @Test
    public void calculate_hierarchy_url_06() {
        verifyHierarchyUrlCalculation("/lookup/{user}", "/lookup/{name}");
    }

    @Test
    public void calculate_hierarchy_url_07() {
        verifyHierarchyUrlCalculation("/lookup/{foo}", "/lookup/{foo}");
    }

    private static void verifyDestruction(ServletContextEvent sce, ServletContext context,
                                          UserAttributesInitializer initializer) {
        initializer.contextDestroyed(sce);
        verify(context, times(1)).removeAttribute(eq(AttributesStore.class.getCanonicalName()));
    }

    private static UserAttributesInitializer verifyInitialisation(ServletContextEvent sce,
                                                                  ServletContext context) {
        UserAttributesInitializer initializer = new UserAttributesInitializer();
        initializer.contextInitialized(sce);

        verify(context, times(1)).setAttribute(eq(AttributesStore.class.getCanonicalName()), any());
        verify(context, never()).removeAttribute(eq(AttributesStore.class.getCanonicalName()));
        return initializer;
    }

    @Test
    public void user_attributes_disabled() {
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_USER_ATTRIBUTES_URL),
                       AuthConstants.AUTH_DISABLED);
        Configurator.setSingleSource(new PropertiesSource(properties));

        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        UserAttributesInitializer initializer =
                verifyInitialisation(sce, context);
        verifyDestruction(sce, context, initializer);
    }

    @Test(expectedExceptions = AuthenticationConfigurationError.class)
    public void user_attributes_development() {
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_USER_ATTRIBUTES_URL),
                       AuthConstants.AUTH_DEVELOPMENT);
        Configurator.setSingleSource(new PropertiesSource(properties));

        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        verifyInitialisation(sce, context);
    }

    @Test
    public void user_attributes_url_01() {
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_USER_ATTRIBUTES_URL),
                       "https://example.org/users/lookup/{user}");
        Configurator.setSingleSource(new PropertiesSource(properties));

        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        UserAttributesInitializer initializer =
                verifyInitialisation(sce, context);
        verifyDestruction(sce, context, initializer);
    }

    @Test
    public void user_attributes_url_02() {
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_USER_ATTRIBUTES_URL),
                       "https://example.org/users/lookup/{user}");
        properties.put(ConfigurationSource.asSystemPropertyKey(AuthConstants.ENV_ATTRIBUTE_HIERARCHY_URL),
                       "https://example.org/labels/hierarchies/{hierarchy}");
        Configurator.setSingleSource(new PropertiesSource(properties));

        ServletContextEvent sce = mock(ServletContextEvent.class);
        ServletContext context = mock(ServletContext.class);
        when(sce.getServletContext()).thenReturn(context);

        UserAttributesInitializer initializer =
                verifyInitialisation(sce, context);
        verifyDestruction(sce, context, initializer);
    }
}
