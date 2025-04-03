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

import java.sql.Date;
import java.time.Instant;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.event.Level;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveError;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.CollectorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.ErrorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.IntermittentlyFaultySink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NonClearingCollectorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;

public class TestLiveErrorReporter {

    @Test(expectedExceptions = NullPointerException.class)
    public void givenLiveErrorReporter_whenReportingNullError_thenNullPointerException() {
        // Given
        Sink<Event<Bytes, LiveError>> collector = NonClearingCollectorSink.of();
        LiveErrorReporter reporter = new LiveErrorReporter(collector);

        // When and Then
        reporter.reportError(null);
    }

    @Test
    public void givenLiveErrorReporter_whenReportingEmptyError_thenSuccess() {
        // Given
        LiveErrorReporter reporter = new LiveErrorReporter(null);

        // When and Then
        reporter.reportError(LiveError.create().build());
    }

    @Test
    public void givenLiveErrorReporter_whenReportingErrors_thenCorrectErrorCount() {
        // Given
        NullSink<Event<Bytes, LiveError>> sink = NullSink.of();
        LiveErrorReporter reporter = new LiveErrorReporter(sink);

        // When
        reporter.reportError(LiveError.create().build());
        reporter.reportError(LiveError.create().build());

        // Then
        Assert.assertEquals(sink.count(), 2L);
    }

    @Test
    public void givenBadDestination_whenReportingErrors_thenErrorIsHandled() {
        // Given
        Sink<Event<Bytes, LiveError>> sink = new ErrorSink<>();
        LiveErrorReporter reporter = new LiveErrorReporter(sink);

        // When and Then
        reporter.reportError(LiveError.create().build());
    }

    @Test
    public void givenSometimesFaultyDestination_whenReportingErrors_thenSomeErrorsAreReported() {
        // Given
        try (CollectorSink<Event<Bytes, LiveError>> sink = CollectorSink.of()) {
            LiveErrorReporter reporter = new LiveErrorReporter(new IntermittentlyFaultySink<>(sink, 0.3));

            // When
            for (int i = 0; i < 100; i++) {
                reporter.reportError(LiveError.create().build());
            }

            // Then
            Assert.assertFalse(sink.get().isEmpty());
            Assert.assertTrue(sink.get().size() < 100);
        }
    }

    @Test
    public void givenLiveErrorReporter_whenReportingDetailedErrors_thenFieldsArePreservedAndPopulatedCorrectly() {
        // Given
        NonClearingCollectorSink<Event<Bytes, LiveError>> collector = new NonClearingCollectorSink<>();
        LiveErrorReporter reporter = new LiveErrorReporter("default-id", collector);

        // When
        // Check that setting an Application ID overrides the reporter live Application ID
        LiveError error = LiveError.create().id("custom-id").build();
        reporter.reportError(error);

        // Check setting specified fields are preserved
        java.util.Date testDate = Date.from(Instant.now().minusMillis(100));
        error = LiveError.create()
                         .message("Failed")
                         .level(Level.ERROR)
                         .timestamp(testDate)
                         .type("CustomError")
                         .recordCounter(12345L)
                         .build();
        reporter.reportError(error);

        // Then
        Assert.assertEquals(collector.get().size(), 2);
        LiveError reported = collector.get().get(0).value();
        Assert.assertNotNull(reported);
        Assert.assertEquals(reported.getId(), "custom-id");
        Assert.assertEquals(reported.getLevel(), Level.INFO.toString());
        Assert.assertEquals(reported.getType(), "Throwable");
        Assert.assertEquals(reported.getCounter(), -1L);
        Assert.assertNotNull(reported.getTimestamp());
        Assert.assertNotEquals(reported.getTimestamp(), testDate);
        Assert.assertNotNull(reported.getTraceback());

        reported = collector.get().get(1).value();
        Assert.assertNotNull(reported);
        Assert.assertEquals(reported.getId(), "default-id");
        Assert.assertEquals(reported.getType(), "CustomError");
        Assert.assertEquals(reported.getCounter(), 12345L);
        Assert.assertEquals(reported.getLevel(), Level.ERROR.toString());
        Assert.assertNotNull(reported.getTimestamp());
        Assert.assertEquals(reported.getTimestamp(), testDate);
        Assert.assertNotNull(reported.getTraceback());
    }

    @Test
    public void givenDestinationThatFailsOnClose_whenReportingErrors_thenClosesWithoutError() {
        // Given
        Sink<Event<Bytes, LiveError>> sink = new Sink<>() {
            @Override
            public void send(Event<Bytes, LiveError> item) {
                // Intentionally Empty - Override

            }

            @Override
            public void close() {
                throw new SinkException("fails close()");
            }
        };
        LiveErrorReporter reporter = new LiveErrorReporter(sink);

        // When
        reporter.reportError(LiveError.create().build());

        // Then
        reporter.close();
    }
}
