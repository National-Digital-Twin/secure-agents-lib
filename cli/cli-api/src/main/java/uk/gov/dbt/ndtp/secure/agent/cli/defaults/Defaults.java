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
package uk.gov.dbt.ndtp.secure.agent.cli.defaults;

import java.util.function.Function;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;

/**
 * Utility class used to help instantiate command options with default values from the environment
 *
 * @deprecated Use {@link Configurator} instead as it allows injecting default
 * configuration, especially for tests, much more easily
 */
@Deprecated(forRemoval = true)
public class Defaults {

    private Defaults() {
    }

    /**
     * Gets a value from the environment to use as a default.
     * <p>
     * It is expected that calling code will use this to assign default values to command option fields that can be
     * overridden by users explicitly supplying those options.
     * </p>
     *
     * @param variable Environment variable to try
     * @return Value (if any)
     * @deprecated Use {@link Configurator#get(String)} instead
     */
    @Deprecated(forRemoval = true)
    public static String fromEnvironment(String variable) {
        return fromEnvironment(new String[] { variable });
    }

    /**
     * Gets a value from the environment to use as a default
     * <p>
     * It is expected that calling code will use this to assign default values to command option fields that can be
     * overridden by users explicitly supplying those options.
     * </p>
     *
     * @param variables Environment variables to try in order of preference
     * @return Value (if any)
     * @deprecated Use {@link Configurator#get(String[])} instead
     */
    @Deprecated(forRemoval = true)
    public static String fromEnvironment(String[] variables) {
        return fromEnvironment(variables, null);
    }

    /**
     * Gets a value from the environment to use as a default.
     * <p>
     * It is expected that calling code will use this to assign default values to command option fields that can be
     * overridden by users explicitly supplying those options.
     * </p>
     *
     * @param variables     Environment variables to try in order of preference
     * @param fallbackValue Fallback value to use if none of the environment variables are set
     * @return Value (if any)
     * @deprecated Use {@link Configurator#get(String[], String)} instead
     */
    @Deprecated(forRemoval = true)
    public static String fromEnvironment(String[] variables, String fallbackValue) {
        return fromEnvironment(variables, v -> v, fallbackValue);
    }

    /**
     * Gets a value from the environment to use as a default.
     * <p>
     * It is expected that calling code will use this to assign default values to command option fields that can be
     * overridden by users explicitly supplying those options.
     * </p>
     *
     * @param variable      Environment variables to read value from
     * @param parser        Function that converts from the string value of the environment variable to the desired
     *                      type
     * @param fallbackValue Fallback value to use if the environment variable is not set
     * @param <T>           Value type
     * @return Value (if any)
     * @deprecated Use {@link Configurator#get(String, Function, Object)} instead
     */
    @Deprecated(forRemoval = true)
    public static <T> T fromEnvironment(String variable, Function<String, T> parser, T fallbackValue) {
        return fromEnvironment(new String[] { variable }, parser, fallbackValue);
    }

    /**
     * Gets a value from the environment to use as a default.
     * <p>
     * It is expected that calling code will use this to assign default values to command option fields that can be
     * overridden by users explicitly supplying those options.
     * </p>
     *
     * @param variables     Environment variables to try in order of preference
     * @param parser        Function that converts from the string value of the environment variable to the desired
     *                      type
     * @param fallbackValue Fallback value if none of the environment variables are set
     * @param <T>           Value type
     * @return Value (if any)
     * @deprecated Use {@link Configurator#get(String[], Function, Object)}
     * instead
     */
    @Deprecated(forRemoval = true)
    public static <T> T fromEnvironment(String[] variables, Function<String, T> parser, T fallbackValue) {
        String value = null;
        if (ArrayUtils.isNotEmpty(variables)) {
            for (String var : variables) {
                value = System.getenv(var);
                if (StringUtils.isNotBlank(value)) {
                    try {
                        T actualValue = parser.apply(value);
                        if (actualValue != null) {
                            return actualValue;
                        }
                    } catch (Throwable e) {
                        // Ignored, just try the next environment variable or fallback to default value
                    }
                }
            }
        }

        return fallbackValue;
    }
}
