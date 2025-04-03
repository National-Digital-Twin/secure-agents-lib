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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.AbstractTransformingSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.AbstractForwardingSinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.IANodeHeaders;

/**
 * A forwarding sink that optionally adds one/more headers onto the event before forwarding it
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class EventHeaderSink<TKey, TValue> extends AbstractTransformingSink<Event<TKey, TValue>, Event<TKey, TValue>> {
    private final List<Function<Event<TKey, TValue>, Header>> headerGenerators = new ArrayList<>();

    /**
     * Creates a new sink
     *
     * @param destination      Destination Sink
     * @param headerGenerators Header generator functions
     */
    EventHeaderSink(Sink<Event<TKey, TValue>> destination,
                    Collection<Function<Event<TKey, TValue>, Header>> headerGenerators) {
        super(destination);
        this.headerGenerators.addAll(Objects.requireNonNull(headerGenerators, "Header Generators cannot be null"));
        if (this.headerGenerators.isEmpty()) {
            throw new IllegalArgumentException("Header Generators cannot be empty");
        }
    }

    @Override
    protected Event<TKey, TValue> transform(Event<TKey, TValue> event) {
        //@formatter:off
        List<Header> additionalHeaders =
                this.headerGenerators.stream()
                                     .map(g -> g.apply(event))
                                     .filter(Objects::nonNull)
                                     .toList();
        //@formatter:on

        // Either add the additional headers (if any) or return unmodified
        if (!additionalHeaders.isEmpty()) {
            return event.addHeaders(additionalHeaders.stream());
        }
        return event;
    }

    /**
     * Creates a builder for an event processed sink
     *
     * @param <TKey>   Event key type
     * @param <TValue> Event value type
     * @return Builder
     */
    public static <TKey, TValue> Builder<TKey, TValue> create() {
        return new Builder<>();
    }

    /**
     * Builder for event processed sinks
     *
     * @param <TKey>   Event Key type
     * @param <TValue> Event Value type
     */
    public static class Builder<TKey, TValue> extends
            AbstractForwardingSinkBuilder<Event<TKey, TValue>, Event<TKey, TValue>, EventHeaderSink<TKey, TValue>, Builder<TKey, TValue>> {

        private final List<Function<Event<TKey, TValue>, Header>> headerGenerators = new ArrayList<>();

        /**
         * Sets a header generator function that generates a header, may be called multiple times to have the sink add
         * multiple headers.
         * <p>
         * The generator function may inspect the input event to determine if, and how, to generate a header.  If a
         * generator does not wish to generate a header for a given input event then it <strong>MUST</strong> return
         * {@code null}.  Any {@code null} values returned by header generators are ignored.
         * </p>
         * <p>
         * If you always wish to generate the same fixed header, regardless of input event data, then call
         * {@link #fixedHeader(String, String)} instead.
         * </p>
         * <p>
         * Note that each header generator function <strong>MUST</strong> be independent of any other generator function
         * configured on the sink.  Each generator functions will be called independently with the input event exactly
         * as received by the {@link EventHeaderSink}.  The resulting set of new headers (if any, see earlier note about
         * returning {@code null} from generator functions) are then added to the event via
         * {@link Event#addHeaders(Stream)}.  This means that if a generator function used conditional logic to decide
         * when to apply a header it can only rely on the headers in the original event, not those added by other
         * generator functions.
         * </p>
         * <p>
         * If you wish to make generators dependent on other generators then you <strong>MUST</strong> create two
         * different instances of the {@link EventHeaderSink} chained together via the {@link #destination(Sink)}
         * method. However, if you find yourself doing this be aware that this is considered bad practise, and you
         * should consider carefully why you cannot generate the desired headers in independent functions instead.
         * </p>
         *
         * @param headerGenerator Header generator function, if {@code null} is produced by the generator then no header
         *                        will be added
         * @return Builder
         */
        public Builder<TKey, TValue> headerGenerator(Function<Event<TKey, TValue>, Header> headerGenerator) {
            if (headerGenerator != null) {
                this.headerGenerators.add(headerGenerator);
            }
            return this;
        }

        /**
         * Configures a number of IANode standard header generator functions that add headers like
         * {@value IANodeHeaders#REQUEST_ID}.
         * <p>
         * See {@link #addRequestId()}, {@link #addInputRequestId()} and {@link #addExecPath(String)} if you want to add
         * only a subset of headers.
         * </p>
         *
         * @param appName Application name for use in the {@value IANodeHeaders#EXEC_PATH} header
         * @return Builder
         */
        public Builder<TKey, TValue> addStandardHeaders(String appName) {
            //@formatter:off
            return this.addRequestId()
                       .addInputRequestId()
                       .addExecPath(appName);
            //@formatter:on
        }

        /**
         * Adds a header generator function that will add an {@value IANodeHeaders#EXEC_PATH} header to each event
         * providing that the supplied application name is not blank.
         *
         * @param appName Application name, if blank then no header will be added
         * @return Builder
         */
        public Builder<TKey, TValue> addExecPath(String appName) {
            // If an app name was provided then we'll add an Exec-Path headers
            if (StringUtils.isNotBlank(appName)) {
                return this.fixedHeader(IANodeHeaders.EXEC_PATH, appName);
            }
            return this;
        }

        /**
         * Adds a header generator function that will add an {@value IANodeHeaders#INPUT_REQUEST_ID} header to each
         * event that has a pre-existing {@value IANodeHeaders#REQUEST_ID} header.
         *
         * @return Builder
         */
        public Builder<TKey, TValue> addInputRequestId() {
            // Add Input-Request-ID header if, and only if, there's an existing Request-ID header
            return this.headerGenerator(e -> {
                String inputRequestId = e.lastHeader(IANodeHeaders.REQUEST_ID);
                if (StringUtils.isNotBlank(inputRequestId)) {
                    return new Header(IANodeHeaders.INPUT_REQUEST_ID, inputRequestId);
                }
                return null;
            });
        }

        /**
         * Adds a header generator function that will add a unique {@value IANodeHeaders#REQUEST_ID} header value to
         * each event.
         *
         * @return Builder
         */
        public Builder<TKey, TValue> addRequestId() {
            // Add Request-ID header
            return this.headerGenerator(e -> new Header(IANodeHeaders.REQUEST_ID, UUID.randomUUID().toString()));
        }

        /**
         * Configures a header generator function that adds a header with a fixed name and value to all events that is
         * not conditional on the input event.  If either the name/value are blank then this has no effect.
         * <p>
         * If you only want to add this header if it isn't already present then use
         * {@link #fixedHeaderIfMissing(String, String)} instead.
         * </p>
         * <p>
         * Use {@link #headerGenerator(Function)} if the generated header needs to have different values, or not exist
         * at all, depending on the input event.
         * </p>
         *
         * @param name  Header Name
         * @param value Header Value
         * @return Builder
         */
        public Builder<TKey, TValue> fixedHeader(String name, String value) {
            if (StringUtils.isNoneBlank(name, value)) {
                return this.headerGenerator(e -> new Header(name, value));
            } else {
                return this;
            }
        }

        /**
         * Configures a header generator function that adds a header with a fixed name and value only if no header with
         * that name exists on the input event.
         * <p>
         * If either the name/value is blank then this has no effect.
         * </p>
         * <p>
         * Use {@link #headerGenerator(Function)} if the generated header needs to have different values, or not exist
         * at all, depending on the input event.
         * </p>
         *
         * @param name  Header Name
         * @param value Header Value
         * @return Builder
         */
        public Builder<TKey, TValue> fixedHeaderIfMissing(String name, String value) {
            if (StringUtils.isNoneBlank(name, value)) {
                return this.headerGenerator(e -> e.headers(name).findAny().isEmpty() ?
                                                 new Header(name, value) : null);
            } else {
                return this;
            }
        }

        /**
         * Configures a number of IANode standard header generator functions for the
         * {@value IANodeHeaders#DATA_SOURCE_NAME} and {@value IANodeHeaders#DATA_SOURCE_TYPE} headers
         *
         * @param dataSourceName Data Source Name
         * @param dataSourceType Data Source Type
         * @return Builder
         */
        public Builder<TKey, TValue> addDataSourceHeaders(String dataSourceName, String dataSourceType) {
            if (StringUtils.isNotBlank(dataSourceName)) {
                fixedHeaderIfMissing(IANodeHeaders.DATA_SOURCE_NAME, dataSourceName);
            }
            if (StringUtils.isNotBlank(dataSourceType)) {
                fixedHeaderIfMissing(IANodeHeaders.DATA_SOURCE_TYPE, dataSourceType);
            }

            return this;
        }

        /**
         * Configures a header generator function that adds a {@value IANodeHeaders#CONTENT_TYPE} header to each event
         * with the given content type value
         *
         * @param contentType Content Type, if blank then no header is added
         * @return Builder
         */
        public Builder<TKey, TValue> addContentType(String contentType) {
            return this.fixedHeader(IANodeHeaders.CONTENT_TYPE, contentType);
        }

        @Override
        public EventHeaderSink<TKey, TValue> build() {
            return new EventHeaderSink<>(this.getDestination(), this.headerGenerators);
        }
    }
}
