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
package uk.gov.dbt.ndtp.secure.agent.sources.memory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;

public class TestSimpleEvent {

    public static final String TEST_KEY = "Test";
    public static final String TEST_VALUE = "value";

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Both.*cannot be null")
    public void bad_event_01() {
        new SimpleEvent<String, String>(null, null, null);
    }

    @Test
    public void event_01() {
        SimpleEvent<String, String> event = new SimpleEvent<>(null, TEST_KEY, null);
        Assert.assertEquals(event.headers().count(), 0L);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertNull(event.value());
    }

    @Test
    public void event_02() {
        SimpleEvent<String, String> event = new SimpleEvent<>(null, null, TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 0L);
        Assert.assertNull(event.key());
        Assert.assertEquals(event.value(), TEST_VALUE);
    }

    @SuppressWarnings("AssertBetweenInconvertibleTypes")
    @Test
    public void event_equality() {
        SimpleEvent<String, String> event = new SimpleEvent<>(null, null, TEST_VALUE);
        Assert.assertEquals(event, event);
        Assert.assertNotEquals(event, null);
        Assert.assertNotEquals(null, event);
        Assert.assertFalse(event.equals(null));
        Assert.assertTrue(event.equals(event));
        Assert.assertFalse(event.equals("foo"));

        SimpleEvent<String, String> copy = new SimpleEvent<>(null, null, TEST_VALUE);
        Assert.assertEquals(event, copy);
        Assert.assertEquals(copy, event);
        Assert.assertTrue(event.equals(copy));
        Assert.assertTrue(copy.equals(event));

        SimpleEvent<String, String> differentValue = new SimpleEvent<>(null, null, "other");
        Assert.assertNotEquals(event, differentValue);
        Assert.assertNotEquals(differentValue, event);
        Assert.assertFalse(event.equals(differentValue));
        Assert.assertFalse(differentValue.equals(event));

        SimpleEvent<Integer, Double> differentTypes = new SimpleEvent<>(null, 123, 4.56);
        Assert.assertNotEquals(event, differentTypes);
        Assert.assertNotEquals(differentTypes, event);
        Assert.assertFalse(event.equals(differentTypes));
        Assert.assertFalse(differentTypes.equals(event));

        SimpleEvent<String, String> withHeaders =
                new SimpleEvent<>(Collections.singletonList(new Header("Content-Type", "text/plain")), null,
                                  TEST_VALUE);
        Assert.assertNotEquals(event, withHeaders);
        Assert.assertFalse(event.equals(withHeaders));
        Assert.assertFalse(withHeaders.equals(event));

        SimpleEvent<String, String> withOtherHeaders =
                new SimpleEvent<>(Arrays.asList(new Header("Content-Type", "text/plain"), new Header("foo", "bar")),
                                  null,
                                  TEST_VALUE);
        Assert.assertNotEquals(withHeaders, withOtherHeaders);
        Assert.assertFalse(withHeaders.equals(withOtherHeaders));
        Assert.assertFalse(withOtherHeaders.equals(withHeaders));

        SimpleEvent<String, String> withOtherHeadersDifferentOrder =
                new SimpleEvent<>(Arrays.asList(new Header("foo", "bar"), new Header("Content-Type", "text/plain")),
                                  null,
                                  TEST_VALUE);
        Assert.assertEquals(withOtherHeaders, withOtherHeadersDifferentOrder);
        Assert.assertTrue(withOtherHeadersDifferentOrder.equals(withOtherHeaders));
        Assert.assertTrue(withOtherHeaders.equals(withOtherHeadersDifferentOrder));
    }

    @Test
    public void event_headers_01() {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Content-Type", "text/plain"));
        headers.add(new Header("Generator", this.getClass().getCanonicalName()));
        headers.add(new Header("Generator", TEST_VALUE));
        SimpleEvent<String, String> event = new SimpleEvent<>(headers, TEST_KEY, TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 3L);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);

        Assert.assertEquals(event.headers("Content-Type").count(), 1L);
        Assert.assertEquals(event.headers("Content-Type").findFirst().orElse(null), "text/plain");
        List<String> generators = event.headers("Generator").toList();
        Assert.assertEquals(generators.size(), 2);
        Assert.assertEquals(generators.get(0), this.getClass().getCanonicalName());
        Assert.assertEquals(generators.get(1), TEST_VALUE);

