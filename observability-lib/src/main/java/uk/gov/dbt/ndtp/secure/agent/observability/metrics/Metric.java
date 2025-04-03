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
package uk.gov.dbt.ndtp.secure.agent.observability.metrics;

import java.util.Collections;
import java.util.Map;
import uk.gov.dbt.ndtp.secure.agent.observability.Chronological;

/**
 * The supertype of all metrics.
 */
public interface Metric extends Chronological {
    /**
     * The name of the metric.
     *
     * @return the metric name, which may not be null.
     */
    String getMetricName();

    /**
     * The metric value.
     *
     * @return the metric value, which may be null to indicate no value or 'datapoint' at this instant.
     */
    Number getValue();

    /**
     * Any labels associated with the metric.
     *
     * @return any label associated with the metric, which may be empty but never null.
     */
    default Map<String, Object> getLabels() {
        return Collections.emptyMap();
    }
}
