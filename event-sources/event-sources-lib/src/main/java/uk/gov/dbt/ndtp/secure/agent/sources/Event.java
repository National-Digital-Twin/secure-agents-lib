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
package uk.gov.dbt.ndtp.secure.agent.sources;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Represents an event from an {@link EventSource}
 *
 * @param <TKey>   Key Type
 * @param <TValue> Value Type
 */
public interface Event<TKey, TValue> {

    /**
     * Provides a stream of all headers defined for this event
     * <p>
     * Note that the same header may occur multiple times to express multiple values for a header.
     * </p>
     *
     * @return Stream of headers
     */
    Stream<Header> headers();

    /**
     * Provides a stream of header values for a specific header key
     *
     * @param key Header key
     * @return Header values
     */
    Stream<String> headers(String key);

    /**
     * Provides the last value specified for a specific header key
     *
     * @param key Header key
     * @return Last header value, or {@code null} if no such header exists
     */
    String lastHeader(String key);

    /**
     * Provides the key for this event
     *
     * @return Key
     */
    TKey key();

    /**
     * Provides the value for this event
     *
     * @return Value
     */
    TValue value();

    /**
     * Creates a new event replacing the original key with the new key
     *
     * @param newKey    New Key
     * @param <TNewKey> New key type
     * @return New event
     */
    <TNewKey> Event<TNewKey, TValue> replaceKey(TNewKey newKey);

    /**
     * Creates a new event replacing the original value with the new value
     *
     * @param newValue    New Value
     * @param <TNewValue> New value type
     * @return New event
     */
    <TNewValue> Event<TKey, TNewValue> replaceValue(TNewValue newValue);

    /**
     * Creates a new event replacing the original key and value with a new key and value
     *
     * @param newKey      New key
     * @param newValue    New value
     * @param <TNewKey>   New key type
     * @param <TNewValue> New value type
     * @return New event
     */
    <TNewKey, TNewValue> Event<TNewKey, TNewValue> replace(TNewKey newKey, TNewValue newValue);

    /**
     * Creates a new event replacing the existing headers with the given ones
     *
     * @param headers New headers
     * @return New event
     */
    Event<TKey, TValue> replaceHeaders(Stream<Header> headers);

    /**
     * Creates a new event appending the given headers to the existing headers
     *
     * @param headers Additional headers
     * @return New event
     */
    Event<TKey, TValue> addHeaders(Stream<Header> headers);

    /**
     * Provides a reference back to the {@link EventSource} that produced this event instance
     * <p>
     * This method <strong>intentionally</strong> returns an untyped reference to the original event source because the
     * events type signature may have been modified as it moved through the processing pipeline (e.g. by callers
     * utilising the {@link #replaceKey(Object)}, {@link #replaceValue(Object)} or {@link #replace(Object, Object)}
     * methods) so the types it now holds may not be the types that it was initially produced from its source with.
     * </p>
     * <p>
     * This method primarily exists so that callers can access the original {@link EventSource} and call it's
     * {@link EventSource#processed(Collection)} method once they are done processing some batch of events.
     * </p>
     *
     * @return Event Source, or {@code null} if not associated with a source
     */
    @SuppressWarnings("rawtypes")
    EventSource source();
}
