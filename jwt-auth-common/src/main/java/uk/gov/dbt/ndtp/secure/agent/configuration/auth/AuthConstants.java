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
package uk.gov.dbt.ndtp.secure.agent.configuration.auth;

import org.apache.commons.lang3.StringUtils;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtHttpConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verifier.aws.AwsConstants;

/**
 * Provides constants related to configuring authentication
 */
public class AuthConstants {

    /**
     * Private constructor prevents instantiation
     */
    private AuthConstants() {
    }

    /**
     * Environment variable that specifies the User Attributes service URL used to obtain attributes for an
     * authenticated user
     */
    public static final String ENV_USER_ATTRIBUTES_URL = "USER_ATTRIBUTES_URL";

    /**
     * Environment variable that specifies the Attributes Hierarchy service URL to obtain attributes hierarchies from,
     * attribute hierarchies are used in evaluating whether user attributes meet the security labels on data
     */
    public static final String ENV_ATTRIBUTE_HIERARCHY_URL = "ATTRIBUTE_HIERARCHY_URL";

    /**
     * Environment variable that specifies the JSON Web Key Set (JWKS) URL to use to obtain the public keys for
     * verifying JSON Web Tokens (JWTs)
     */
    public static final String ENV_JWKS_URL = "JWKS_URL";

    /**
     * Special value used for the {@value #ENV_JWKS_URL} environment variable to indicate that authentication is
     * disabled.
     */
    public static final String AUTH_DISABLED = "disabled";

    /**
     * Special value used for the {@value #ENV_JWKS_URL} environment variable to indicate that authentication is in
     * development mode.  When in this mode the provided bearer tokens are not JSON Web Tokens (JWTs) but instead are
     * simply base64 encoded email addresses.
     *
     * @deprecated Development authentication mode no longer supported
     */
    @Deprecated(forRemoval = false)
    public static final String AUTH_DEVELOPMENT = "development";

    /**
     * Prefix for special values to the {@value #ENV_JWKS_URL} environment variable to indicate that authentication is
     * using AWS ELB where the rest of the value will indicate the AWS region we are deployed in
     */
    public static final String AUTH_PREFIX_AWS = "aws:";

    /**
     * The default set of Authentication header names from which a JWT may be extracted
     */
    public static final String DEFAULT_AUTH_HEADER_NAMES =
            StringUtils.joinWith(",", JwtHttpConstants.HEADER_AUTHORIZATION, AwsConstants.HEADER_DATA);
    /**
     * The default set of Authentication header prefixes used in conjunction with the header names to control how JWTs
     * are extracted from headers
     *
     * @see #DEFAULT_AUTH_HEADER_NAMES
     */
    public static final String DEFAULT_AUTH_HEADER_PREFIXES =
            StringUtils.joinWith(",", JwtHttpConstants.AUTH_SCHEME_BEARER, "");

    /**
     * The default set of JWT claims that will be inspected to find the username
     */
    public static final String DEFAULT_USERNAME_CLAIMS = StringUtils.joinWith(",","email", "username");

}
