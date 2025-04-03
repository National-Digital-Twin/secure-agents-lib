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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verification.JwtVerifier;

/**
 * An insecure verifier that just treats tokens as being Base 64 encoded usernames and generates a fake JWS based on
 * that.  This means that this verifier is not verifying anything; it just assumes the input token is trusted.
 * <p>
 * <strong>DO NOT USE</strong> in production
 * </p>
 * @deprecated Removed, <strong>DO NOT USE</strong> even for tests
 */
@Deprecated(forRemoval = true)
public class InsecureDevelopmentVerifier implements JwtVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsecureDevelopmentVerifier.class);

    private final SecretKey key = Jwts.SIG.HS256.key().build();

    @Override
    public Jws<Claims> verify(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        String username = new String(Base64.getDecoder().decode(token));
        if (StringUtils.isBlank(username)) {
            return null;
        }

        LOGGER.trace("Raw token is {} which decoded to username {}", token, username);

        // Make a fake JWT and then reparse it back into a Jws<Claims> to fulfil the verifier contract
        String encoded = Jwts.builder()
                             .subject(username)
                             .claims()
                             .add(Map.of("email", username, "username", username))
                             .and()
                             .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                             .signWith(this.key)
                             .compact();
        return Jwts.parser().verifyWith(this.key).build().parseSignedClaims(encoded);
    }
}
