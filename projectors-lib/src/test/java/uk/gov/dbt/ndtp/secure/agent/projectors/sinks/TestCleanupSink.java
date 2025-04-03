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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;

public class TestCleanupSink {

    @Test
    public void givenCleanupSinkWithNullCloseable_whenClosing_thenNothingRegisteredForCleanup() {
        // Given
        try (CleanupSink<String> sink = Sinks.<String>cleanup().resource(null).discard().build()) {
            // When
            sink.close();

            // Then
            Assert.assertEquals(sink.resourcesCount(), 0);
        }
    }

    @Test
    public void givenCleanupSinkWithNullResourcesList_whenClosing_thenNothingRegisteredForCleanup() {
        // Given
        try (CleanupSink<String> sink = Sinks.<String>cleanup().resources((List<Closeable>) null).discard().build()) {
            // When
            sink.close();

            // Then
            Assert.assertEquals(sink.resourcesCount(), 0);
        }
    }

    @Test
    public void givenCleanupSinkWithNullResourcesArray_whenClosing_thenNothingRegisteredForCleanup() {
        // Given
        try (CleanupSink<String> sink = Sinks.<String>cleanup().resources((Closeable[]) null).discard().build()) {
            // When
            sink.close();

            // Then
            Assert.assertEquals(sink.resourcesCount(), 0);
        }
    }

    @Test
    public void givenCleanupSinkWithListOfNullCloseables_whenClosing_thenNothingRegisteredForCleanup() {
        // Given
        List<Closeable> resources = new ArrayList<>();
        resources.add(null);
        resources.add(null);
        try (CleanupSink<String> sink = Sinks.<String>cleanup().resources(resources).discard().build()) {
            // When
            sink.close();

            // Then
            Assert.assertEquals(sink.resourcesCount(), 0);
        }
    }

    @Test
    public void givenCleanupSinkWithListOfNullCloseablesDirectly_whenClosing_thenNothingRegisteredForCleanup() {
        // Given
        List<Closeable> resources = new ArrayList<>();
        resources.add(null);
        resources.add(null);
        try (CleanupSink<String> sink = new CleanupSink<>(NullSink.of(), resources)) {
            // When
            sink.close();

            // Then
            Assert.assertEquals(sink.resourcesCount(), 0);
        }
    }

    @Test
    public void givenCleanupSinkWithCloseables_whenClosing_thenResourcesAreClosed() {
        // Given
        GoodCloseable resource1 = new GoodCloseable();
        GoodCloseable resource2 = new GoodCloseable();
        try (CleanupSink<String> sink = Sinks.<String>cleanup()
                                             .resource(resource1)
                                             .resource(resource2)
                                             .discard()
                                             .build()) {
            // When
            sink.close();

            // Then
            Assert.assertTrue(resource1.closed.get());
            Assert.assertTrue(resource2.closed.get());
        }
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Fails")
    public void givenCleanupSinkWithDestinationThatFailsOnClose_whenClosing_thenErrorIsThrown_andResourcesAreClosed() {
        // Given
        GoodCloseable resource = new GoodCloseable();
        try (CleanupSink<String> sink = Sinks.<String>cleanup()
                                             .resource(resource)
                                             .destination(new FailsOnCloseSink<>())
                                             .build()) {
            // When and Then
            sink.close();
            Assert.fail("Destination sink close exception should still be thrown");
        } finally {
            // And
            Assert.assertTrue(resource.closed.get());
        }
    }

    @Test
    public void givenCleanupSinkWithValidDestination_whenSendingItems_thenItemsAreDelivered_andOnClosingResourcesAreClosed() {
        // Given
        GoodCloseable resource = new GoodCloseable();
        try (CollectorSink<String> collector = Sinks.<String>collect().build()) {
            try (CleanupSink<String> sink = Sinks.<String>cleanup().resource(resource).destination(collector).build()) {
                // When
                sink.send("a");
                sink.send("b");

                // Then
                Assert.assertFalse(collector.get().isEmpty());
                Assert.assertEquals(collector.get().size(), 2);

                // And
                sink.close();
                Assert.assertTrue(resource.closed.get());
                Assert.assertTrue(collector.get().isEmpty());
            }
        }
    }

    @DataProvider(name = "closeables")
    public Object[][] closeables() {
        return new Object[][] {
                { List.of(new GoodCloseable(), new BadCloseable(), new GoodCloseable()) },
                { List.of(new GoodCloseable(), new BadCloseable()) },
                { List.of(new BadCloseable(), new GoodCloseable()) },
                };
    }

    @Test(dataProvider = "closeables")
    public void givenCleanupSinkWithMultipleCloseables_whenClosing_thenResourcesAreClosed(List<Closeable> resources) {
        // Given
        try (CleanupSink<String> sink = Sinks.<String>cleanup().resources(resources).discard().build()) {
            // When
            sink.close();

            // Then
            for (Closeable c : resources) {
                if (c instanceof GoodCloseable good) {
                    Assert.assertTrue(good.closed.get());
                }
            }
        }
    }

    @Test(dataProvider = "closeables")
    public void givenCleanupSinkWithMultipleCloseablesAsArray_whenClosing_thenResourcesAreClosed(
            List<Closeable> resources) {
        // Given
        try (CleanupSink<String> sink = Sinks.<String>cleanup()
                                             .resources(resources.toArray(new Closeable[0]))
                                             .discard()
                                             .build()) {
            // When
            sink.close();

            // Then
            for (Closeable c : resources) {
                if (c instanceof GoodCloseable good) {
                    Assert.assertTrue(good.closed.get());
                }
            }
        }
    }

    @Test
    public void givenMultipleCleanupSinksWithSameResource_whenClosing_thenResourceIsClosedMultipleTimes() {
        // Given
        CountingCloseable resource = new CountingCloseable();
        try (CleanupSink<String> sink = Sinks.<String>cleanup()
                                             .resource(resource)
                                             .cleanup(c -> c.resources(resource).discard())
                                             .build()) {
            // When
            sink.close();

            // Then
            Assert.assertEquals(resource.count.get(), 2);
        }
    }


    private final class GoodCloseable implements Closeable {

        public AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public void close() throws IOException {
            this.closed.set(true);
        }
    }

    private final class BadCloseable implements Closeable {

        @Override
        public void close() throws IOException {
            throw new RuntimeException("Close failure");
        }
    }

    private final class CountingCloseable implements Closeable {
        private final AtomicInteger count = new AtomicInteger(0);


        @Override
        public void close() throws IOException {
            this.count.incrementAndGet();
        }
    }

    private final class FailsOnCloseSink<T> implements Sink<T> {
        @Override
        public void send(T item) {
            // No-op
        }

        @Override
        public void close() {
            throw new RuntimeException("Fails");
        }
    }
}