        Assert.assertEquals(event.headers("foo").count(), 0L);
    }

    @Test
    public void event_headers_02() {
        Collection<Header> headers = new ArrayList<>();
        headers.add(new Header("Generator", this.getClass().getCanonicalName()));
        headers.add(new Header("Generator", TEST_VALUE));
        SimpleEvent<String, String> event = new SimpleEvent<>(headers, TEST_KEY, TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 2L);
        Assert.assertEquals(event.key(), TEST_KEY);
        Assert.assertEquals(event.value(), TEST_VALUE);

        List<String> generators = event.headers("Generator").toList();
        Assert.assertEquals(generators.size(), 2);
        Assert.assertEquals(generators.get(0), this.getClass().getCanonicalName());
        Assert.assertEquals(generators.get(1), TEST_VALUE);
        Assert.assertEquals(event.lastHeader("Generator"), TEST_VALUE);

        Assert.assertEquals(event.headers("foo").count(), 0L);
    }

    @Test
    public void event_headers_03() {
        SimpleEvent<String, String> event = new SimpleEvent<>(Collections.emptyList(), TEST_KEY, TEST_VALUE);
        Assert.assertEquals(event.headers().count(), 0L);
        Assert.assertEquals(event.headers("Generator").count(), 0L);
        Assert.assertNull(event.lastHeader("Generator"));
    }

    @SuppressWarnings("AssertBetweenInconvertibleTypes")
    @Test
    public void event_mutation_01() {
        SimpleEvent<String, String> event =
                new SimpleEvent<>(List.of(new Header("Content-Type", "text/plain")), TEST_KEY, TEST_VALUE);

        Event<Integer, String> mutatedKey = event.replaceKey(1234);
        Assert.assertEquals(mutatedKey.value(), event.value());
        Assert.assertNotEquals(mutatedKey.key(), event.key());
        Assert.assertEquals(mutatedKey.lastHeader("Content-Type"), "text/plain");

        Event<String, Boolean> mutatedValue = event.replaceValue(true);
        Assert.assertEquals(mutatedValue.key(), event.key());
        Assert.assertNotEquals(mutatedValue.value(), event.value());
        Assert.assertEquals(mutatedValue.lastHeader("Content-Type"), "text/plain");

        Event<Integer, Double> mutated = event.replace(5678, 1.23e4);
        Assert.assertNotEquals(mutated.key(), event.key());
        Assert.assertNotEquals(mutated.value(), event.value());
        Assert.assertEquals(mutated.lastHeader("Content-Type"), "text/plain");

        Event<Integer, Double> mutatedHeaders =
                mutated.addHeaders(Stream.of(new Header("Content-Type", "foo/bar")));
        Assert.assertEquals(mutatedHeaders.key(), mutated.key());
        Assert.assertEquals(mutatedHeaders.value(), mutated.value());
        Assert.assertNotEquals(mutatedHeaders, mutated);
        Assert.assertEquals(mutatedHeaders.lastHeader("Content-Type"), "foo/bar");
    }


    @Test
    public void event_mutation_pass_event_source() {
        TestEventSource source = new TestEventSource();

        SimpleEvent<String, String> event =
                new SimpleEvent<>(List.of(new Header("Content-Type", "text/plain")), TEST_KEY, TEST_VALUE, source);

        Event<Integer, String> mutatedKey = event.replaceKey(1234);
        Assert.assertEquals(mutatedKey.value(), event.value());
        Assert.assertNotEquals(mutatedKey.key(), event.key());
        Assert.assertEquals(mutatedKey.lastHeader("Content-Type"), "text/plain");
        Assert.assertEquals(mutatedKey.source(), source);

        Event<String, Boolean> mutatedValue = event.replaceValue(true);
        Assert.assertEquals(mutatedValue.key(), event.key());
        Assert.assertNotEquals(mutatedValue.value(), event.value());
        Assert.assertEquals(mutatedValue.lastHeader("Content-Type"), "text/plain");
        Assert.assertEquals(mutatedValue.source(), source);

        Event<Integer, Double> mutated = event.replace(5678, 1.23e4);
        Assert.assertNotEquals(mutated.key(), event.key());
        Assert.assertNotEquals(mutated.value(), event.value());
        Assert.assertEquals(mutated.lastHeader("Content-Type"), "text/plain");
        Assert.assertEquals(mutated.source(), source);

        Event<Integer, Double> mutatedHeaders =
                mutated.addHeaders(Stream.of(new Header("Content-Type", "foo/bar")));
        Assert.assertEquals(mutatedHeaders.key(), mutated.key());
        Assert.assertEquals(mutatedHeaders.value(), mutated.value());
        Assert.assertNotEquals(mutatedHeaders, mutated);
        Assert.assertEquals(mutatedHeaders.lastHeader("Content-Type"), "foo/bar");
        Assert.assertEquals(mutatedHeaders.source(), source);
    }

    public static class TestEventSource implements EventSource<String, String> {

        @Override
        public boolean availableImmediately() {
            return false;
        }

        @Override
        public boolean isExhausted() {
            return false;
        }

        @Override
        public void close() {
            // Test class - Empty on purpose

        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public Event<String, String> poll(Duration timeout) {
            return null;
        }

        @Override
        public Long remaining() {
            return 0L;
        }

        @Override
        public void processed(Collection<Event> processedEvents) {
            return;
        }
    }
}
