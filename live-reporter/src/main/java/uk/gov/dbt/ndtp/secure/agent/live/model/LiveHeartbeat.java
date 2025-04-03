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
package uk.gov.dbt.ndtp.secure.agent.live.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * A heartbeat used to report component status to IANode Live
 */
public class LiveHeartbeat {

    private String id, instanceId, name, componentType;
    private Date timestamp;
    private long reportingPeriod;
    private IODescriptor input, output;
    private LiveStatus status;

    /**
     * Creates an empty heartbeat
     */
    public LiveHeartbeat() {
    }

    /**
     * Creates a heartbeat
     *
     * @param id              ID
     * @param instanceId      Instance ID
     * @param name            Name
     * @param componentType   Component Type
     * @param timestamp       Timestamp
     * @param reportingPeriod Reporting period (in seconds)
     * @param input           Input descriptor
     * @param output          Output descriptor
     * @param status          Status
     */
    public LiveHeartbeat(String id, String instanceId, String name, String componentType, Date timestamp,
                         long reportingPeriod, IODescriptor input, IODescriptor output, LiveStatus status) {
        this.id = id;
        this.instanceId = instanceId;
        this.name = name;
        this.componentType = componentType;
        this.timestamp = timestamp;
        this.reportingPeriod = reportingPeriod;
        this.input = input;
        this.output = output;
        this.status = status;
    }

    /**
     * Creates a copy of the heartbeat
     *
     * @return New copy of the heartbeat
     */
    public LiveHeartbeat copy() {
        return new LiveHeartbeat(this.id, this.instanceId, this.name, this.componentType, this.timestamp,
                                 this.reportingPeriod, this.input, this.output, this.status);
    }

    /**
     * Gets the ID for the application.  This should be the same every time a given application runs.
     *
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID for the application.  This should be the same every time a given application runs.
     *
     * @param id ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the Instance ID for the application.  This <strong>MUST</strong> be different every time a given application
     * runs.
     *
     * @return Instance ID
     */
    @JsonProperty("instance_id")
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the Instance ID for the application.  This <strong>MUST</strong> be different every time a given application
     * runs.
     *
     * @param instanceId Instance ID
     */
    @JsonProperty("instance_id")
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Gets the human-readable name of the application
     *
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable name of the application
     *
     * @param name Name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the component type of the application i.e. what function does this application perform within the IANode
     * Platform
     *
     * @return Component Type
     */
    @JsonProperty("component_type")
    public String getComponentType() {
        return componentType;
    }

    /**
     * Sets the component type of the application i.e. what function does this application perform within the IANode
     * Platform
     *
     * @param componentType Component Type
     */
    @JsonProperty("component_type")
    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    /**
     * Gets the timestamp of the heartbeat
     *
     * @return Timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the heartbeat
     *
     * @param timestamp Timestamp
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the timestamp of the heartbeat to the current time
     */
    @JsonIgnore
    public void setTimestampToNow() {
        this.setTimestamp(Date.from(Instant.now()));
    }

    /**
     * Gets the reporting period (in seconds) for this application i.e. how often should consumers of the heartbeats
     * expect to receive a new heartbeat
     *
     * @return Reporting period (in seconds)
     */
    @JsonProperty("reporting_period")
    public long getReportingPeriod() {
        return reportingPeriod;
    }

    /**
     * Sets the reporting period (in seconds) for this application i.e. how often should consumers of the heartbeats
     * expect to receive a new heartbeat
     *
     * @param reportingPeriod Reporting period (in seconds)
     */
    @JsonProperty("reporting_period")
    public void setReportingPeriod(long reportingPeriod) {
        this.reportingPeriod = reportingPeriod;
    }

    /**
     * Gets the descriptor of this applications input
     *
     * @return Input descriptor
     */
    public IODescriptor getInput() {
        return input;
    }

    /**
     * Sets the descriptor of this applications input
     *
     * @param input Input descriptor
     */
    public void setInput(IODescriptor input) {
        this.input = input;
    }

    /**
     * Gets the descriptor of this applications output
     *
     * @return Output descriptor
     */
    public IODescriptor getOutput() {
        return output;
    }

    /**
     * Sets the descriptor of this applications output
     *
     * @param output Output descriptor
     */
    public void setOutput(IODescriptor output) {
        this.output = output;
    }

    /**
     * Gets the status of the application
     *
     * @return Status
     */
    public LiveStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the application
     *
     * @param status Status
     */
    public void setStatus(LiveStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveHeartbeat that = (LiveHeartbeat) o;
        //@formatter:off
        return reportingPeriod == that.reportingPeriod &&
               Objects.equals(id, that.id) &&
               Objects.equals(instanceId, that.instanceId) &&
               Objects.equals(name, that.name) &&
               Objects.equals(componentType, that.componentType) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(input, that.input) &&
               Objects.equals(output, that.output) &&
               status == that.status;
        //@formatter:on
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instanceId, name, componentType, timestamp, reportingPeriod, input, output, status);
    }
}
