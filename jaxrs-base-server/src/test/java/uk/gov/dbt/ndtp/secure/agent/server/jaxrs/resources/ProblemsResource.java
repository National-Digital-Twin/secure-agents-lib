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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;

@Path("problems")
public class ProblemsResource {

    @GET
    @Produces({ MediaType.APPLICATION_JSON, Problem.MEDIA_TYPE, MediaType.TEXT_PLAIN , "application/custom"})
    public Response getProblem(@Context HttpHeaders headers,
                               @QueryParam("type") @DefaultValue("RuntimeException") String type,
                               @QueryParam("title") @DefaultValue("Unexpected Error") String title,
                               @QueryParam("status") @DefaultValue("500") int status,
                               @QueryParam("detail") @DefaultValue("") String detail) {
        return new Problem(type, title, status, detail, null).toResponse(headers);
    }

    @GET
    @Path("/throw")
    @Produces(MediaType.APPLICATION_JSON)
    public Response throwError(@QueryParam("message") @DefaultValue("Unexpected error") String message) {
        throw new RuntimeException(message);
    }


}
