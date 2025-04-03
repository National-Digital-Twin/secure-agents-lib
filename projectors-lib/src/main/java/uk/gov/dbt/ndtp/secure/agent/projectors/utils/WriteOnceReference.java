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
package uk.gov.dbt.ndtp.secure.agent.projectors.utils;

import java.util.function.Supplier;

/**
 * A reference wrapper that permits the reference value to be written only once
 *
 * @param <T> Reference type
 */
public class WriteOnceReference<T> {

    private T reference;

    /**
     * Creates an empty reference that is not currently set
     */
    public WriteOnceReference() {

    }

    /**
     * Creates a reference that is set to the given value, if that value is {@code null} then this is equivalent to
     * calling the no-args constructor
     *
     * @param value Value
     */
    public WriteOnceReference(T value) {
        this.reference = value;
    }

    /**
     * Gets the current value
     *
     * @return Value, or {@code null} if not set
     */
    public T get() {
        return this.reference;
    }

    /**
     * Gets the current value, computing it with the provided supplier function if it is not yet set
     *
     * @param supplier Supplier function
     * @return Current, or newly computed value
     */
    public T computeIfAbsent(Supplier<T> supplier) {
        if (this.reference == null) {
            this.reference = supplier.get();
        }
        return this.reference;
    }

    /**
     * Gets whether the reference is currently set
     *
     * @return True if set to a non-null value, false otherwise
     */
    public boolean isSet() {
        return this.reference != null;
    }

    /**
     * Sets the value
     *
     * @param value Value
     * @throws IllegalStateException Thrown if the reference value has already been set to a non-null value
     */
    public void set(T value) {
        if (this.reference != null) {
            throw new IllegalStateException("Reference has already been set");
        }
        this.reference = value;
    }
}
