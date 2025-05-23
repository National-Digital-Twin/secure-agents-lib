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

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides access to library versions with caching of the detected versions
 * <p>
 * Versions are detected by having libraries place a {@code /<library>.version} in the root of the classpath which is a
 * Java properties file containing version information where {@code <library>} is the name of the library in question
 * e.g. {@code my-cool-library}.
 * </p>
 * <p>
 * This properties file <strong>SHOULD</strong> contain a {@value #PROPERTY_VERSION} property containing the version for
 * the lib, other properties may also be present to provide additional version information <strong>BUT</strong> are not
 * required.  If the version cannot be detected then {@value #UNKNOWN} will be returned.
 * </p>
 */
public class LibraryVersion {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryVersion.class);

    /**
     * Default version string used if the actual version cannot be detected
     */
    public static final String UNKNOWN = "unknown";

    /**
     * Property in the version file used to store the library version
     */
    public static final String PROPERTY_VERSION = "version";

    private static final Map<String, Properties> PROPERTIES_CACHE = new HashMap<>();

    private static final Map<String, String> VERSION_CACHE = new HashMap<>();

    private LibraryVersion() {
    }

    /**
     * Gets the version properties for the given library, caching it for reuse
     * <p>
     * See {@link LibraryVersion} documentation for more detail about how this works.
     * </p>
     *
     * @param library Library
     * @return Version properties, will be an empty properties set if unavailable
     */
    public static Properties getProperties(String library) {
        synchronized (PROPERTIES_CACHE) {
            return PROPERTIES_CACHE.computeIfAbsent(library, l -> {
                try (InputStream input = LibraryVersion.class.getResourceAsStream("/" + l + ".version")) {
                    if (input == null) {
                        LOGGER.warn(
                                "Library " + library + " does not have a " + library + ".version file on the Classpath");
                        return new Properties();
                    }
                    Properties ps = new Properties();
                    ps.load(input);
                    return ps;
                } catch (Throwable e) {
                    LOGGER.warn("Failed to read version properties for library " + library);
                    return new Properties();
                }
            });
        }
    }

    /**
     * Gets the version for the given library, caching it for reuse.
     * <p>
     * See {@link LibraryVersion} documentation for more detail about how this works.
     * </p>
     *
     * @param library Library
     * @return Version, may be {@value #UNKNOWN} if it cannot be obtained
     */
    public static String get(String library) {
        synchronized (VERSION_CACHE) {
            return VERSION_CACHE.computeIfAbsent(library, l -> {
                Properties ps = getProperties(l);
                String version = (String) ps.get(PROPERTY_VERSION);
                return StringUtils.isNotBlank(version) ? version : UNKNOWN;
            });
        }
    }

    /**
     * Lists all the libraries whose version information has been loaded and cached
     *
     * @return Cached libraries
     */
    public static Set<String> cachedLibraries() {
        Set<String> libraries;
        synchronized (PROPERTIES_CACHE) {
            libraries = new HashSet<>(PROPERTIES_CACHE.keySet());
        }
        return Collections.unmodifiableSet(libraries);
    }

    /**
     * Resets any cached version information, only intended for use by tests
     */
    public static void resetCaches() {
        synchronized (PROPERTIES_CACHE) {
            synchronized (VERSION_CACHE) {
                VERSION_CACHE.clear();
            }
            PROPERTIES_CACHE.clear();
        }
    }
}
