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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications.AbstractApplication;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.writers.ProblemPlainTextWriter;

/**
 * Represents a Problem
 * <p>
 * This POJO is an implementation of the Problem response described in
 * <a href="https://www.rfc-editor.org/rfc/rfc7807.html">RFC 7807</a>.
 * </p>
 */
@JsonPropertyOrder({ "type", "title", "status", "detail", "instance" })
public class Problem {

    /**
     * JAX-RS Media Type for Problem JSON responses per RFC 7807
     */
    public static final MediaType APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");

    /**
     * Media Type string for Problem JSON responses per RFC 7807
     */
    public static final String MEDIA_TYPE = "application/problem+json";


    private String type, title, instance, detail;
    private int status;

    /**
     * Creates a new empty problem
     */
    public Problem() {
    }

    /**
     * Creates a problem
     *
     * @param type     Type
     * @param title    Title
     * @param status   HTTP Status Code
     * @param detail   Problem details
     * @param instance Problem instance
     */
    public Problem(String type, String title, int status, String detail, String instance) {
        this.type = type;
        this.title = title;
        this.instance = instance;
        this.detail = detail;
        this.status = status;
    }

    /**
     * Converts the problem into an HTTP Response that can be returned by the server
     *
     * <p>
     * Note that as of {@code 0.25.0} we no longer set the
     * {@link Response.ResponseBuilder#type(String)} to {@value #MEDIA_TYPE} by default since we
     * added support for serializing problems into other media types, e.g. {@code text/plain}.  Setting it explicitly
     * overrode the JAX-RS server runtimes ability to set it based on the negotiated {@code Content-Type} for the
     * response.
     * </p>
     * <p>
     * Thus any endpoint that can return problem responses in this way <strong>MUST</strong> declare at least one of
     * {@value MediaType#TEXT_PLAIN}, {@value MediaType#APPLICATION_JSON} or
     * {@value #MEDIA_TYPE} in its {@link jakarta.ws.rs.Produces} annotation in order for the problem response to be
     * serializable.  If you need to support problem responses in other media types then you will need to implement and
     * register a suitable {@link jakarta.ws.rs.ext.MessageBodyWriter} in your application in the same way as we
     * register {@link ProblemPlainTextWriter} by default in our base
     * {@link AbstractApplication} class.
     * </p>
     *
     * @return HTTP Response
     * @deprecated Preferable to use {@link #toResponse(HttpHeaders)} to get improved Content Negotiation behaviour
     * wherever possible
     */
    @Deprecated
    public Response toResponse() {
        return toResponse(null);
    }

    /**
     * Converts the problem into an HTTP Response that can be returned by the server
     *
     * <p>
     * Note that as of {@code 0.25.0} we no longer set the
     * {@link Response.ResponseBuilder#type(String)} to {@value #MEDIA_TYPE} by default since we
     * added support for serializing problems into other media types, e.g. {@code text/plain}.  Setting it explicitly
     * overrode the JAX-RS server runtimes ability to set it based on the negotiated {@code Content-Type} for the
     * response.  However, if non-null {@link HttpHeaders} are provided then we make our best effort to set it
     * appropriately based on the negotiated acceptable media types.
     * </p>
     * <p>
     * Thus any endpoint that can return problem responses in this way <strong>MUST</strong> declare at least one of
     * {@value MediaType#TEXT_PLAIN}, {@value MediaType#APPLICATION_JSON} or
     * {@value #MEDIA_TYPE} in its {@link jakarta.ws.rs.Produces} annotation in order for the problem response to be
     * serializable.  If you need to support problem responses in other media types then you will need to implement and
     * register a suitable {@link jakarta.ws.rs.ext.MessageBodyWriter} in your application in the same way as we
     * register {@link ProblemPlainTextWriter} by default in our base
     * {@link AbstractApplication} class.
     * </p>
     *
     * @return HTTP Response
     * @since 0.25.1
     */
    public Response toResponse(HttpHeaders headers) {
        return Response.status(this.status).entity(this).type(selectContentType(headers)).build();
    }

    /**
     * Selects the most appropriate {@code Content-Type} header based on the HTTP Content Negotiation the JAX-RS
     * framework has already done
     *
     * @param headers HTTP Headers
     * @return Content Type, possibly {@code null} if it's been negotiated to some type we don't have built-in support
     * for
     */
    public String selectContentType(HttpHeaders headers) {
        if (headers == null) {
            return MEDIA_TYPE;
        }
        for (MediaType acceptable : headers.getAcceptableMediaTypes()) {
            if (acceptable.isWildcardType()) {
                return MEDIA_TYPE;
            } else if (acceptable.equals(APPLICATION_PROBLEM_JSON)) {
                return MEDIA_TYPE;
            } else if (acceptable.equals(MediaType.APPLICATION_JSON_TYPE)) {
                return MediaType.APPLICATION_JSON;
            } else if (acceptable.equals(MediaType.TEXT_PLAIN_TYPE)) {
                return MediaType.TEXT_PLAIN;
            }
        }
        return null;
    }

    /**
     * Gets the type of the problem
     *
     * @return Type
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the problem
     *
     * @param type Type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the title of the problem
     *
     * @return Title
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the problem
     *
     * @param title Title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the instance of the problem
     *
     * @return Instance
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getInstance() {
        return instance;
    }

    /**
     * Sets the instance of the problem
     *
     * @param instance Instance
     */
    public void setInstance(String instance) {
        this.instance = instance;
    }

    /**
     * Gets the detailed description of the problem
     *
     * @return Detailed description
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getDetail() {
        return detail;
    }

    /**
     * Sets the detailed description of the problem
     *
     * @param detail Detailed description
     */
    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * Gets the HTTP Status code for this problem
     *
     * @return HTTP Status Code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the HTTP Status Code for this problem
     *
     * @param status HTTP Status Code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Problem problem = (Problem) o;
        return status == problem.status && Objects.equals(type, problem.type) && Objects.equals(title,
                                                                                                problem.title) && Objects.equals(
                instance, problem.instance) && Objects.equals(detail, problem.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, instance, detail, status);
    }

    @Override
    public String toString() {
        return "Problem{" + "type='" + type + '\'' + ", title='" + title + '\'' + ", instance='" + instance + '\'' + ", detail='" + detail + '\'' + ", status=" + status + '}';
    }
}
