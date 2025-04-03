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
package uk.gov.dbt.ndtp.secure.agent.observability;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

/**
 * Utilities relating to presenting runtime information
 */
public class RuntimeInfo {

    private static final double KIBIBIT = 1024;
    private static final double MEBIBIT = KIBIBIT * KIBIBIT;
    private static final double GIBIBIT = MEBIBIT * KIBIBIT;
    private static final double TEBIBIT = GIBIBIT * KIBIBIT;

    /**
     * Private constructor prevents direct instantiation
     */
    private RuntimeInfo() {
    }

    /**
     * Prints the runtime information (memory, Java and OS) to the given logger
     *
     * @param logger Logger to output to
     */
    public static void printRuntimeInfo(Logger logger) {
        double rawMemory = Runtime.getRuntime().maxMemory();
        Pair<Double, String> memory = RuntimeInfo.parseMemory(rawMemory);
        logger.info("Processors: {}", Runtime.getRuntime().availableProcessors());
        logger.info("Memory:     {} {}", String.format("%.2f", memory.getKey()), memory.getValue());
        logger.info("Java:       {}", System.getProperty("java.version"));
        logger.info("OS:         {} {} {}", System.getProperty("os.name"), System.getProperty("os.version"),
                    System.getProperty("os.arch"));
    }

    /**
     * Prints library version information to the given logger.
     * <p>
     * This will only include libraries whose versions have previously been requested via
     * {@link LibraryVersion#get(String)}.
     * </p>
     *
     * @param logger Logger to output to
     */
    public static void printLibraryVersions(Logger logger) {
        for (String library : LibraryVersion.cachedLibraries()) {
            logger.info("Using library {} version {}", library, LibraryVersion.get(library));
        }
    }

    /**
     * Parses a raw memory value in bytes into a human-readable representation
     * <p>
     * The human-readable representation will be expressed in binary units, rather than decimal, units.  So for example
     * a parsed value of {@code 2 GiB} represents 2 Gibibytes, which is {@code 2147483648} bytes.
     * </p>
     * <p>
     * Due to the nature of floating point arithmetic some calculated values may be approximate.  This is especially
     * true if the output of this function is used in presenting a formatted value to the user where further truncation
     * of the value may occur.
     * </p>
     *
     * @param rawMemory Raw Memory
     * @return Human-readable representation consisting of a value and a unit
     */
    public static Pair<Double, String> parseMemory(double rawMemory) {
        if (rawMemory < MEBIBIT) {
            return Pair.of(rawMemory / KIBIBIT, "KiB");
        } else if (rawMemory < GIBIBIT) {
            return Pair.of(rawMemory / MEBIBIT, "MiB");
        } else if (rawMemory < TEBIBIT) {
            return Pair.of(rawMemory / GIBIBIT, "GiB");
        } else {
            return Pair.of(rawMemory / TEBIBIT, "TiB");
        }
    }
}
