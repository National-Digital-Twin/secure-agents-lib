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
package uk.gov.dbt.ndtp.secure.agent.configuration;

import java.util.Properties;
import java.util.function.Function;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.ConfigurationSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.NullSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.PropertiesSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.SystemPropertiesSource;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestConfigurator {

    private static void verifyNoValue(String key) {
        Assert.assertNull(Configurator.get(key));
    }

    private static <T> void verifyNoValue(String key, Function<String, T> parser) {
        Assert.assertNull(Configurator.get(key, parser, null));
    }

    private static void verifyValuePresent(String key) {
        Assert.assertNotNull(Configurator.get(key));
    }

    private static void verifyValue(String key, String expected) {
        Assert.assertEquals(Configurator.get(key), expected);
    }

    private static <T> void verifyValue(String key, Function<String, T> parser, T expected) {
        Assert.assertEquals(Configurator.get(key, parser, null), expected);
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
    public void configurator_defaults_01() {
        Assert.assertEquals(Configurator.activeSources().size(), 1);
        Assert.assertTrue(Configurator.useAllSources());
        verifyNoValue("NO_SUCH_KEY");
    }

    @Test
    public void configurator_defaults_02() {
        Assert.assertEquals(Configurator.activeSources().size(), 1);
        Assert.assertTrue(Configurator.useAllSources());
        Assert.assertNull(Configurator.get(new String[0]));
    }

    @Test
    public void configurator_defaults_03() {
        if (StringUtils.isBlank(System.getenv("PATH"))) {
            throw new SkipException("No PATH in your environment, test cannot run");
        }

        Assert.assertEquals(Configurator.activeSources().size(), 1);
        Assert.assertTrue(Configurator.useAllSources());
        // A key that really should be guaranteed to exist
        verifyValuePresent("PATH");
    }

    @Test
    public void configurator_null_source_01() {
        Configurator.addSource(NullSource.INSTANCE);
        Configurator.setUseAllSources(false);

        Assert.assertEquals(Configurator.activeSources().size(), 1);
        Assert.assertFalse(Configurator.useAllSources());
        verifyNoValue("PATH");
    }

    @Test
    public void configurator_properties_01() {
        Properties properties = new Properties();
        properties.put("test", "value");

        Configurator.addSource(new PropertiesSource(properties));
        Assert.assertEquals(Configurator.activeSources().size(), 2);
        Assert.assertTrue(Configurator.useAllSources());
        verifyValue("test", "value");
    }

    @Test
    public void configurator_properties_02() {
        Properties properties = new Properties();
        properties.put("test", "1234");

        Configurator.addSource(new PropertiesSource(properties));
        Assert.assertEquals(Configurator.activeSources().size(), 2);
        Assert.assertTrue(Configurator.useAllSources());
        verifyValue("test", Integer::parseInt, 1234);
    }

    @Test
    public void configurator_properties_02b() {
        Properties properties = new Properties();
        properties.put("test", 1234);

        Configurator.addSource(new PropertiesSource(properties));
        Assert.assertEquals(Configurator.activeSources().size(), 2);
        Assert.assertTrue(Configurator.useAllSources());
        verifyValue("test", Integer::parseInt, 1234);
    }

    @Test
    public void configurator_properties_03() {
        Properties properties = new Properties();
        properties.put("test", "abcd");

        Configurator.addSource(new PropertiesSource(properties));
        Assert.assertEquals(Configurator.activeSources().size(), 2);
        Assert.assertTrue(Configurator.useAllSources());
        verifyNoValue("test", Integer::parseInt);
    }

    @Test
    public void configurator_properties_04() {
        Properties properties = new Properties();
        properties.put("test", "value");

        Configurator.addSource(new PropertiesSource(properties));
        Assert.assertEquals(Configurator.activeSources().size(), 2);
        Assert.assertTrue(Configurator.useAllSources());
        // A parser function that forces all values to null
        verifyNoValue("test", x -> null);
    }

    @Test
    public void configurator_system_properties_01() {
        Properties origSysProperties = System.getProperties();
        try {
            Properties properties = new Properties();
            properties.put("test", "value");
            System.setProperties(properties);

            Configurator.addSource(SystemPropertiesSource.INSTANCE);
            Assert.assertEquals(Configurator.activeSources().size(), 2);
            Assert.assertTrue(Configurator.useAllSources());
            verifyValue("test", "value");

            // This source always captures the current version of the System Properties
            Properties otherProperties = new Properties();
            otherProperties.put("test", "another-value");
            System.setProperties(otherProperties);
            verifyValue("test", "another-value");
        } finally {
            System.setProperties(origSysProperties);
        }
    }

    @Test
    public void key_formats_01() {
        String key = "foo.bar";
        Assert.assertEquals(ConfigurationSource.asEnvironmentVariableKey(key), "FOO_BAR");
        Assert.assertEquals(ConfigurationSource.asSystemPropertyKey(key), key);
    }

    @Test
    public void key_formats_02() {
        String key = "FOO_BAR";
        Assert.assertEquals(ConfigurationSource.asEnvironmentVariableKey(key), key);
        Assert.assertEquals(ConfigurationSource.asSystemPropertyKey(key), "foo.bar");
    }
}
