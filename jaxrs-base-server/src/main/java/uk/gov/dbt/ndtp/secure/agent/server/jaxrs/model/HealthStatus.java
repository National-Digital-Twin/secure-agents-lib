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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

/**
 * Represents health status for a server
 */
public class HealthStatus {

    private boolean healthy;
    private final List<String> reasons = new ArrayList<>();

    private final Map<String, Object> config = new HashMap<>();

    /**
     * Creates a blank health status
     * <p>
     * This constructor intended only for deserializers.
     * </p>
     */
    public HealthStatus() {

    }

    /**
     * Creates a new health status
     *
     * @param healthy Whether the server is healthy
     */
    public HealthStatus(boolean healthy) {
        this.healthy = healthy;
    }

    /**
     * Creates a new health status
     *
     * @param healthy       Whether the server is healthy
     * @param reasons       Reasons for the server health status
     * @param configuration Relevant server configuration to its health
     */
    @Builder
    public HealthStatus(boolean healthy, Collection<String> reasons, Map<String, Object> configuration) {
        this.healthy = healthy;
        if (reasons != null) {
            this.reasons.addAll(reasons);
        }
        if (configuration != null) {
            this.config.putAll(configuration);
        }
    }

    /**
     * Gets whether the server is healthy
     *
     * @return True if the server is healthy, false otherwise
     */
    @JsonProperty("healthy")
    public boolean isHealthy() {
        return this.healthy;
    }

    /**
     * Gets the reasons (if any) for the current server status
     *
     * @return Reasons
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("reasons")
    public List<String> reasons() {
        return this.reasons;
    }

    /**
     * Gets any server configuration to be exposed as part of the health status
     *
     * @return Configuration
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("config")
    public Map<String, Object> getConfig() {
        return config;
    }

    /**
     * Sets whether the server is healthy
     *
     * @param healthy Whether the server is healthy
     */
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    /**
     * Sets the reasons for the health status
     *
     * @param reasons Reasons
     */
    public void setReasons(List<String> reasons) {
        if (reasons != null) {
            this.reasons.addAll(reasons);
        }
    }

    /**
     * Sets the configuration that contributed to the health status
     *
     * @param config Configuration
     */
    public void setConfig(Map<String, Object> config) {
        if (config != null) {
            this.config.putAll(config);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthStatus that = (HealthStatus) o;
        return healthy == that.healthy &&
                Objects.equals(reasons, that.reasons) &&
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(healthy, reasons, config);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HealthStatus{");
        sb.append("healthy=").append(healthy);
        sb.append(", reasons=[");
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(reasons.get(i));
        }
        sb.append("], config={");
        boolean first = true;
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}}");
        return sb.toString();
    }
}
