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

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ParamException;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;

/**
 * Maps parameter exceptions into RFC 7807 Problem responses
 */
@Provider
public class ParamExceptionMapper implements ExceptionMapper<ParamException> {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(ParamException exception) {
        //@formatter:off
        return new Problem("BadRequestParameter",
                           "Bad Parameter",
                           400,
                           String.format("%s Parameter '%s' received invalid value",
                                         exception.getParameterType()
                                                  .getSimpleName()
                                                  .replace("Param", ""),
                                         exception.getParameterName()),
                           null)
                .toResponse(this.headers);
        //@formatter:on
    }
}
