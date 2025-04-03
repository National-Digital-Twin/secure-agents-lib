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

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.CrossOriginFilter;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.DefaultCorsConfiguration;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.RequestIdFilter;

public class TestCorsConfigurationBuilder {

    private String asCorsString(String... values) {
        if (values == null || values.length == 0) return "";
        return StringUtils.join(values, DefaultCorsConfiguration.CORS_DELIMITER);
    }

    @Test
    public void cors_builder_01() {
        CorsConfigurationBuilder builder = new CorsConfigurationBuilder(true);
        Map<String, String> params = builder.buildInitParameters();
        verifyDefaults(params);
    }

    private void verifyDefaults(Map<String, String> params) {
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_METHODS_PARAM),
                            asCorsString(DefaultCorsConfiguration.ALLOWED_METHODS));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_HEADERS_PARAM),
                            asCorsString(DefaultCorsConfiguration.ALLOWED_HEADERS));
        Assert.assertEquals(params.get(CrossOriginFilter.EXPOSED_HEADERS_PARAM),
                            asCorsString(DefaultCorsConfiguration.EXPOSED_HEADERS));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM), "true");
    }

    @Test
    public void cors_builder_02() {
        //@formatter:off
        CorsConfigurationBuilder builder
                = new CorsConfigurationBuilder(true)
                        // Add configuration already present in the defaults and ensure it isn't duplicated in the
                        // build parameters
                        .addAllowedMethods("GET")
                        .addAllowedHeaders("Content-Type")
                        .addExposedHeaders(RequestIdFilter.REQUEST_ID);
        //@formatter:on
        Map<String, String> params = builder.buildInitParameters();
        verifyDefaults(params);
    }

    @Test
    public void cors_builder_03() {
        // No defaults used
        CorsConfigurationBuilder builder = new CorsConfigurationBuilder(false);
        Map<String, String> params = builder.buildInitParameters();
        Assert.assertNull(params.get(CrossOriginFilter.ALLOWED_METHODS_PARAM));
        Assert.assertNull(params.get(CrossOriginFilter.ALLOWED_HEADERS_PARAM));
        Assert.assertNull(params.get(CrossOriginFilter.EXPOSED_HEADERS_PARAM));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM), "false");
    }

    @Test
    public void cors_builder_04() {
        // No defaults used
        CorsConfigurationBuilder builder =
                new CorsConfigurationBuilder(false).preflightMaxAge(60)
                                                   .chainPreflight(true)
                                                   .allowCredentials(true)
                                                   .setAllowedMethods("GET", "POST")
                                                   .setAllowedHeaders("Content-Type")
                                                   .setExposedHeaders(RequestIdFilter.REQUEST_ID);
        Map<String, String> params = builder.buildInitParameters();
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_METHODS_PARAM),
                            asCorsString("GET", "POST"));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_HEADERS_PARAM),
                            asCorsString("Content-Type"));
        Assert.assertEquals(params.get(CrossOriginFilter.EXPOSED_HEADERS_PARAM),
                            asCorsString(RequestIdFilter.REQUEST_ID));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM), "true");
        Assert.assertEquals(params.get(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM), "60");
        Assert.assertEquals(params.get(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM), "true");
    }

    @Test
    public void cors_builder_05() {
        // No defaults used
        CorsConfigurationBuilder builder =
                new CorsConfigurationBuilder(false).preflightMaxAge(60)
                                                   .chainPreflight()
                                                   .allowCredentials()
                                                   .setAllowedMethods("GET", "POST")
                                                   .addAllowedMethods("HEAD", "OPTIONS")
                                                   .setAllowedHeaders("Content-Type")
                                                   .addAllowedHeaders(RequestIdFilter.REQUEST_ID)
                                                   .setExposedHeaders(RequestIdFilter.REQUEST_ID)
                                                   .addExposedHeaders("X-Test");
        Map<String, String> params = builder.buildInitParameters();
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_METHODS_PARAM),
                            asCorsString("GET", "POST", "HEAD", "OPTIONS"));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_HEADERS_PARAM),
                            asCorsString("Content-Type", RequestIdFilter.REQUEST_ID));
        Assert.assertEquals(params.get(CrossOriginFilter.EXPOSED_HEADERS_PARAM),
                            asCorsString(RequestIdFilter.REQUEST_ID, "X-Test"));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM), "true");
        Assert.assertEquals(params.get(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM), "60");
        Assert.assertEquals(params.get(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM), "true");
    }

    @Test
    public void cors_builder_06() {
        // No defaults used
        CorsConfigurationBuilder builder =
                new CorsConfigurationBuilder(false).preflightMaxAge(-1)
                                                   .allowCredentials(false)
                                                   .chainPreflight(false)
                                                   .setAllowedMethods("GET", "POST")
                                                   .setAllowedMethods("PUT")
                                                   .setAllowedHeaders("Content-Type")
                                                   .setAllowedHeaders(RequestIdFilter.REQUEST_ID)
                                                   .setExposedHeaders(RequestIdFilter.REQUEST_ID);
        Map<String, String> params = builder.buildInitParameters();
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_METHODS_PARAM),
                            asCorsString("PUT"));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOWED_HEADERS_PARAM),
                            asCorsString(RequestIdFilter.REQUEST_ID));
        Assert.assertEquals(params.get(CrossOriginFilter.EXPOSED_HEADERS_PARAM),
                            asCorsString(RequestIdFilter.REQUEST_ID));
        Assert.assertEquals(params.get(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM), "false");
        Assert.assertNull(params.get(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM));
        Assert.assertEquals(params.get(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM), "false");
    }

    @Test
    public void cors_builder_07() {
        // No defaults used, various things set to null
        String[] NULL = null;
        CorsConfigurationBuilder builder =
                new CorsConfigurationBuilder(false).setAllowedOrigins(NULL)
                                                   .setAllowedTimingOrigins(NULL)
                                                   .setAllowedMethods(NULL)
                                                   .setAllowedHeaders(NULL)
                                                   .setExposedHeaders(NULL);
        Map<String, String> params = builder.buildInitParameters();
        Assert.assertNull(params.get(CrossOriginFilter.ALLOWED_METHODS_PARAM));
        Assert.assertNull(params.get(CrossOriginFilter.ALLOWED_HEADERS_PARAM));
        Assert.assertNull(params.get(CrossOriginFilter.EXPOSED_HEADERS_PARAM));
    }
}
