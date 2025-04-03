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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;

/**
 * An extension of the {@link JacksonJsonSink} purely for test purposes, so we can examine the auto-configured output and
 * object mapper without having to expose that on the original class
 */
public class InspectableJacksonJsonSink extends JacksonJsonSink {

    /**
     * Creates a new sink with default options (standard out and compact printing)
     */
    public InspectableJacksonJsonSink() {
        super();
    }

    /**
     * Creates a new sink with custom options and default destination of {@link System#out}
     *
     * @param prettyPrint Whether to pretty print output
     */
    public InspectableJacksonJsonSink(boolean prettyPrint) {
        super(prettyPrint);
    }

    /**
     * Creates a new sink with custom options
     *
     * @param output      Destination output stream
     * @param prettyPrint Whether to pretty print output
     */
    public InspectableJacksonJsonSink(OutputStream output, boolean prettyPrint) {
        super(output, prettyPrint);
    }

    /**
     * Gets the configured output stream
     *
     * @return Output stream
     */
    public OutputStream getOutputStream() {
        return this.output;
    }

    /**
     * Gets the configured object mapper
     *
     * @return Object mapper
     */
    public ObjectMapper getObjectMapper() {
        return this.mapper;
    }
}
