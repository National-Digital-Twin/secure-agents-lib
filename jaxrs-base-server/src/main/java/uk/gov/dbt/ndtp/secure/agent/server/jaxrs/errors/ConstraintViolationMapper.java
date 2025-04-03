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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.validation.internal.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.Problem;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils.ParamInfo;

/**
 * Maps constraint violation errors into RFC 7807 Problem responses
 * <p>
 * Constraint violations occur when an API is called with parameter values that do not meet the constraints imposed by
 * Jakarta Bean Validation annotations.
 * </p>
 */
@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationMapper.class);

    @Context
    UriInfo uri;

    @Context
    HttpHeaders headers;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        StringBuilder detail = new StringBuilder();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            if (!detail.isEmpty()) {
                detail.append('\n');
            }

            ParamInfo info = ParamInfo.fromViolation(violation);
            LOGGER.debug("Mapped parameter violation at path '{}' to real parameter name '{}' with type '{}'",
                         violation.getPropertyPath(),
                         info.getName(), info.getType());
            detail.append('/').append(this.uri.getPath()).append(" received ");
            if (info.getType() != null) {
                detail.append(info.getType()).append(' ');
            }
            detail.append("Parameter '")
                  .append(info.getName())
                  .append("' with invalid value: ")
                  .append(violation.getMessage());
        }
        Response.Status status = ValidationHelper.getResponseStatus(exception);

        String problemType
                = exception.getConstraintViolations().isEmpty()
                  ? "InvalidRequestParameter"
                  : "InvalidRequestParameters";
        //@formatter:off
        return new Problem(problemType,
                           null,
                           status.getStatusCode(),
                           detail.toString(),
                           null)
                .toResponse(this.headers);
        //@formatter:on
    }
}
