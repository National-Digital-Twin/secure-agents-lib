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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.CrossOriginFilter;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters.DefaultCorsConfiguration;

/**
 * A builder used to define CORS (Cross Origin Resource Sharing) parameters for the server
 */
public class CorsConfigurationBuilder {

    private final Set<String> allowedMethods, allowedHeaders, exposedHeaders, allowedOrigins, allowedTimingOrigins;

    private boolean allowCredentials = false, chainPreFlight = true;
    private Integer preflightMaxAge = null;

    /**
     * Creates a new CORS builder that uses our default configuration
     */
    public CorsConfigurationBuilder() {
        this(true);
    }

    /**
     * Creates a new CORS builder that optionally uses our defaults
     *
     * @param useDefaults Whether to use our defaults from {@link DefaultCorsConfiguration}
     */
    public CorsConfigurationBuilder(boolean useDefaults) {
        this.allowedMethods = new LinkedHashSet<>();
        this.allowedHeaders = new LinkedHashSet<>();
        this.exposedHeaders = new LinkedHashSet<>();
        this.allowedOrigins = new LinkedHashSet<>();
        this.allowedTimingOrigins = new LinkedHashSet<>();
        if (useDefaults) {
            // Set up our defaults
            withDefaults();
        }
    }

    /**
     * Sets up some default CORS configuration
     * <p>
     * This is automatically called if you call one of the constructor variants that enables our defaults
     * </p>
     *
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder withDefaults() {
        Collections.addAll(this.allowedMethods, DefaultCorsConfiguration.ALLOWED_METHODS);
        Collections.addAll(this.allowedHeaders, DefaultCorsConfiguration.ALLOWED_HEADERS);
        Collections.addAll(this.exposedHeaders, DefaultCorsConfiguration.EXPOSED_HEADERS);
        this.allowCredentials = true;
        return this;
    }

    /**
     * Sets the allowed HTTP Methods clearing any previously configured allowed methods
     * <p>
     * If you want to append to the previously configured allowed methods then use {@link #addAllowedMethods(String[])}
     * instead.
     * </p>
     *
     * @param allowedMethods Allowed methods
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder setAllowedMethods(String... allowedMethods) {
        this.allowedMethods.clear();
        return addAllowedMethods(allowedMethods);
    }

    /**
     * Adds to the allowed HTTP Methods
     * <p>
     * If you want to replace to the previously configured allowed methods then use {@link #setAllowedMethods(String[])}
     * instead.
     * </p>
     *
     * @param allowedMethods Allowed methods
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder addAllowedMethods(String... allowedMethods) {
        if (allowedMethods != null) {
            Collections.addAll(this.allowedMethods, allowedMethods);
        }
        return this;
    }

    /**
     * Sets the allowed HTTP Headers clearing any previously configured allowed headers
     * <p>
     * If you want to append to the previously configured allowed headers then use {@link #addAllowedHeaders(String[])}
     * instead.
     * </p>
     *
     * @param allowedHeaders Allowed headers
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder setAllowedHeaders(String... allowedHeaders) {
        this.allowedHeaders.clear();
        return addAllowedHeaders(allowedHeaders);
    }

    /**
     * Adds to the allowed HTTP Headers
     * <p>
     * If you want to replace to the previously configured allowed headers then use {@link #setAllowedHeaders(String[])}
     * instead.
     * </p>
     *
     * @param allowedHeaders Allowed headers
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder addAllowedHeaders(String... allowedHeaders) {
        if (allowedHeaders != null) {
            Collections.addAll(this.allowedHeaders, allowedHeaders);
        }
        return this;
    }

    /**
     * Sets the exposed HTTP Headers clearing any previously configured exposed headers
     * <p>
     * If you want to append to the previously configured exposed headers then use {@link #addExposedHeaders(String[])}
     * instead.
     * </p>
     *
     * @param exposedHeaders Exposed headers
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder setExposedHeaders(String... exposedHeaders) {
        this.exposedHeaders.clear();
        return addExposedHeaders(exposedHeaders);
    }

    /**
     * Adds to the exposed HTTP Headers
     * <p>
     * If you want to replace to the previously configured exposed headers then use {@link #setExposedHeaders(String[])}
     * instead.
     * </p>
     *
     * @param exposedHeaders Exposed headers
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder addExposedHeaders(String... exposedHeaders) {
        if (exposedHeaders != null) {
            Collections.addAll(this.exposedHeaders, exposedHeaders);
        }
        return this;
    }

    /**
     * Sets the allowed HTTP Origins clearing any previously configured allowed origins
     * <p>
     * If you want to append to the previously configured allowed origins then use {@link #addAllowedOrigins(String[])}
     * instead.
     * </p>
     *
     * @param allowedOrigins Allowed origins
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder setAllowedOrigins(String... allowedOrigins) {
        this.allowedOrigins.clear();
        return addAllowedOrigins(allowedOrigins);
    }

    /**
     * Adds to the allowed HTTP Origins
     * <p>
     * If you want to replace to the previously configured allowed origins then use {@link #setAllowedOrigins(String[])}
     * instead.
     * </p>
     *
     * @param allowedOrigins Allowed origins
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder addAllowedOrigins(String... allowedOrigins) {
        if (allowedOrigins != null) {
            Collections.addAll(this.allowedOrigins, allowedOrigins);
        }
        return this;
    }

    /**
     * Sets the allowed HTTP Origins that can time the resource clearing any previously configured allowed timing
     * origins
     * <p>
     * If you want to append to the previously configured allowed timing origins then use
     * {@link #addAllowedTimingOrigins(String[])} instead.
     * </p>
     *
     * @param allowedTimingOrigins Allowed timing origins
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder setAllowedTimingOrigins(String... allowedTimingOrigins) {
        this.allowedTimingOrigins.clear();
        return addAllowedTimingOrigins(allowedTimingOrigins);
    }

    /**
     * Adds to the allowed HTTP Timing Origins
     * <p>
     * If you want to replace to the previously configured allowed timing origins then use
     * {@link #setAllowedTimingOrigins(String[])} instead.
     * </p>
     *
     * @param allowedTimingOrigins Allowed timing origins
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder addAllowedTimingOrigins(String... allowedTimingOrigins) {
        if (allowedTimingOrigins != null) {
            Collections.addAll(this.allowedTimingOrigins, allowedTimingOrigins);
        }
        return this;
    }

    /**
     * Sets that the CORS configuration will allow pre-flight requests to include credentials
     *
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder allowCredentials() {
        return allowCredentials(true);
    }

    /**
     * Sets whether the CORS configuration will allow pre-flight requests to include credentials
     *
     * @param allow Whether credentials are allowed
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder allowCredentials(boolean allow) {
        this.allowCredentials = allow;
        return this;
    }

    /**
     * Sets whether preflight requests will be chained, i.e. forwarded to the actual application for further handling
     *
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder chainPreflight() {
        return chainPreflight(true);
    }

    /**
     * Sets whether preflight requests will be chained, i.e. forwarded to the actual application for further handling,
     * if {@code false} then only the {@link CrossOriginFilter} will process preflight requests
     *
     * @param preFlight Whether CORS pre-flight requests are chained to the actual application
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder chainPreflight(boolean preFlight) {
        this.chainPreFlight = preFlight;
        return this;
    }

    /**
     * Sets how long preflight request results can be cached by the browser
     *
     * @param maxAgeSeconds Max age in seconds
     * @return CORS Configuration Builder
     */
    public CorsConfigurationBuilder preflightMaxAge(int maxAgeSeconds) {
        this.preflightMaxAge = maxAgeSeconds;
        return this;
    }

