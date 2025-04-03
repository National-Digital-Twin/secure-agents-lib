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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.utils.Bytes;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveHeartbeat;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveStatus;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.CollectorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.DelaySink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.FilterSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.IntermittentlyFaultySink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NonClearingCollectorSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events.EventValueSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;

public class TestLiveReporter {

    public static final int MINIMUM_DELTA = 2;
    private static final IODescriptor TEST_INPUT = new IODescriptor("input.txt", "file");
    private static final IODescriptor TEST_OUTPUT = new IODescriptor("raw", "topic");
    private static final String TEST_ID = "test-01";
    private static final String TEST_NAME = "Test 01";

    @DataProvider(name = "reporter")
    public Object[][] reporterTestParameters() {
        return new Object[][] {
                { 1_000, 100, LiveStatus.COMPLETED },
                { 1_000, 100, LiveStatus.TERMINATED },
                { 1_000, 100, LiveStatus.ERRORING },
                // Won't ever send anything other than Started and Completed as reporting period is higher than runtime
                { 1_000, 15_000, LiveStatus.COMPLETED },
                // Longer running test
                { 10_000, 1_000, LiveStatus.TERMINATED },
                // Very high frequency heartbeat
                { 1_000, 25, LiveStatus.COMPLETED }
        };
    }

    @Test(dataProvider = "reporter", invocationCount = 3)
    public void givenParameters_whenRunningLiveReporter_thenHeartbeatsAsExpected(int runtime, int reportingMilliseconds,
                                                                                 LiveStatus stopStatus) throws
            InterruptedException {
        // Given
        CollectorSink<Event<Bytes, LiveHeartbeat>> sink = new NonClearingCollectorSink<>();
        LiveReporter reporter =
                new LiveReporter(sink, Duration.ofMillis(reportingMilliseconds), TEST_ID, TEST_NAME, "mapper",
                                 TEST_INPUT, TEST_OUTPUT);

        // When
        runReporter(reporter, runtime, stopStatus);

        // Then
        List<Event<Bytes, LiveHeartbeat>> heartbeats = sink.get();
        Assert.assertFalse(heartbeats.isEmpty(), "Expected some heartbeats to have been sent");

        int actualSize = heartbeats.size();
        int expectedSize = (runtime / reportingMilliseconds) + 1;
        int difference = Math.abs(actualSize - expectedSize);
        // If runtime is greater than reporting period allow a delta of 25% of expected size due to the vagaries of
        // thread scheduling and locking overheads especially on CI/CD machines
        // Also allow for the started and completed heartbeats
        int allowedDelta = runtime > reportingMilliseconds ? ((expectedSize / 4) + MINIMUM_DELTA) : MINIMUM_DELTA;
        Assert.assertTrue(difference <= allowedDelta, String.format(
                "Expected to receive approximately %,d heartbeats but found %,d which was outside the permitted delta of %,d",
                expectedSize, actualSize, allowedDelta));

        verifyHeartbeat(heartbeats, 0, LiveStatus.STARTED);
        for (int i = 1; i < heartbeats.size() - 1; i++) {
            verifyHeartbeat(heartbeats, i, LiveStatus.RUNNING);
        }
        verifyHeartbeat(heartbeats, heartbeats.size() - 1, stopStatus);
    }

    private LiveHeartbeat verifyHeartbeat(List<Event<Bytes, LiveHeartbeat>> heartbeats, int index,
                                          LiveStatus expectedStatus) {
        LiveHeartbeat heartbeat = heartbeats.get(index).value();
        Assert.assertEquals(heartbeat.getStatus(), expectedStatus);
        return heartbeat;
    }

    private static void runReporter(LiveReporter reporter, int runtime, LiveStatus stopStatus) throws
            InterruptedException {
        reporter.start();
        Thread.sleep(runtime);
        reporter.stop(stopStatus);
    }

    @Test
    public void givenNullDestination_whenCreatingLiveReporting_thenWarningIssued() throws InterruptedException {
        // Given and When
        // A null value for sink issues a WARNING and causes all heartbeats to go to a NullSink
        LiveReporter reporter = new LiveReporter(null, Duration.ofMillis(100), TEST_ID, TEST_NAME, "mapper",
                                                 TEST_INPUT,
                                                 TEST_OUTPUT);

        // Then
        runReporter(reporter, 1000, LiveStatus.COMPLETED);
    }

