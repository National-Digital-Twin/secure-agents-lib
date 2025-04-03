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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.init;

import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;

/**
 * A comparator that compares our {@link ServerConfigInit} implementations based upon their declared
 * {@link ServerConfigInit#priority()} values with higher priority values sorting first.
 */
public class InitPriorityComparator implements Comparator<ServerConfigInit> {

    /**
     * Singleton comparator instance for convenience
     */
    public static final InitPriorityComparator INSTANCE = new InitPriorityComparator();

    @Override
    public int compare(ServerConfigInit a, ServerConfigInit b) {
        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (b == null) {
            return 1;
        } else if (a == b) {
            return 0;
        }

        // Compare priorities, multiply by -1 to invert the comparison since we want higher priority values to sort
        // first
        int c = -1 * Integer.compare(a.priority(), b.priority());
        if (c == 0) {
            c = StringUtils.compare(a.getName(), b.getName());
        }
        return c;
    }
}
