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
package uk.gov.dbt.ndtp.secure.agent.configuration.auth.TestIANodeConfigurationAdaptor;

import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.configuration.auth.AuthConstants;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.NullSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.PropertiesSource;
import uk.gov.dbt.ndtp.servlet.auth.jwt.configuration.ConfigurationParameters;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verifier.aws.AwsVerificationProvider;

public class TestIANodeConfigurationAdaptor {

    private static final String EXAMPLE_JWKS_URL = "https://example.org/jwks.json";
    private static final String TEST_AWS_REGION = "eu-west-1";

    @AfterMethod
    private void cleanup() {
        Configurator.reset();
    }

    private void setConfiguration(Properties properties) {
        Configurator.setSingleSource(new PropertiesSource(properties));
    }

    @DataProvider(name = "authParameters")
    public Object[][] configurationParameters() {
        return new Object[][] {
                { ConfigurationParameters.PARAM_JWKS_URL, false },
                { AwsVerificationProvider.PARAM_AWS_REGION, false },
                { ConfigurationParameters.PARAM_HEADER_NAMES, true },
                { ConfigurationParameters.PARAM_HEADER_PREFIXES, true },
                { ConfigurationParameters.PARAM_USERNAME_CLAIMS, true },
                { ConfigurationParameters.PARAM_PATH_EXCLUSIONS, false },
                { ConfigurationParameters.PARAM_ALLOWED_CLOCK_SKEW, false }
        };
    }

    @Test(dataProvider = "authParameters")
    public void givenNullConfiguration_whenConfiguringAuth_thenParametersIsNullOrDefault(String param,
                                                                                         boolean hasDefault) {
        // Given
        Configurator.setSingleSource(NullSource.INSTANCE);
        MockAdaptor adaptor = new MockAdaptor();

        // When
        String configValue = adaptor.getParameter(param);

        // Then
        if (hasDefault) {
            Assert.assertNotNull(configValue,
                                 "This parameter should have a non-null default value when not configured");
        } else {
            Assert.assertNull(configValue, "This parameter should be null when not configured");
        }
    }

    @Test
    public void givenJwksUrlConfiguration_whenConfiguringAuth_thenParameterIsReturned_andAwsParameterReturnsNull() {
        // Given
        Properties properties = new Properties();
        properties.put(AuthConstants.ENV_JWKS_URL, EXAMPLE_JWKS_URL);
        setConfiguration(properties);
        MockAdaptor adaptor = new MockAdaptor();

        // When
        String configValue = adaptor.getParameter(ConfigurationParameters.PARAM_JWKS_URL);

        // Then
        Assert.assertNotNull(configValue);
        Assert.assertEquals(configValue, EXAMPLE_JWKS_URL);

        // And
        Assert.assertNull(adaptor.getParameter(AwsVerificationProvider.PARAM_AWS_REGION));
    }

    @Test
    public void givenAwsConfiguration_whenConfiguringAuth_thenParameterIsReturned_andJwksUrlParameterReturnsNull() {
        // Given
        Properties properties = new Properties();
        properties.put(AuthConstants.ENV_JWKS_URL, AuthConstants.AUTH_PREFIX_AWS + TEST_AWS_REGION);
        setConfiguration(properties);
        MockAdaptor adaptor = new MockAdaptor();

        // When
        String configValue = adaptor.getParameter(AwsVerificationProvider.PARAM_AWS_REGION);

        // Then
        Assert.assertNotNull(configValue);
        Assert.assertEquals(configValue, TEST_AWS_REGION);

        // And
        Assert.assertNull(adaptor.getParameter(ConfigurationParameters.PARAM_JWKS_URL));
    }

    @Test
    public void givenPathExclusionsConfiguration_whenConfiguringAuth_thenNullIsReturned() {
        // Given
        Properties properties = new Properties();
        properties.put(ConfigurationParameters.PARAM_PATH_EXCLUSIONS, "/healthz");
        setConfiguration(properties);
        MockAdaptor adaptor = new MockAdaptor();

        // When
        String configValue = adaptor.getParameter(ConfigurationParameters.PARAM_PATH_EXCLUSIONS);

        // Then
        Assert.assertNull(configValue);
    }

}
