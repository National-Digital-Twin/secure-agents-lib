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

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.output.NullOutputStream;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;

public class TestSinkBuilders {

    @Test
    public void sink_builders_01() {
        CollectorSink<String> collector = Sinks.<String>collect().build();
        Assert.assertTrue(collector.get().isEmpty());

        collector.send("a");
        Assert.assertFalse(collector.get().isEmpty());
        Assert.assertEquals(collector.get().get(0), "a");

        collector.close();
        Assert.assertTrue(collector.get().isEmpty());
    }

    @Test
    public void sink_builders_02() {
        NullSink<String> sink = Sinks.<String>discard().build();
        Assert.assertEquals(sink.count(), 0);

        for (int i = 0; i < 100_000; i++) {
            sink.send("test");
        }

        Assert.assertEquals(sink.count(), 100_000);
        sink.close();
        Assert.assertEquals(sink.count(), 0);
    }

    @Test
    public void sink_builders_03() {
        FilterSink<Integer> sink = Sinks.<Integer>filter()
                                        .predicate(x -> x % 2 == 0)
                                        .destination(Sinks.collect())
                                        .build();

        Assert.assertTrue(sink.shouldForward(2));
        Assert.assertFalse(sink.shouldForward(1));

        sendNumbers(sink, 1_000);
    }

    private static void sendNumbers(Sink<Integer> sink, int size) {
        for (int i = 0; i < size; i++) {
            sink.send(i);
        }
    }

    @Test
    public void sink_builders_03b() {
        CollectorSink<Integer> collector = Sinks.<Integer>collect().build();
        FilterSink<Integer> sink = Sinks.<Integer>filter()
                                        .predicate(x -> x % 2 == 0)
                                        .destination(collector)
                                        .build();

        Assert.assertTrue(sink.shouldForward(2));
        Assert.assertFalse(sink.shouldForward(1));

        sendNumbers(sink, 1_000);
        Assert.assertEquals(collector.get().size(), 500);
    }

    @Test
    public void sink_builders_03c() {
        // No explicit destination defaults to NullSink
        FilterSink<Integer> sink = Sinks.<Integer>filter()
                                        .predicate(x -> x % 2 == 0)
                                        .build();

        Assert.assertTrue(sink.shouldForward(2));
        Assert.assertFalse(sink.shouldForward(1));

        sendNumbers(sink, 1_000);
    }

