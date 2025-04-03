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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.SinkBuilder;

/**
 * A sink that writes its inputs out to a destination {@link OutputStream} as JSON
 * <p>
 * This is primarily intended for test and debugging where you want to see and inspect the JSON being produced, in real
 * pipelines the serialization to JSON is most likely handled directly by the sink that writes to a secure-agent.
 * </p>
 */
public class JacksonJsonSink<T> implements Sink<T> {
    /**
     * The Jackson Object Mapper that the sink will use
     */
    protected final ObjectMapper mapper;
    /**
     * The output stream that the sink will write to
     */
    protected final OutputStream output;

    /**
     * Creates a new sink with default options (standard out and compact printing)
     */
    JacksonJsonSink() {
        this(System.out, false);
    }

    /**
     * Creates a new sink with custom options and default destination of {@link System#out}
     *
     * @param prettyPrint Whether to pretty print output
     */
    JacksonJsonSink(boolean prettyPrint) {
        this(System.out, prettyPrint);
    }

    /**
     * Creates a new sink with custom options
     *
     * @param output      Destination output stream
     * @param prettyPrint Whether to pretty print output
     */
    JacksonJsonSink(OutputStream output, boolean prettyPrint) {
        this.mapper = new ObjectMapper();
        if (prettyPrint) {
            this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        this.mapper.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        Objects.requireNonNull(output, "Output cannot be null");
        this.output = output;
    }

    @Override
    public void send(T item) throws SinkException {
        try {
            this.mapper.writeValue(this.output, item);
        } catch (IOException e) {
            throw new SinkException("Failed to serialize item to JSON", e);
        }
    }

    @Override
    public void close() {
        // Nothing to do
    }

    /**
     * Creates a builder for a JSON sink
     *
     * @param <TItem> Item type
     * @return JSON Sink builder
     */
    public static <TItem> Builder<TItem> create() {
        return new Builder<>();
    }

    /**
     * A builder for JSON sinks
     *
     * @param <TItem> Item type
     */
    public static class Builder<TItem> implements SinkBuilder<TItem, JacksonJsonSink<TItem>> {

        private boolean prettyPrint = false;
        private OutputStream destination;

        /**
         * Enables pretty JSON printing
         *
         * @return Builder
         */
        public Builder<TItem> prettyPrint() {
            return prettyPrint(true);
        }

        /**
         * Configured JSON pretty printing
         *
         * @param enabled Whether pretty printing should be enabled
         * @return Builder
         */
        public Builder<TItem> prettyPrint(boolean enabled) {
            this.prettyPrint = enabled;
            return this;
        }

        /**
         * Configures that JSON output will go to standard output
         *
         * @return Builder
         */
        public Builder<TItem> toStdOut() {
            return toStream(System.out);
        }

        /**
         * Configures that JSON output will go to standard error
         *
         * @return Builder
         */
        public Builder<TItem> toStdErr() {
            return toStream(System.err);
        }

        /**
         * Configures that JSON output will go to some stream
         *
         * @param output Output stream
         * @return Builder
         */
        public Builder<TItem> toStream(OutputStream output) {
            this.destination = output;
            return this;
        }

        /**
         * Builders a new JSON sink
         *
         * @return JSON sink
         */
        @Override
        public JacksonJsonSink<TItem> build() {
            return new JacksonJsonSink<>(this.destination, this.prettyPrint);
        }
    }
}