    /**
     * Sets a {@link CrossOriginFilter} init parameter if the provided value is non-empty
     *
     * @param params    Init parameters
     * @param paramName Parameter name
     * @param value     Value, may be empty in which case nothing is set and {@link CrossOriginFilter} will use its
     *                  default value(s) for that parameter
     */
    private void setIfConfigured(Map<String, String> params, String paramName, Collection<String> value) {
        if (!value.isEmpty()) {
            params.put(paramName, StringUtils.join(value, DefaultCorsConfiguration.CORS_DELIMITER));
        }
    }

    /**
     * Builds the init parameters to pass to the {@link CrossOriginFilter} to configure it with the configuration
     * provided to this builder
     *
     * @return Init parameters
     */
    public Map<String, String> buildInitParameters() {
        Map<String, String> params = new HashMap<>();
        setIfConfigured(params, CrossOriginFilter.ALLOWED_METHODS_PARAM, this.allowedMethods);
        setIfConfigured(params, CrossOriginFilter.ALLOWED_HEADERS_PARAM, this.allowedHeaders);
        setIfConfigured(params, CrossOriginFilter.EXPOSED_HEADERS_PARAM, this.exposedHeaders);
        setIfConfigured(params, CrossOriginFilter.ALLOWED_ORIGINS_PARAM, this.allowedOrigins);
        setIfConfigured(params, CrossOriginFilter.ALLOWED_TIMING_ORIGINS_PARAM, this.allowedTimingOrigins);
        params.put(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, Boolean.toString(allowCredentials));
        params.put(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, Boolean.toString(chainPreFlight));
        if (this.preflightMaxAge != null && this.preflightMaxAge > 0) {
            params.put(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM, Integer.toString(this.preflightMaxAge));
        }
        return params;
    }
}