    @Test
    public void sink_builders_03d() {
        // No explicit destination defaults to NullSink
        FilterSink<Integer> sink = Sinks.<Integer>filter()
                                        .predicate(x -> x % 2 == 0)
                                        .metricsLabel("odd-numbers")
                                        .build();

        Assert.assertTrue(sink.shouldForward(2));
        Assert.assertFalse(sink.shouldForward(1));

        sendNumbers(sink, 1_000);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*already been set.*")
    public void sink_builders_03e() {
        // Setting multiple destinations is an error
        // Note that the way the API is designed actually makes this very hard to do by accident because the return type
        // from destination() is the generic SinkBuilder interface not the actual builder type so users have to try
        // really hard to do this, but we're testing it nonetheless
        // NB - Intentional redundant cast here, as of 0.12.0 the cast is no longer necessary but kept this variant
        //      of the test for historical reasons
        Sinks.<Integer>filter()
                                             .predicate(x -> x % 2 == 0)
                                             .destination(Sinks.collect())
                .destination(Sinks.collect())
                .build();
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*already been set.*")
    public void sink_builders_03f() {
        // Setting multiple destinations is an error
        // With the introduction of self-referential parameters for the base builder this is now easier to do
        Sinks.<Integer>filter()
             .predicate(x -> x % 2 == 0)
             .destination(Sinks.collect())
             .destination(Sinks.collect())
             .build();
    }

    @Test
    public void sink_builders_04() {
        try (SuppressDuplicatesSink<String> sink = Sinks.<String>suppressDuplicates()
                                                   .cacheSize(100)
                                                   .collect()
                                                   .build()) {
            for (int i = 0; i < 10_000; i++) {
                sink.send("test");
            }
        }
    }

    @Test
    public void sink_builders_05() {
        try (SuppressUnmodifiedSink<String, Character, String> sink =
                Sinks.<String, Character, String>suppressUnmodified()
                     .cacheSize(100)
                     .withMetrics("strings")
                     .keyFunction(item -> item.charAt(0))
                     .valueFunction(item -> item)
                     .comparator(Comparator.naturalOrder())
                     .discard()
                     .build()) {

            String[] animals = new String[] { "aardvark", "badger", "cat", "dog", "cat", "dog", "aardvark" };
            for (String animal : animals) {
                sink.send(animal);
            }
        }
    }

    @Test
    public void sink_builders_06() {
        try(JacksonJsonSink<String> sink = Sinks.<String>toJson()
                                            .toStdOut()
                                            .toStdErr()
                                            .toStream(NullOutputStream.INSTANCE)
                                            .prettyPrint()
                                            .build()) {
            sink.send("test");
        }
    }

    @Test
    public void sink_builders_07() {
        try (ThroughputSink<String> sink = Sinks.<String>throughput()
                                           .tracker(t -> t.logger(TestSinkBuilders.class)
                                                          .action("Testing")
                                                          .reportTimeUnit(
                                                                  TimeUnit.MILLISECONDS)
                                                          .reportBatchSize(100))
                                           .discard()
                                           .build()) {

            for (int i = 0; i < 10_000; i++) {
                sink.send("test");
            }
            Assert.assertEquals(sink.receivedCount(), 10_000);
        }
    }

    @Test
    public void sink_builders_08() {
        try(CollectorSink<String> collector = new CollectorSink<>()) {
            try (Sink<String> sink = Sinks.<String>throughput()
                                          .tracker(t -> t.reportBatchSize(1000)
                                                         .logger(TestSinkBuilders.class)
                                                         .inSeconds()
                                                         .itemsName("strings")
                                                         .action("Testing"))
                                          .suppressDuplicates(s -> s.cacheSize(10000)
                                                                    .withMetrics("duplicates")
                                                                    .filter(f -> f.predicate(x -> x.length() > 6)
                                                                                  .destination(collector))).build()) {

                sink.send("test");
                Assert.assertTrue(collector.get().isEmpty());

                sink.send("testing");
                Assert.assertFalse(collector.get().isEmpty());
                Assert.assertEquals(collector.get().get(0), "testing");

                for (int i = 0; i < 10_000; i++) {
                    sink.send("testing");
                }
                Assert.assertEquals(collector.get().size(), 1);
            }
        }
    }

    @Test
    public void sink_builders_09() {
        try (Sink<Object> sink =
                Sinks.throughput()
                     .tracker(t -> t.reportBatchSize(100).logger(TestSinkBuilders.class))
                     .filter(f -> f.predicate(x -> x.hashCode() % 2 == 0)
                                   .throughput(ts -> ts.tracker(t -> t.reportBatchSize(1000)
                                                                      .logger(TestSinkBuilders.class)
                                                                      .itemsName("filtered objects"))
                                                       .suppressDuplicates(s -> s.cacheSize(10)
                                                                                 .withMetrics("unique objects")
                                                                                 .suppressUnmodified(
                                                                                         u -> u.cacheSize(5)
                                                                                               .keyFunction(k -> k)
                                                                                               .valueFunction(
                                                                                                       v -> v)
                                                                                               .comparator(
                                                                                                       Comparator.comparingInt(
                                                                                                               Object::hashCode))
                                                                                               .discard()))))
                     .build()) {

            for (int i = 0; i < 1_000; i++) {
                sink.send(new Object());
            }
        }
    }

    @Test
    public void sink_builders_10() {
        try (Sink<Object> sink =
                Sinks.throughput()
                     .tracker(t -> t.reportBatchSize(100).logger(TestSinkBuilders.class))
                     .filter(f -> f.predicate(x -> x.hashCode() % 2 == 0)
                                   .collect())
                     .build()) {

            for (int i = 0; i < 1_000; i++) {
                sink.send(new Object());
            }
        }
    }

    @Test
    public void sink_builders_11() {
        try (Sink<String> sink =
                Sinks.<String>throughput()
                     .tracker(t -> t.reportBatchSize(100).logger(TestSinkBuilders.class))
                     .toJson(j -> j.prettyPrint().toStream(NullOutputStream.INSTANCE))
                     .build()) {

            for (int i = 0; i < 1_000; i++) {
                sink.send(Integer.toString(i));
            }
        }
    }

    @Test
    public void sink_builders_12() {
        try (CollectorSink<String> collector = CollectorSink.of()) {
            try (Sink<String> sink =
                         Sinks.<String>filter()
                              .predicate(p -> p.length() > 6)
                              .destination(new DelaySink<>(collector, 1000))
                              .build()) {
                sink.send("testing");
                Assert.assertEquals(collector.get().size(), 1);
                Assert.assertEquals(collector.get().get(0), "testing");
            }
        }
    }

    @Test
    public void sink_builders_13() {
        final AtomicInteger count = new AtomicInteger(0);
        try (Sink<Integer> sink = Sinks.<Integer>filter().destination(i -> count.incrementAndGet()).build()) {
            sendNumbers(sink, 1_000);
            Assert.assertEquals(count.get(), 1_000);

            count.set(0);
            sendNumbers(sink, 100_000);
            Assert.assertEquals(count.get(), 100_000);
        }
    }

    @Test
    public void sink_builders_14() {
        NullOutputStream output = NullOutputStream.INSTANCE;
        try (Sink<Integer> sink = Sinks.<Integer>filter().destination(output::write).build()) {
            sendNumbers(sink, 1_000);
        }
    }

    @Test(expectedExceptions = SinkException.class, expectedExceptionsMessageRegExp = "test")
    public void sink_builders_15() {
        try (Sink<Integer> sink = Sinks.<Integer>filter().destination(i -> {throw new SinkException("test");}).build()) {
            sink.send(0);
        }
    }
}
