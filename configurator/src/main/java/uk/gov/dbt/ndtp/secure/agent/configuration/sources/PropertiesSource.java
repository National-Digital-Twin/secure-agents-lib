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

import java.util.Objects;
import java.util.Properties;

/**
 * A configuration source backed by a Java {@link Properties} object
 */
public class PropertiesSource implements ConfigurationSource {

    private final Properties properties;

    /**
     * Properties to use as the configuration source
     *
     * @param properties Properties
     */
    public PropertiesSource(Properties properties) {
        Objects.requireNonNull("Properties cannot be null");
        this.properties = properties;
    }

    @Override
    public String get(String key) {
        return getFromProperties(this.properties, key);
    }

    /**
     * Gets a configuration value from the given {@link Properties} object
     * <p>
     * This will convert the provided {@code key} into System property style key using
     * {@link ConfigurationSource#asSystemPropertyKey(String)} and use that as the primary search key.  If the value
     * does not exist with that key within the given properties then it falls back
     * </p>
     *
     * @param properties Properties
     * @param key        Key
     * @return Value (if available), otherwise {@code null}
     */
    static String getFromProperties(Properties properties, String key) {
        String variableKey = ConfigurationSource.asSystemPropertyKey(key);
        String value = properties.getProperty(variableKey);
        // May be stored as a non-String property in which case just convert toString()
        // Or may be stored with the key exactly, not the key as converted to a system property style key
        if (value == null && properties.containsKey(variableKey)) {
            value = properties.get(variableKey).toString();
        } else if (value == null && properties.containsKey(key)) {
            value = properties.get(key).toString();
        }
        return value;
    }
}
