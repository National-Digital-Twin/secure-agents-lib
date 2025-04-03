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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;

/**
 * An abstract JAX-RS resource that provides a {@code /healthz} endpoint for reporting server health
 */
@Path("/")
public abstract class AbstractHealthResource {

    private static final int CACHE_SIZE = 10;
    private static final Duration CACHE_DURATION = Duration.ofSeconds(30);
    /**
     * A cache used to hold the most recently computed Health Status of the application
     * <p>
     * Because multiple applications/servers may be hosted within a single JVM this cache, while a static singleton, has
     * a default size of {@value #CACHE_SIZE} to account for that.  Computed health statuses for different
     * applications/servers are stored in the cache against a key based upon the host domain, port and path of the
     * {@code /healthz} endpoint answering the request.
     * </p>
     */
    private static final Cache<String, HealthStatus> CACHED_STATUS =
            Caffeine.newBuilder()
                    .maximumSize(CACHE_SIZE)
                    .initialCapacity(CACHE_SIZE)
                    .expireAfterWrite(CACHE_DURATION)
                    .build();

    /**
     * Invalidates the cached health status, generally only needs to be called from tests
     */
    public static void invalidateCachedStatus() {
        CACHED_STATUS.invalidateAll();
    }

    /**
     * Error message returned when the derived resource fails to generate a Health Status
     */
    public static final String UNEXPECTED_ERROR_REASON =
            "Server encountered an unexpected error generating its health status";

    /**
     * Returns a health response
     * <p>
     * The underlying {@link HealthStatus}, as computed by the {@link #determineStatus(ServletContext)} method, will be
     * cached for up to 30 seconds so subsequent requests to this endpoint within that window will return the same
     * status.  This is so that repeated requests to the {@code /healthz} endpoint do not allow an indirect denial of
     * service attack against underlying services that the application may be probing to determine its health status.
     * </p>
     *
     * @param servletContext Servlet Context
     * @return Health response
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("healthz")
    public Response healthy(@Context ServletContext servletContext, @Context UriInfo uriInfo) {
        HealthStatus status;
        try {
            // Could potentially be multiple servers/applications running in the same JVM so differentiate their cached
            // status by the host, port and path at which it was reached
            String healthKeyCache =
                    String.format("%s:%d%s", uriInfo.getRequestUri().getHost(), uriInfo.getRequestUri().getPort(),
                                  uriInfo.getRequestUri().getPath());
            status = CACHED_STATUS.get(healthKeyCache, k -> this.determineStatus(servletContext));
        } catch (Throwable t) {
            // There is an issue we've seen where a derived resources determineStatus() implementation hits an error
            // that then results in a 500 Internal Server Error rather than an actual 503 Service Unavailable so catch
            // that possibility here and report it appropriately with a generic error message
            status = new HealthStatus(false, List.of(UNEXPECTED_ERROR_REASON, t.getMessage()), Collections.emptyMap());
        }
        return Response.status(status.isHealthy() ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE)
                       .entity(status)
                       .build();
    }

    /**
     * Determines the status for the server
     * <p>
     * Computing this may involve probing other services that the application depends upon to accurately report the
     * health status of the server.  However, the calling method {@link #healthy(ServletContext, UriInfo)} will cache
     * the computed response for a while (30 seconds) in order to avoid the {@code /healthz} endpoint enabling an
     * indirect Denial of Service attack on the underlying services.
     * </p>
     *
     * @param context Servlet Context
     * @return Health Status
     */
    protected abstract HealthStatus determineStatus(ServletContext context);
}
