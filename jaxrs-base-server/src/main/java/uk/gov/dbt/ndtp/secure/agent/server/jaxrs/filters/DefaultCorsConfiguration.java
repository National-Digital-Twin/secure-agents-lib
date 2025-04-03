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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications.CorsConfigurationBuilder;

/**
 * Provides our default CORS configuration as used by the
 * {@link CorsConfigurationBuilder}
 */
public class DefaultCorsConfiguration {

    private DefaultCorsConfiguration() {
    }

    /**
     * The delimiter used in CORS headers to separate the items in a list
     */
    public static final String CORS_DELIMITER = ", ";

    /**
     * The HTTP Headers that may be accessed by scripts in response to a pre-flight request
     * <p>
     * Per <a href="https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_response_header">CORS Safe-listed
     * Response headers</a> there's a few headers that are automatically safe-listed by browsers, these are additional
     * headers not on that list that our clients might wish to access.
     * </p>
     * <p>
     * Currently this only includes our custom {@value RequestIdFilter#REQUEST_ID} header.
     * </p>
     */
    public static final String[] EXPOSED_HEADERS = new String[] { RequestIdFilter.REQUEST_ID };

    /**
     * The HTTP Headers that are allowed in CORS requests
     * <p>
     * Per <a href="https://developer.mozilla.org/en-US/docs/Glossary/CORS-safelisted_request_header">CORS Safe-listed
     * Request Headers</a> there's a few headers that are automatically safe-listed by browsers, these are just the
     * additional headers not on that list that our clients might wish to send in a pre-flight request. Note that even
     * for those safe-listed headers their values have some restrictions so we still explicitly include some of those
     * ({@value HttpHeaders#ACCEPT} and {@value HttpHeaders#CONTENT_TYPE}) where we expect values outside of those
     * safe-list restrictions.
     * </p>
     * <p>
     * In particular, we add the {@value HttpHeaders#AUTHORIZATION} header, this is useful when testing the API from the
     * Swagger UI as that always includes it even in pre-flight requests.  We know some APIs make use of multipart
     * requests for uploading data/configuration files so we add {@value HttpHeaders#CONTENT_DISPOSITION}.  Finally we
     * allow our custom {@value RequestIdFilter#REQUEST_ID} header which we use to uniquely identify each request the
     * server processes.
     * </p>
     */
    public static final String[] ALLOWED_HEADERS = new String[] {
            HttpHeaders.ACCEPT,
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CONTENT_DISPOSITION,
            RequestIdFilter.REQUEST_ID
    };

    /**
     * The HTTP methods that we allow by CORS requests
     */
    public static final String[] ALLOWED_METHODS = new String[] {
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.HEAD,
            HttpMethod.OPTIONS,
            HttpMethod.PATCH
    };
}
