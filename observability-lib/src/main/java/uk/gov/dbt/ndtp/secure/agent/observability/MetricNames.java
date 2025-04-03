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

/**
 * Constants related to metric names
 */
public class MetricNames {

    private MetricNames() {
    }

    /**
     * Metric for number of items received, or a portion therein
     */
    public static final String ITEMS_RECEIVED = "items.received";

    /**
     * Metric description for items received metric
     */
    public static final String ITEMS_RECEIVED_DESCRIPTION = "Total items received";

    /**
     * Metric for number of items processed, or a portion therein
     */
    public static final String ITEMS_PROCESSED = "items.processed";

    /**
     * Metric description for items processed description
     */
    public static final String ITEMS_PROCESSED_DESCRIPTION = "Total items processed";

    /**
     * Metric for the processing rate of items
     */
    public static final String ITEMS_PROCESSING_RATE = "items.processing_rate";

    /**
     * Metric description for items processing rate
     */
    public static final String ITEMS_PROCESSING_RATE_DESCRIPTION = "How fast items are being processed";

    /**
     * Metric for the number of items filtered out
     */
    public static final String ITEMS_FILTERED = "items.filtered";

    /**
     * Metric description for items filtered metric
     */
    public static final String ITEMS_FILTERED_DESCRIPTION =
            "Number of projected items that were ultimately filtered out by the projection";
    /**
     * Metric for the number of duplicate items suppressed
     */
    public static final String DUPLICATES_SUPPRESSED = "items.duplicates_suppressed";

    /**
     * Metric description for duplicates suppressed metric
     */
    public static final String DUPLICATES_SUPPRESSED_DESCRIPTION =
            "Number of duplicate items suppressed during projection";

    /**
     * Metric for the number of unmodified duplicate items suppressed
     */
    public static final String UNMODIFIED_SUPPRESSED = "items.unmodified_suppressed";

    /**
     * Metric description for unmodified suppressed metric
     */
    public static final String UNMODIFIED_SUPPRESSED_DESCRIPTION = "Number of unmodified duplicate items suppressed";
}
