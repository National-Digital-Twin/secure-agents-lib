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
package uk.gov.dbt.ndtp.secure.agent.projectors.driver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.jena.graph.Graph;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.NoOpProjector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Projector;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.InMemoryEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

public class TestProjectorDriver {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final List<Future<?>> futures = new ArrayList<>();

    private <TKey, TValue> Future<?> runDriver(ProjectorDriver<TKey, TValue, Event<TKey, TValue>> driver) {
        Future<?> future = executor.submit(driver);
        this.futures.add(future);
        return future;
    }

    @AfterMethod
    public void testCleanup() {
        int i = 0;
        while (i < this.futures.size()) {
            Future<?> future = this.futures.get(i);
            if (future.isDone() || future.isCancelled()) {
                // Remove any previously completed/cancelled futures
                this.futures.remove(i);
            } else {
                // Active future so cancel it now but keep it around
                future.cancel(true);
                i++;
            }
        }
    }

    @AfterClass
    public void cleanup() {
        this.executor.shutdownNow();
    }

    @Test
    public void projector_driver_01() throws ExecutionException, InterruptedException, TimeoutException {
        InfiniteEventSource source = new InfiniteEventSource("Event %,d", 0);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .limit(10_000)
                               .build();

        Future<?> future = this.runDriver(driver);
        future.get(5, TimeUnit.SECONDS);

        Assert.assertTrue(source.isClosed());
        Assert.assertEquals(source.eventsYielded(), 10_000);
    }

    @Test
    public void projector_driver_02() {
        InfiniteEventSource source = new InfiniteEventSource("Event %,d", 100);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               // Unlimited so we can test cancellation
                               .unlimited()
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        driver.cancel();
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_03() {
        InfiniteEventSource source = new InfiniteEventSource("Event %,d", 100);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               // Unlimited so we can test closing the source outside the drivers control
                               .unlimited()
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        source.close();
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_04() {
        // Testing driver encountering an exhausted sink
        InMemoryEventSource<Integer, String> source =
                new InMemoryEventSource<>(List.of(new SimpleEvent<>(Collections.emptyList(), 1, "Singleton event")));
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .unlimited()
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_05() {
        InfiniteEventSource source = new InfiniteEventSource("Event %,d", 5_000);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .unlimited()
                               .pollTimeout(Duration.ofSeconds(1))
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        driver.cancel();
        waitIgnoringErrors(future, 3, TimeUnit.SECONDS);
        Assert.assertEquals(source.eventsYielded(), 0);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_06() {
        InfiniteEventSource source = new InfiniteEventSource("Event %,d", 1_000);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .unlimited()
                               // Forcing stalls to test abort on consecutive stalls
                               .pollTimeout(Duration.ofMillis(100))
                               .maxStalls(3)
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        driver.cancel();
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertEquals(source.eventsYielded(), 0);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_07() {
        // Source reports its availability incorrectly which will upset the driver and cause it to abort
        InfiniteEventSource source = new LyingEventSource("Event %,d", 1_000);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .unlimited()
                               .pollTimeout(Duration.ofMillis(100))
                               .maxStalls(3)
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        driver.cancel();
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertEquals(source.eventsYielded(), 0);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_08() {
        // Source reports its remaining value in a random interval which should trigger the remaining related warnings
        InfiniteEventSource source = new RemainingInfiniteEventSource("Event %,d", 2);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .unlimited()
                               .unlimitedStalls()
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 10, TimeUnit.SECONDS);
        driver.cancel();
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertTrue(source.eventsYielded() > 0);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_08a() {
        // Source reports its remaining value in a random interval which should trigger the remaining related warnings
        InfiniteEventSource source = new RemainingInfiniteEventSource("Event %,d", 100);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .unlimited()
                               .unlimitedStalls()
                               .pollTimeout(Duration.ofMillis(10))
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 10, TimeUnit.SECONDS);
        driver.cancel();
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertEquals(source.eventsYielded(), 0);
        Assert.assertTrue(source.isClosed());
    }

    @Test
    public void projector_driver_09() {
        // Source reports its remaining value in a random interval which should trigger the remaining related warnings
        // Here we're explicitly setting our remaining events to a high number but setting yield and poll timeouts such
        // that those events are never read forcing a particular form of the warning to be tested
        InfiniteEventSource source = new RemainingInfiniteEventSource("Event %,d", 5_000, 1_000);
        Sink<Event<Integer, String>> sink = NullSink.of();
        ProjectorDriver<Integer, String, Event<Integer, String>> driver =
                ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                               .source(source)
                               .projector(new NoOpProjector<>())
                               .destination(sink)
                               .unlimited()
                               .unlimitedStalls()
                               .pollTimeout(Duration.ofMillis(10))
                               .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        driver.cancel();
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
        Assert.assertEquals(source.eventsYielded(), 0);
        Assert.assertTrue(source.isClosed());
    }

    private static void waitIgnoringErrors(Future<?> future, int timeout, TimeUnit timeUnit) {
        try {
            future.get(timeout, timeUnit);
        } catch (Throwable t) {
            // Ignore
        }
    }

    @Test
    public void projector_driver_10() {
        EventSource<Integer, Graph> source = new InMemoryEventSource<>(Collections.emptyList());
        Projector<Event<Integer, Graph>, Event<Integer, Graph>> projector = new NoOpProjector<>();

        ProjectorDriver<Integer, Graph, Event<Integer, Graph>> driver
                = ProjectorDriver.<Integer, Graph, Event<Integer, Graph>>create()
                                 .source(source)
                                 .projector(projector)
                                 .destination(NullSink::of)
                                 .pollTimeout(Duration.ofSeconds(5))
                                 .limit(10_000_000)
                                 .maxStalls(36)
                                 .reportBatchSize(100_000)
                                 .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 1, TimeUnit.SECONDS);
    }

    @Test
    public void projector_driver_11() throws InterruptedException {
        InfiniteEventSource source = new StallingInfiniteEventSource("Event %,d", 1_500, 3);

        ProjectorDriver<Integer, String, Event<Integer, String>> driver
                = ProjectorDriver.<Integer, String, Event<Integer, String>>create()
                                 .source(source)
                                 .projector(new NoOpProjector<>())
                                 .destination(NullSink::of)
                                 .pollTimeout(Duration.ofSeconds(1))
                                 .limit(10)
                                 .build();

        Future<?> future = this.runDriver(driver);
        waitIgnoringErrors(future, 5, TimeUnit.SECONDS);
        try {
            future.get();
        } catch (ExecutionException e) {
            Assert.fail("Unexpected driver error: " + e.getMessage());
        }
        Assert.assertTrue(future.isDone());
    }
}
