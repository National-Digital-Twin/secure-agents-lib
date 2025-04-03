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

import java.util.Collections;
import java.util.List;

/**
 * Provides helper utilities related to result paging
 */
public class Paging {

    /**
     * Constant used to indicate that there is no result limit
     */
    public static final long UNLIMITED = -1;

    /**
     * Constant used to indicate the offset for the first result
     */
    public static final long FIRST_OFFSET = 1;

    private Paging() {
    }

    /**
     * Applies paging parameters to the given results
     *
     * @param limit   Limit, a value of -1 is interpreted as unlimited results
     * @param offset  Offset, a 1 based index for the result to start from
     * @param results Results to apply paging over
     * @param <T>     Result type
     * @return Paged results
     */
    public static <T> List<T> applyPaging(Long limit, Long offset, List<T> results) {
        if (limit < UNLIMITED) {
            limit = UNLIMITED;
        }

        if (limit == 0) {
            // A zero limit means no results
            return Collections.emptyList();
        } else if (offset > FIRST_OFFSET) {
            // If there's an offset apply that next
            if (offset > results.size()) {
                // If the offset is greater than the results size then no results
                return Collections.emptyList();
            } else if (limit == UNLIMITED) {
                // If an offset but unlimited results just return from the offset onwards
                return results.subList((int) (offset - 1), results.size());
            } else {
                // With a limit and offset return the portion from the offset to offset + limit
                // Remember that offset + limit could go beyond the total results in which case we need to just return
                // the results from the offset to the end of the results
                return results.subList((int) (offset - 1), (int) Math.min(offset + limit - 1, results.size()));
            }
        }

        if (limit != UNLIMITED && results.size() > limit) {
            // If we have a limit without an offset apply the limit now
            return results.subList(0, Math.toIntExact(limit));
        }

        // For unlimited results and an offset of 1 leave results unmodified
        return results;
    }
}
