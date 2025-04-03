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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * A filter that attaches a unique Request ID to each incoming request and includes that Request ID in the outgoing
 * response.  The Request ID is also placed into the Logging {@link MDC} so that logging configuration can choose to
 * include it in.
 * <p>
 * Note we intentionally set the {@link Priority} for this filter to {@code 0} such that it should be the very first
 * (and last) filter that gets applied to requests and responses.
 * </p>
 */
@Provider
@Priority(0)
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {
    /**
     * HTTP Header, and Logger {@link MDC} key, used to specify the Request ID associated with an HTTP Request
     */
    public static final String REQUEST_ID = "Request-ID";

    /**
     * The maximum possible length of a client supplied Request ID, a Request ID above this length will be truncated to
     * this maximum.
     */
    public static final int MAX_CLIENT_REQUEST_ID_LENGTH = UUID.randomUUID().toString().length();

    /**
     * Use a unique starting point each time the server starts up so even if Clients are providing custom Request IDs
     * the server will continue to append unique suffixes to them
     */
    private static final AtomicLong CLIENT_ID_SUFFIX = new AtomicLong(System.currentTimeMillis());

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Copy the assigned Request ID to the HTTP Response
        responseContext.getHeaders().putSingle(REQUEST_ID, requestContext.getHeaderString(REQUEST_ID));

        // Remove from MDC as the Request has finished processing
        MDC.remove(REQUEST_ID);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Did the client supply a custom Request ID?  If not generate a unique one
        String requestId = requestContext.getHeaderString(REQUEST_ID);
        if (StringUtils.isNotBlank(requestId)) {
            // If the Client provided their own Request ID we append a unique suffix each time.  This allows each
            // request to be uniquely identified while also allowing clients to use the same Request ID for a sequence
            // of related requests.  We impose a maximum length on the client provided ID to avoid them supplying
            // something ridiculous and effectively allowing them to DDoS the logging.
            if (requestId.length() > MAX_CLIENT_REQUEST_ID_LENGTH) {
                requestId = requestId.substring(0, MAX_CLIENT_REQUEST_ID_LENGTH);
            }
            requestId = requestId + "/" + CLIENT_ID_SUFFIX.incrementAndGet();
        } else {
            requestId = UUID.randomUUID().toString();
        }
        requestContext.getHeaders().putSingle(REQUEST_ID, requestId);

        // Place into the Logging MDC so logging patterns can include this if desired
        MDC.put(REQUEST_ID, requestId);
    }
}
