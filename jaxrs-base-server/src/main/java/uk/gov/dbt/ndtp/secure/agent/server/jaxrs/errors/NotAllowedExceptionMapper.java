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

import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;

/**
 * An exception mapper that handles {@link NotAllowedException}'s i.e. HTTP 406s
 */
@Provider
public class NotAllowedExceptionMapper implements ExceptionMapper<NotAllowedException> {

    @Context
    private UriInfo uri;

    @Context
    private HttpHeaders headers;

    @Context
    private Request request;

    @Override
    public Response toResponse(NotAllowedException exception) {
        //@formatter:off
        return new Problem("MethodNotAllowed",
                           null,
                           Response.Status.METHOD_NOT_ALLOWED.getStatusCode(),
                           String.format("/%s does not permit %s requests",
                                         this.uri.getPath(), this.request.getMethod()),
                           null)
                .toResponse(this.headers);
        //@formatter:on
    }
}