    @Test
    public void givenAlwaysFaultyDestination_whenRunningLiveReporter_thenNoHeartbeatsIssued() throws
            InterruptedException {
        // Given
        CollectorSink<Event<Bytes, LiveHeartbeat>> sink = new NonClearingCollectorSink<>();
        LiveReporter reporter = LiveReporter.create()
                                            .id(TEST_ID)
                                            .name(TEST_NAME)
                                            .componentType("mapper")
                                            .input(TEST_INPUT)
                                            .output(TEST_OUTPUT)
                                            .reportingPeriod(Duration.ofMillis(100))
                                            .destination(new IntermittentlyFaultySink<>(sink, 1.0))
                                            .build();

        // When
        runReporter(reporter, 1000, LiveStatus.TERMINATED);

        // Then
        Assert.assertTrue(sink.get().isEmpty());
    }

    @Test
    public void givenSometimesFaultyDestination_whenRunningLiveReporter_thenSomeHeartbeatsIssued() throws
            InterruptedException {
        // Given
        CollectorSink<Event<Bytes, LiveHeartbeat>> sink = new NonClearingCollectorSink<>();
        LiveReporter reporter = LiveReporter.create()
                                            .id(TEST_ID)
                                            .name(TEST_NAME)
                                            .componentType("mapper")
                                            .input(TEST_INPUT)
                                            .output(TEST_OUTPUT)
                                            .reportingPeriod(Duration.ofMillis(100))
                                            // 30% faulty
                                            .destination(new IntermittentlyFaultySink<>(sink, 0.3))
                                            .build();

        // When
        runReporter(reporter, 1000, LiveStatus.TERMINATED);

        // Then
        int expectedSize = (1000 / 100) + 1;
        Assert.assertFalse(sink.get().isEmpty());
        Assert.assertTrue(sink.get().size() < expectedSize);
    }

    @Test
    public void givenDelaySink_whenRunningLiveReporter_thenUncleanShutdown() throws InterruptedException {
        // Given
        // A delay sink will cause the reporter to not finish in a timely manner
        LiveReporter reporter =
                new LiveReporter(new DelaySink<>(NullSink.of(), 5000), Duration.ofSeconds(15), TEST_ID, TEST_NAME,
                                 "mapper", TEST_INPUT, TEST_OUTPUT);

        // When and Then
        runReporter(reporter, 1000, LiveStatus.COMPLETED);
    }

    @Test
    public void givenLiveReporter_whenStartingMultipleTimes_thenSubsequentStartThrowsError() {
        // Given
        LiveReporter reporter = new LiveReporter(NullSink.of(), Duration.ofMillis(100), TEST_ID, TEST_NAME, "mapper",
                                                 TEST_INPUT,
                                                 TEST_OUTPUT);

        // When and Then
        reporter.start();
        Assert.assertThrows(IllegalStateException.class, reporter::start);
        reporter.stop(LiveStatus.COMPLETED);
    }

    @Test
    public void givenLiveReporter_whenRunningAndRestarting_thenDifferentInstanceIds() throws InterruptedException {
        // Given
        CollectorSink<Event<Bytes, LiveHeartbeat>> sink = new NonClearingCollectorSink<>();
        LiveReporter reporter = new LiveReporter(sink, Duration.ofMinutes(1), TEST_ID, TEST_NAME, "mapper",
                                                 TEST_INPUT,
                                                 TEST_OUTPUT);

        // When
        runReporter(reporter, 100, LiveStatus.COMPLETED);

        List<Event<Bytes, LiveHeartbeat>> firstRunHeartbeats = new ArrayList<>(sink.get());
        sink.get().clear();
        LiveHeartbeat firstStart = verifyHeartbeat(firstRunHeartbeats, 0, LiveStatus.STARTED);
        LiveHeartbeat firstStop = verifyHeartbeat(firstRunHeartbeats, 1, LiveStatus.COMPLETED);
        Assert.assertEquals(firstStart.getInstanceId(), firstStop.getInstanceId());

        // Run again should generate a new Instance ID
        runReporter(reporter, 100, LiveStatus.COMPLETED);
        List<Event<Bytes, LiveHeartbeat>> secondRunHeartbeats = new ArrayList<>(sink.get());
        sink.get().clear();
        LiveHeartbeat secondStart = verifyHeartbeat(secondRunHeartbeats, 0, LiveStatus.STARTED);
        LiveHeartbeat secondStop = verifyHeartbeat(secondRunHeartbeats, 1, LiveStatus.COMPLETED);
        Assert.assertEquals(secondStart.getInstanceId(), secondStop.getInstanceId());

        // Then
        Assert.assertNotEquals(firstStart.getInstanceId(), secondStop.getInstanceId());
        Assert.assertNotEquals(firstStop.getInstanceId(), secondStop.getInstanceId());
    }

