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
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.ConfigurationSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.PropertiesSource;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestPropertiesSource {

    private static final String KEY = "foo.bar";
    private static final String TEST_STRING_VALUE = "abc";
    private static final String TEST_NUMBER_STRING_VALUE = "123";
    private static final int TEST_NUMBER_VALUE = 123;

    @Test
    public void givenEmptyProperties_whenRetrievingByPropertyStyleKey_thenNullIsReturned() {
        // Given
        Properties props = new Properties();
        PropertiesSource source = new PropertiesSource(props);

        // When
        String value = source.get(KEY);

        // Then
        Assert.assertNull(value);
    }

    @Test
    public void givenStringProperty_whenRetrievingByPropertyStyleKey_thenCorrectValueRetrieved() {
        // Given
        Properties props = new Properties();
        props.put(KEY, TEST_STRING_VALUE);
        PropertiesSource source = new PropertiesSource(props);

        // When
        String value = source.get(KEY);

        // Then
        Assert.assertEquals(value, TEST_STRING_VALUE);
    }

    @Test
    public void givenStringProperty_whenRetrievingByEnvironmentStyleKey_thenCorrectValueRetrieved() {
        // Given
        Properties props = new Properties();
        props.put(ConfigurationSource.asEnvironmentVariableKey(KEY), TEST_STRING_VALUE);
        PropertiesSource source = new PropertiesSource(props);

        // When
        String value = source.get(ConfigurationSource.asEnvironmentVariableKey(KEY));

        // Then
        Assert.assertEquals(value, TEST_STRING_VALUE);
    }

    @Test
    public void givenStringPropertyWithEquivalentKeys_whenRetrievingByPropertyStyleKey_thenPropertyStyleValueRetrieved() {
        // Given
        Properties props = new Properties();
        props.put(KEY, TEST_STRING_VALUE);
        props.put(ConfigurationSource.asEnvironmentVariableKey(KEY), "other");
        PropertiesSource source = new PropertiesSource(props);

        // When
        String value = source.get(KEY);

        // Then
        Assert.assertEquals(value, TEST_STRING_VALUE);
    }

    @Test
    public void givenStringPropertyWithEquivalentKeys_whenRetrievingByEnvironmentStyleKey_thenPropertyStyleValueRetrieved() {
        // Given
        Properties props = new Properties();
        props.put(KEY, TEST_STRING_VALUE);
        props.put(ConfigurationSource.asEnvironmentVariableKey(KEY), "other");
        PropertiesSource source = new PropertiesSource(props);

        // When
        String value = source.get(ConfigurationSource.asEnvironmentVariableKey(KEY));

        // Then
        Assert.assertEquals(value, TEST_STRING_VALUE);
    }

    @Test
    public void givenNumericStringProperty_whenRetrievingByPropertyStyleKey_thenCorrectValueRetrieved() {
        // Given
        Properties props = new Properties();
        props.put(KEY, TEST_NUMBER_STRING_VALUE);
        PropertiesSource source = new PropertiesSource(props);

        // When
        String value = source.get(KEY);

        // Then
        Assert.assertEquals(value, TEST_NUMBER_STRING_VALUE);
    }

    @Test
    public void givenNumericIntegerProperty_whenRetrievingByPropertyStyleKey_thenCorrectValueRetrieved() {
        // Given
        Properties props = new Properties();
        props.put(KEY, TEST_NUMBER_VALUE);
        PropertiesSource source = new PropertiesSource(props);

        // When
        String value = source.get(KEY);

        // Then
        Assert.assertEquals(value, TEST_NUMBER_STRING_VALUE);
    }

    @DataProvider(name = "typedValues")
    public Object[][] typedProperties() {
        return new Object[][] {
                { 1.23d, Double.toString(1.23) },
                { false, Boolean.toString(false) },
                { Long.MAX_VALUE, Long.toString(Long.MAX_VALUE) },
                { 'c', "c" },
                { (byte) 0x12, Byte.toString((byte)0x12 ) }
        };
    }

    @Test(dataProvider = "typedValues")
    public void givenTypedProperty_whenRetrievingByPropertyStyleKey_thenCorrectValueRetrieved(Object value, String expectedValue) {
        // Given
        Properties props = new Properties();
        props.put(KEY, value);
        PropertiesSource source = new PropertiesSource(props);

        // When
        String actualValue = source.get(KEY);

        // Then
        Assert.assertEquals(actualValue, expectedValue);
    }

    @Test(dataProvider = "typedValues")
    public void givenTypedProperty_whenRetrievingByEnvironmentStyleKey_thenCorrectValueRetrieved(Object value, String expectedValue) {
        // Given
        Properties props = new Properties();
        props.put(KEY, value);
        PropertiesSource source = new PropertiesSource(props);

        // When
        String actualValue = source.get(ConfigurationSource.asEnvironmentVariableKey(KEY));

        // Then
        Assert.assertEquals(actualValue, expectedValue);
    }
}
