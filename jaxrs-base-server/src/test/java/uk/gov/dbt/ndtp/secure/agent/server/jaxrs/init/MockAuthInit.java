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

import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.auth.JwtAuthEngineWithProblemChallenges;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtHttpConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.JwtServletConstants;
import uk.gov.dbt.ndtp.servlet.auth.jwt.sources.HeaderSource;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verification.SignedJwtVerifier;

public class MockAuthInit implements ServletContextListener {

    public static final SecretKey SIGNING_KEY = Jwts.SIG.HS256.key().build();
    public static final String X_CUSTOM_AUTH = "X-Custom-Auth";

    public static String createToken(String username) {
        return Jwts.builder()
                   .subject(username)
                   .expiration(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)))
                   .signWith(SIGNING_KEY)
                   .compact();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext()
           .setAttribute(JwtServletConstants.ATTRIBUTE_JWT_ENGINE, new JwtAuthEngineWithProblemChallenges(
                   List.of(new HeaderSource(JwtHttpConstants.HEADER_AUTHORIZATION, JwtHttpConstants.AUTH_SCHEME_BEARER),
                           new HeaderSource(X_CUSTOM_AUTH, null)), null));
        sce.getServletContext()
           .setAttribute(JwtServletConstants.ATTRIBUTE_JWT_VERIFIER,
                         new SignedJwtVerifier(Jwts.parser().verifyWith(SIGNING_KEY).build()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Intentionally Empty - Override
    }
}
