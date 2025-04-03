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
package uk.gov.dbt.ndtp.secure.agent.live;

import java.util.Collections;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveError;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

/**
 * A reporter that can report {@link LiveError} instances which are used by IANode Live to present information about
 * the state of applications running on the IANode Platform.
 */
public class LiveErrorReporter {

    /**
     * Default Kafka topic for Live Error Reporting
     */
    public static final String DEFAULT_LIVE_TOPIC = "provenance.errors";

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveErrorReporter.class);

    private final Sink<Event<Bytes, LiveError>> destination;
    private final String applicationId;


    /**
     * Creates a new {@link LiveErrorReporterBuilder} for building a {@link LiveErrorReporter}
     *
     * @return Builder
     */
    public static LiveErrorReporterBuilder create() {
        return new LiveErrorReporterBuilder();
    }

    /**
     * Creates a new Live Error Reporter
     *
     * @param sink Destination sink to which reports should be sent
     */
    LiveErrorReporter(Sink<Event<Bytes, LiveError>> sink) {
        this(null, sink);
    }

    /**
     * Creates a new Live Error Reporter
     *
     * @param applicationId Application ID
     * @param sink          Destination sink to which reports should be sent
     */
    LiveErrorReporter(String applicationId, Sink<Event<Bytes, LiveError>> sink) {
        this.applicationId = applicationId;
        this.destination = sink != null ? sink : NullSink.of();
        if (sink == null) {
            LOGGER.warn("No sink specified, live errors are not being reported anywhere!");
        }
    }

    /**
     * Reports an error to IANode Live
     *
     * @param error Error
     */
    public void reportError(LiveError error) {
        Objects.requireNonNull(error, "Error to report cannot be null");

        // Inject various default values into the error if they aren't already suitably populated
        if (StringUtils.isBlank(error.getId())) {
            error.setId(this.applicationId);
        }
        if (error.getTimestamp() == null) {
            error.setTimestampToNow();
        }
        if (StringUtils.isBlank(error.getTraceback())) {
            StringBuilder trace = new StringBuilder();
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                trace.append(element).append('\n');
            }
            error.setTraceback(trace.toString());
        }
        if (StringUtils.isBlank(error.getType())) {
            error.setType("Throwable");
        }
        if (StringUtils.isBlank(error.getLevel())) {
            error.setLevel(Level.INFO.toString());
        }
        if (error.getCounter() == null) {
            error.setCounter(-1L);
        }

        try {
            this.destination.send(new SimpleEvent<>(Collections.emptyList(), null, error));
        } catch (SinkException e) {
            LOGGER.warn("Failed to report Live Error: {}", e.getMessage());
        }
    }

    /**
     * Closes the reporter which closes the underlying sink
     */
    public void close() {
        try {
            this.destination.close();
        } catch (SinkException e) {
            // This might happen naturally during shutdown when the exact ordering in which components shutdown isn't
            // guaranteed, we just log the message because we don't want to throw the error upwards as that might
            // interrupt other areas of otherwise orderly shutdown and/or conflate the cause of the shutdown with the
            // failing error handling.
            LOGGER.warn("Failed to close Live Error reporter: {}", e.getMessage());
        }
    }
}
