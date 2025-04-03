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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.ExternalParams;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Mode;

@Path("params")
public class ParamsResource {

    private static Mode MODE = Mode.A;

    @GET
    @Path("/mode")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getMode() {
        return Response.ok().entity(MODE).build();
    }

    @POST
    @Path("/mode")
    public Response setMode(@QueryParam("mode") @NotNull Mode mode) {
        MODE = mode;
        return Response.noContent().build();
    }

    @POST
    @Path("/everything/{path}")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    public Response everything(@PathParam("path") String path, @QueryParam("query") String query,
                               @HeaderParam("X-Custom-Header") String header, @CookieParam("cookie") String cookie,
                               @FormParam("form") String form) {
        return Response.noContent().build();
    }

    @POST
    @Path("/external/{path}")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    public Response external(@BeanParam @Valid ExternalParams params) {
        return Response.noContent().build();
    }
}
