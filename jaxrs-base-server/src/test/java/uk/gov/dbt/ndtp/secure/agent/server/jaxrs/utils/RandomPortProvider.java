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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A provider of a sequence of port numbers starting from some base port with a random adjustment applied.
 * <p>
 * This is designed to reduce test failures that can happen when the OS is slow to release ports.
 * </p>
 */
public final class RandomPortProvider {

    private final AtomicInteger port;
    private static final Random RANDOM = new Random();

    /**
     * Creates a new random port provider
     *
     * @param basePort Base port to use as starting point for generating the sequence of port numbers
     */
    public RandomPortProvider(int basePort) {
        this.port = new AtomicInteger(basePort + RANDOM.nextInt(5, 50));
    }

    /**
     * Gets the current port number in use
     *
     * @return Current port number
     */
    public int getPort() {
        return this.port.get();
    }

    /**
     * Gets a new port number to use
     *
     * @return New port number
     */
    public int newPort() {
        return this.port.incrementAndGet();
    }
}
