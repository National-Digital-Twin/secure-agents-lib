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

import io.jsonwebtoken.security.Jwk;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;
import java.util.Objects;

@Path("/")
public class AwsElbResource {

    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

    @GET
    @Path("/{key}")
    @Produces({
            MediaType.APPLICATION_JSON
    })
    public Response getKeyById(@PathParam("key") @NotBlank String keyId) {
        if (JwksResource.JWKS == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Jwk<?> jwk = JwksResource.JWKS.getKeys().stream().filter(k -> Objects.equals(k.getId(), keyId)).findFirst().orElse(null);
        if (jwk == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        StringBuilder pem = new StringBuilder();
        pem.append(BEGIN_PUBLIC_KEY)
           .append('\n')
           .append(Base64.getEncoder().encodeToString(jwk.toKey().getEncoded()))
           .append('\n')
           .append(END_PUBLIC_KEY)
           .append('\n');
        return Response.ok().type(MediaType.TEXT_PLAIN_TYPE).entity(pem.toString()).build();
    }
}
