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
package uk.gov.dbt.ndtp.secure.agent.sources.offsets.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.sources.offsets.MemoryOffsetStore;

/**
 * An abstract file backed offset store where offsets are serialised using Jackson
 */
public class AbstractJacksonOffsetStore extends MemoryOffsetStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJacksonOffsetStore.class);

    private static final TypeReference<Map<String, Object>> GENERIC_MAP_TYPE = new TypeReference<>() {
    };

    /**
     * The Jackson mapper that is used
     */
    protected final ObjectMapper mapper;
    /**
     * Offsets file that is in use
     */
    protected final File offsetsFile;

    /**
     * Creates a new file backed offset store that will use Jackson to serialise the offsets to the file
     *
     * @param mapper      Object mapper
     * @param offsetsFile Offsets file that will be used to persist the offsets
     */
    public AbstractJacksonOffsetStore(ObjectMapper mapper, File offsetsFile) {
        this.mapper = Objects.requireNonNull(mapper, "Jackson Mapper cannot be null");
        this.offsetsFile = Objects.requireNonNull(offsetsFile, "Offsets File cannot be null");

        // If the offsets file already exists read the existing offsets back in and cache in memory
        if (this.offsetsFile.exists() && this.offsetsFile.length() > 0) {
            try {
                Map<String, Object> storedOffsets = this.mapper.readValue(this.offsetsFile, GENERIC_MAP_TYPE);
                this.offsets.putAll(storedOffsets);
                LOGGER.debug("Offsets file {} contained {} offsets", this.offsetsFile.getAbsolutePath(),
                             storedOffsets.size());
            } catch (IOException e) {
                LOGGER.warn("Failed to read offsets from file {}: {}", this.offsetsFile.getName(), e.getMessage());
                throw new IllegalStateException(
                        String.format("Failed to read offsets from file %s", this.offsetsFile.getAbsolutePath()),
                        e);
            }
        } else {
            LOGGER.debug("Offsets file {} does not yet exist/is empty, no persistent offsets were loaded as a result",
                         this.offsetsFile.getAbsolutePath());
        }
    }

    @Override
    protected void flushInternal() {
        persistOffsetsToFile();
        super.flushInternal();
    }

    @Override
    protected void closeInternal() {
        persistOffsetsToFile();
        super.closeInternal();
    }

    /**
     * Persists the offsets to the underlying file
     */
    private void persistOffsetsToFile() {
        try {
            this.mapper.writeValue(this.offsetsFile, this.offsets);
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Failed to write offsets to file %s: %s", this.offsetsFile.getAbsolutePath(),
                                  e.getMessage()), e);
        }
    }
}
