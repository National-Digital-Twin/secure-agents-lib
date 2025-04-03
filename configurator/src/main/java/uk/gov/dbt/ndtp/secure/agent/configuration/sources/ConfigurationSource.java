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
package uk.gov.dbt.ndtp.secure.agent.configuration.sources;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Interface for configuration sources that provide access to raw configuration values
 */
public interface ConfigurationSource {

    /**
     * Valid characters for system property variables
     * <p>
     * While more characters are potentially valid here limiting keys to this subset allows for easily converting
     * between environment and system property formats.
     * </p>
     */
    String SYSTEM_PROPERTY_CHARACTERS = "abcdefghijklmnopqrstuvwxyz.";
    /**
     * Valid characters for environment variables.
     * <p>
     * While more characters are potentially valid here limiting keys to this subset allows for easily converting
     * between environment and system property formats.
     * </p>
     */
    String ENVIRONMENT_VARIABLE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ_";

    /**
     * Provides the raw string value of the given configuration key if available
     *
     * @param key Key
     * @return Value, {@code null} if unavailable
     */
    String get(String key);

    /**
     * Given a key converts it into environment variable format i.e. all upper case with underscore used as a separator
     *
     * @param key Key
     * @return Environment variable format key
     */
    static String asEnvironmentVariableKey(String key) {
        if (StringUtils.containsOnly(key, ENVIRONMENT_VARIABLE_CHARACTERS)) {
            return key;
        }
        return key.toUpperCase(Locale.ROOT).replace(".", "_");
    }

    /**
     * Given a key converts it into system property variable format i.e. all lower case with period used as a separator
     *
     * @param key Key
     * @return System property format key
     */
    static String asSystemPropertyKey(String key) {
        if (StringUtils.containsOnly(key, SYSTEM_PROPERTY_CHARACTERS)) {
            return key;
        }
        return key.toLowerCase(Locale.ROOT).replace("_", ".");
    }
}
