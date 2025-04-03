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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.errors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;

/**
 * Maps otherwise unhandled errors into RFC 7807 Problem responses
 */
@Provider
public class FallbackExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackExceptionMapper.class);

    @Context
    private HttpHeaders headers;

    private String buildDetail(Throwable e) {
        StringBuilder builder = new StringBuilder();
        while (e != null) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            if (e.getMessage() != null) {
                builder.append(e.getMessage());
            }
            e = e.getCause();
        }

        return builder.toString();
    }

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webEx) {
            // If something represents a specific JAX-RS exception then reformat that into a Problem response
            //@formatter:off
            return new Problem(webEx.getClass().getSimpleName(),
                               null,
                               webEx.getResponse().getStatus(),
                               webEx.getMessage(),
                               webEx.getClass().getCanonicalName())
                    .toResponse(this.headers);
            //@formatter:on
        }

        // Explicitly log the error with its stack trace for diagnostic purposes
        LOGGER.error("Unhandled exception, see stack trace for more detail:", exception);

        // For any other error just translate into a 500 Internal Server Error
        //@formatter:off
        return new Problem("InternalServerError",
                           "Unexpected Error",
                           500,
                           buildDetail(exception),
                           exception.getClass().getCanonicalName())
                .toResponse(this.headers);
        //@formatter:on
    }
}
