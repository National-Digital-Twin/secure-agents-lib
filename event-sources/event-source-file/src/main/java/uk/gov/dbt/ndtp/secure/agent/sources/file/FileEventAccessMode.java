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
package uk.gov.dbt.ndtp.secure.agent.sources.file;

/**
 * Possible operation modes for file event IO
 */
public enum FileEventAccessMode {
    /**
     * Read-only
     */
    READ_ONLY(true, false),
    /**
     * Write only
     */
    WRITE_ONLY(false, true),
    /**
     * Read/write
     */
    READ_WRITE(true, true);

    private final boolean requiresSerializers, requiresDeserializers;

    /**
     * Creates a new access mode
     *
     * @param requiresDeserializers Whether deserializers are required for this mode
     * @param requiresSerializers   Whether serializers are required for this mode
     */
    FileEventAccessMode(boolean requiresDeserializers, boolean requiresSerializers) {
        this.requiresSerializers = requiresSerializers;
        this.requiresDeserializers = requiresDeserializers;
    }

    /**
     * Gets whether this access mode requires serializers to be configured
     *
     * @return True if serializers are required, false otherwise
     */
    public boolean requiresSerializers() {
        return this.requiresSerializers;
    }

    /**
     * Gets whether this access mode requires deserializers to be configured
     *
     * @return True if deserializers are required, false otherwise
     */
    public boolean requiresDeserializers() {
        return this.requiresDeserializers;
    }
}
