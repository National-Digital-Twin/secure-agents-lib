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

/**
 * A configuration source backed by system properties.
 * <p>
 * This always reflects the current state of {@link System#getProperties()} so any changes to System Properties will be
 * immediately visible to this source.  If you prefer to capture only the current state of System Properties then you
 * should use a {@link PropertiesSource} instead taking a copy of {@link System#getProperties()}.
 */
public final class SystemPropertiesSource implements ConfigurationSource {

    /**
     * The singleton instance of the system properties source
     */
    public static final ConfigurationSource INSTANCE = new SystemPropertiesSource();

    /**
     * Private constructor to force use of the singleton instance {@link #INSTANCE}
     */
    private SystemPropertiesSource() {

    }

    @Override
    public String get(String key) {
        return PropertiesSource.getFromProperties(System.getProperties(), key);
    }
}
