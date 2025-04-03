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
package uk.gov.dbt.ndtp.secure.agent.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.ConfigurationSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.EnvironmentSource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides a centralised location with a lightweight abstraction for retrieving configuration that allows for plugging
 * in arbitrary configuration sources as needed.
 * <p>
 * The default setup reads configuration only from environment variables via {@link EnvironmentSource}, however
 * additional sources can be added via {@link #addSource(ConfigurationSource)}.  This is primarily intended to be useful
 * for testing where we want to inject arbitrary configuration for our tests, but should be flexible enough to allow
 * full blown configuration management solutions to be added in the future.
 * </p>
 */
public class Configurator {

    /**
     * Private constructor as this class is intended for use as a static singleton
     */
    private Configurator() {

    }

    private static final List<ConfigurationSource> SOURCES = new ArrayList<>();
    private static boolean USE_ALL_SOURCES = true;

    static {
        reset();
    }

    /**
     * Resets the configurator to its defaults (single {@link EnvironmentSource} and {@link #useAllSources()} is
     * {@code true})
     */
    public static void reset() {
        resetSources();
        USE_ALL_SOURCES = true;
    }

    /**
     * Resets the configuration sources to their defaults (single {@link EnvironmentSource})
     */
    public static void resetSources() {
        SOURCES.clear();
        SOURCES.add(EnvironmentSource.INSTANCE);
    }

    /**
     * Gets the collection of active configuration sources
     *
     * @return Active configuration sources
     */
    public static Collection<ConfigurationSource> activeSources() {
        if (USE_ALL_SOURCES) {
            return SOURCES;
        } else {
            return List.of(SOURCES.get(0));
        }
    }

    /**
     * Adds a new configuration source
     * <p>
     * Newly added sources always take precedence over any previously added sources and are queried in the order
     * configured.
     * </p>
     *
     * @param source Configuration source
     * @throws NullPointerException Thrown if the given source is null
     */
    public static void addSource(ConfigurationSource source) {
        Objects.requireNonNull(source);
        SOURCES.add(0, source);
    }

    /**
     * Sets a single configuration source to use overwriting any previously added sources
     *
     * @param source Configuration source
     * @throws NullPointerException Thrown if the given source is null
     */
    public static void setSingleSource(ConfigurationSource source) {
        Objects.requireNonNull(source);
        SOURCES.clear();
        addSource(source);
    }

    /**
     * Gets whether all configured sources are used
     *
     * @return True if all configured sources are used
     */
    public static boolean useAllSources() {
        return USE_ALL_SOURCES;
    }

    /**
     * Sets whether all configured sources are used or only the most recently configured.
     * <p>
     * This is primarily intended for test scenarios where you want to tightly control what configuration is picked up
     * by the code under test.
     * </p>
     *
     * @param useAll True if all sources are used, false if only the most recently configured should be used.
     */
    public static void setUseAllSources(boolean useAll) {
        USE_ALL_SOURCES = useAll;
    }

    /**
     * Gets the first value available from the configured sources
     *
     * @param key Configuration key
     * @return Value (if any)
     */
    public static String get(String key) {
        return get(new String[] { key });
    }

    /**
     * Gets the first value available from the configured sources
     *
     * @param key Configuration key
     * @return Value (if any)
     */
    public static String get(String[] key) {
        return get(key, null);
    }

    /**
     * Gets the first value available from the configured sources using the given configuration keys
     *
     * @param key           Configuration key(s)
     * @param fallbackValue Fallback value to use if no value is found for any of the given keys
     * @return Value (if any)
     */
    public static String get(String[] key, String fallbackValue) {
        return get(key, v -> v, fallbackValue);
    }

    /**
     * Gets the first value available from the configured sources using the given configuration key
     *
     * @param key           Configuration key
     * @param parser        Function that converts from the raw string value to the desired type
     * @param fallbackValue Fallback value to use if no value is found
     * @param <T>           Value type
     * @return Value (if any)
     */
    public static <T> T get(String key, Function<String, T> parser, T fallbackValue) {
        return get(new String[] { key }, parser, fallbackValue);
    }

    /**
     * Gets the first value available from the configured sources using the given configuration keys
     *
     * @param keys          Configuration key(s)
     * @param parser        Function that converts from the raw string value to the desired type
     * @param fallbackValue Fallback value to use if no value is found
     * @param <T>           Value type
     * @return Value (if any)
     */
  public static <T> T get(String[] keys, Function<String, T> parser, T fallbackValue) {
        if (ArrayUtils.isEmpty(keys)) {
            return fallbackValue;
        }

        for (ConfigurationSource source : activeSources()) {
            T value = getValueFromSource(keys, source, parser);
            if (value != null) {
                return value;
            }
        }

        return fallbackValue;
    }

    private static <T> T getValueFromSource(String[] keys, ConfigurationSource source, Function<String, T> parser) {
        for (String key : keys) {
            String value = source.get(key);
            if (StringUtils.isNotBlank(value)) {
                try {
                    T actualValue = parser.apply(value);
                    if (actualValue != null) {
                        return actualValue;
                    }
                } catch (Throwable e) {
                    // Ignored, just try the next key or fallback to default value
                }
            }
        }
        return null;
    }
}
