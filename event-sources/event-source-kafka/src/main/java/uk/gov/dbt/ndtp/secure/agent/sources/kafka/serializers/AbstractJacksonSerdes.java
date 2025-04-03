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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Objects;

/**
 * Abstract Jackson based serdes
 */
public class AbstractJacksonSerdes {

    /**
     * The Jackson mapper that will be used, derived implementations can customise this as desired in their own
     * constructors
     */
    protected final ObjectMapper mapper;

    /**
     * Creates a new serdes with the default object mapper
     */
    public AbstractJacksonSerdes() {
        this(new ObjectMapper());
    }

    /**
     * Creates a new serdes with a specified object mapper
     *
     * @param mapper Jackson mapper
     */
    public AbstractJacksonSerdes(ObjectMapper mapper) {
        Objects.requireNonNull(mapper, "Jackson Mapper cannot be null");
        this.mapper = mapper;
    }

    /**
     * Serializes a value to a byte array
     *
     * @param value Value
     * @param <T>   Value type
     * @return Byte array
     * @throws JsonProcessingException Thrown if serialization fails
     */
    protected final <T> byte[] serialize(T value) throws JsonProcessingException {
        return this.mapper.writeValueAsBytes(value);
    }

    /**
     * Deserializes a value from a byte array
     *
     * @param value Byte array
     * @param cls   Value type
     * @param <T>   Value type
     * @return Value
     * @throws IOException Thrown if deserialization fails
     */
    protected final <T> T deserialize(byte[] value, Class<T> cls) throws IOException {
        return this.mapper.readValue(value, cls);
    }
}