    @Test
    public void givenLiveReporter_whenStoppedFromDifferentThread_thenStopsCleanly() {
        LiveReporter reporter = new LiveReporter(NullSink.of(), Duration.ofMinutes(1), TEST_ID, TEST_NAME, "mapper",
                                                 TEST_INPUT,
                                                 TEST_OUTPUT);

        reporter.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(() -> reporter.stop(LiveStatus.TERMINATED));
            future.get();
        } catch (Throwable e) {
            Assert.fail("Errored trying to stop reporter: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void givenJsonSink_whenLiveReporting_thenJsonIsOutput() throws InterruptedException {
        // Given
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Sink<Event<Bytes, LiveHeartbeat>> toJson =
                EventValueSink.<Bytes, LiveHeartbeat>create().toJson(j -> j.prettyPrint().toStream(output)).build();

        // When
        LiveReporter reporter = new LiveReporter(toJson, Duration.ofMinutes(1), TEST_ID, TEST_NAME, "mapper",
                                                 TEST_INPUT,
                                                 TEST_OUTPUT);
        runReporter(reporter, 100, LiveStatus.COMPLETED);

        // Then
        String jsonData = output.toString(StandardCharsets.UTF_8);
        Assert.assertFalse(StringUtils.isBlank(jsonData));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*id.*cannot be null")
    public void givenNoParameters_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create().build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*name.*cannot be null")
    public void givenNoName_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create().id("test").build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*component type.*cannot be null")
    public void givenNoComponentType_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create().id("test").name("Test").build();
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Reporting period cannot be null")
    public void givenNullReportingPeriod_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create().id("test").name("Test").componentType("mapper").reportingPeriod(null).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Reporting period.*<= 0")
    public void givenZeroReportingPeriod_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create().id("test").name("Test").componentType("mapper").reportingPeriod(Duration.ZERO).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Reporting period.*<= 0")
    public void givenNegativeReportingPeriod_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create()
                    .id("test")
                    .name("Test")
                    .componentType("mapper")
                    .reportingPeriod(Duration.ZERO.minusSeconds(10))
                    .build();
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Input descriptor cannot be null")
    public void givenNoInputDescriptor_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create().id("test").name("Test").componentType("mapper").build();
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Output descriptor cannot be null")
    public void givenNoOutputDescriptor_whenBuildingReporter_thenError() {
        // Given, When and Then
        LiveReporter.create().id("test").name("Test").componentType("mapper").input("input-topic", "topic").build();
    }

    @Test
    public void givenAllParameters_whenBuildingReporter_thenSuccess() {
        // Given and When
        LiveReporter reporter = LiveReporter.create()
                                            .id("test")
                                            .name("test")
                                            .componentType("mapper")
                                            .input("input", "topic")
                                            .output("output", "topic")
                                            .reportingPeriod(Duration.ofSeconds(30))
                                            .destination(NullSink.of())
                                            .destination(FilterSink.<Event<Bytes, LiveHeartbeat>>create()
                                                                   .destination(
                                                                           EventValueSink.<Bytes, LiveHeartbeat>create()
                                                                                         .collect()))
                                            .build();

        // Then
        Assert.assertNotNull(reporter);
    }
}
