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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka;

/**
 * Provides constants for exposed metrics for Kafka event sources
 */
public class KafkaMetricNames {
    /**
     * Metric for amount of time spent waiting for poll requests to Kafka to complete
     */
    public static final String POLL_TIMING = "messaging.kafka.poll_timing";

    /**
     * Metric for number of events fetched from Kafka on each poll request
     */
    public static final String FETCH_EVENTS_COUNT = "messaging.kafka.fetch_events_count";

    /**
     * Metric for current Kafka lag i.e. how far behind a consumer group is in processing a given topic
     */
    public static final String KAFKA_LAG = "messaging.kafka.lag";

    /**
     * Description for the Kafka poll timings metric
     */
    public static final String POLL_TIMING_DESCRIPTION =
            "Kafka events poll times i.e. how long we spent waiting for Kafka to return us new events";
    /**
     * Description for the Kafka fetch events count metric
     */
    public static final String FETCH_EVENTS_COUNT_DESCRIPTION =
            "Kafka event fetch counts i.e. how many events Kafka returned us on each poll.";
    /**
     * Description for the Kafka lag metric
     */
    public static final String LAG_DESCRIPTION =
            "Kafka Lag i.e. how far behind on reading a topic a given consumer group is.";
}
