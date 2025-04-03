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

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.MockData;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("data")
public class DataResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataResource.class);

    private static final Map<String, String> DATA = new ConcurrentHashMap<>();

    /**
     * Resets the data state
     */
    public static void reset() {
        DATA.clear();
    }

    @GET
    @Path("/{key}")
    @Produces({
            MediaType.APPLICATION_JSON
    })
    public Response getData(@Context HttpHeaders headers, @PathParam("key") @NotBlank String key) {
        if (DATA.containsKey(key)) {
            LOGGER.info("Retrieved key {} with value {}", key, DATA.get(key));
            return Response.status(Response.Status.OK).entity(new MockData(key, DATA.get(key))).build();
        } else {
            logNoSuchKey(key);
            return new Problem("KeyNotFound",
                               "Key Not Found",
                               Response.Status.NOT_FOUND.getStatusCode(),
                               String.format("Key %s was not found", key),
                               null).toResponse(headers);
        }
    }

    @POST
    @Path("/{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setData(@PathParam("key") @NotBlank String key, @QueryParam("value") @NotBlank String value) {
        DATA.put(key, value);
        LOGGER.info("Updated key {} to value {}", key, value);
        return Response.status(Response.Status.OK).entity(new MockData(key, value)).build();
    }

    @DELETE
    @Path("/{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteData(@Context HttpHeaders headers, @PathParam("key") @NotBlank String key, @QueryParam("value") String value) {
        if (!DATA.containsKey(key)) {
            logNoSuchKey(key);
            return new Problem("FailedPrecondition",
                               "Key Not Found",
                               Response.Status.PRECONDITION_FAILED.getStatusCode(),
                               String.format("Cannot delete key %s as it does not exist", key),
                               null).toResponse(headers);
        } else {
            String currentValue = DATA.get(key);
            if (StringUtils.isNotBlank(value) && !Objects.equals(value, currentValue)) {
                LOGGER.error("Key {} does not have value {}", key, value);
                return new Problem("FailedPrecondition",
                                   "Non-Matching value",
                                   Response.Status.PRECONDITION_FAILED.getStatusCode(),
                                   String.format(
                                           "Cannot delete key %s as its current value %s does not match the value to delete %s",
                                           key, currentValue, value),
                                   null).toResponse(headers);
            }

            DATA.remove(key);
            LOGGER.info("Deleted key {}", key);
            return Response.status(Response.Status.NO_CONTENT).build();
        }
    }

    private static void logNoSuchKey(String key) {
        LOGGER.error("Key {} does not exist", key);
    }

    @DELETE
    @Path("/actions/destroy")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response destroyData() {
        if (DATA.isEmpty()) {
            throw new ClientErrorException(Response.Status.CONFLICT);
        }
        DATA.clear();
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
