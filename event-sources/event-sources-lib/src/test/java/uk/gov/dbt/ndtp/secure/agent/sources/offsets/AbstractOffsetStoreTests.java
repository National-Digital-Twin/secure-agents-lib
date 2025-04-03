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
package uk.gov.dbt.ndtp.secure.agent.sources.offsets;

import java.util.Map;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public abstract class AbstractOffsetStoreTests {


    /**
     * Creates an instance of the offset store implementation to test
     * <p>
     * <strong>NB:</strong> If the implementation under test returns {@code true} for its {@link #isPersistent()}
     * implementation then it <strong>MUST</strong> return the same persistent instance if this method is called
     * multiple times from a single test method.
     * </p>
     *
     * @return Offset store instance
     */
    protected abstract OffsetStore createOffsetStore();

    /**
     * Gets whether the implementations underlying storage is persistent, i.e. after a {@link OffsetStore#close()} call
     * can the offsets be retrieved from a subsequent new instance of the implementation.
     * <p>
     * See {@link #createOffsetStore()} documentation for more details.  Note that the expectation of this test harness
     * is that for implementations that return {@code true} here, calling {@link #createOffsetStore()} again within the
     * same test should produce an instance that is backed by the data stored by any previous instance created within
     * the current test.
     * </p>
     *
     * @return True if persistent, false otherwise
     */
    protected abstract boolean isPersistent();

    /**
     * Gets whether an implementations persistence is delayed, i.e. it only persists offsets when either
     * {@link OffsetStore#flush()} or {@link OffsetStore#close()} is called.
     *
     * @return True if persistence is delayed, false if persistence is immediate
     */
    protected abstract boolean isPersistenceDelayed();

    protected final void unsupportedOffsetType(OffsetStore store, Class<?> offsetType) {
        throw new SkipException(
                String.format("OffsetStore implementation %s does not support offset type %s so test cannot be run",
                              store.getClass().getCanonicalName(), offsetType.getCanonicalName()));
    }

    public static void verifyHasOffsets(OffsetStore store, String... keys) {
        for (String key : keys) {
            Assert.assertTrue(store.hasOffset(key));
        }
    }

    public static void verifyOffsetsNotPresent(OffsetStore store, String... keys) {
        for (String key : keys) {
            Assert.assertFalse(store.hasOffset(key));
            Assert.assertNull(store.loadOffset(key));
        }
    }

    public static <T> void verifyOffsets(OffsetStore store, Map<String, T> expectedOffsets) {
        for (Map.Entry<String, T> expected : expectedOffsets.entrySet()) {
            Assert.assertEquals(store.<T>loadOffset(expected.getKey()), expected.getValue());
        }
    }

    @Test
    public void givenNumericOffset_whenStoringOffsets_thenOffsetsCanBeRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);
        verifyOffsetsNotPresent(store, "test");

        // When
        store.saveOffset("test", 12345L);

        // Then
        verifyHasOffsets(store, "test");
        verifyOffsets(store, Map.of("test", 12345L));
    }

    @Test
    public void givenNumericOffset_whenStoringNullOffset_thenNullOffsetIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);

        // When
        store.saveOffset("test", null);

        // Then
        verifyHasOffsets(store, "test");
        Assert.assertNull(store.loadOffset("test"));
    }

    @Test(expectedExceptions = ClassCastException.class)
    @SuppressWarnings("unused")
    public void givenNumericOffset_whenRetrievingWithWrongType_thenErrorIsThrown() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);

        // When
        store.saveOffset("test", 12345L);

        // Then
        Map<String, Object> offset = store.loadOffset("test");
        Assert.fail("Should have thrown a ClassCastException");
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void givenNumericOffset_whenRetrievingWithCastableType_thenOffsetIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);

        // When
        store.saveOffset("test", 12345L);

        // Then
        Assert.assertEquals(store.loadOffset("test"), Long.toString(12345L));
    }

    @Test
    public void givenNumericOffset_whenStoringMultipleOffsets_thenOffsetsCanBeRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);
        verifyOffsetsNotPresent(store, "test");

        // When
        store.saveOffset("a", 1L);
        store.saveOffset("b", 2L);
        store.saveOffset("c", 3L);

        // Then
        verifyHasOffsets(store, "a", "b", "c");
        verifyOffsets(store, Map.of("a", 1L, "b", 2L, "c", 3L));
    }

    @Test
    public void givenNumericOffset_whenUpdatingKey_thenLatestValueIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);
        verifyOffsetsNotPresent(store, "test");

        // When and Then
        store.saveOffset("test", 100L);
        verifyOffsets(store, Map.of("test", 100L));
        store.saveOffset("test", 10_000L);
        verifyOffsets(store, Map.of("test", 10_000L));
        store.saveOffset("test", 10L);
        verifyOffsets(store, Map.of("test", 10L));
    }

    @Test
    public void givenNumericOffset_whenDeletingOffsets_thenOffsetsAreNotRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);
        verifyOffsetsNotPresent(store, "test");

        // When
        store.saveOffset("test", 12345L);
        verifyOffsets(store, Map.of("test", 12345L));
        store.deleteOffset("test");

        // Then
        verifyOffsetsNotPresent(store, "test");
    }

    @Test
    public void givenNumericOffset_whenNotPopulated_thenNothingIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);

        // When
        // Intentionally not populating the offset store

        // Then
        verifyOffsetsNotPresent(store, "test");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void givenOffsetStore_whenClosed_thenErrorIsThrownOnHas() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);

        // When
        store.close();

        // Then
        store.hasOffset("test");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void givenOffsetStore_whenClosed_thenErrorIsThrownOnSave() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        store.close();

        // Then
        store.saveOffset("test", 12345L);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void givenOffsetStore_whenClosed_thenErrorIsThrownOnLoad() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        store.close();

        // Then
        store.loadOffset("test");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void givenOffsetStore_whenClosed_thenErrorIsThrownOnDelete() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        store.close();

        // Then
        store.deleteOffset("test");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void givenOffsetStore_whenClosed_thenErrorIsThrownOnFlush() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        store.close();

        // Then
        store.flush();
    }

    @Test
    public void givenOffsetStore_whenClosedMultipleTimes_noErrorIsThrown() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        store.close();
        store.close();

        // Then - closing multiple times should be safe and not produce any errors
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenOffsetStore_whenNullKeyIsUsed_thenErrorIsThrown_01() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        // Then
        store.hasOffset(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenOffsetStore_whenNullKeyIsUsed_thenErrorIsThrown_02() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(String.class)) {
            unsupportedOffsetType(store, String.class);
        }

        // When
        // Then
        store.saveOffset(null, "test");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenOffsetStore_whenNullKeyIsUsed_thenErrorIsThrown_03() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        // Then
        store.loadOffset(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenOffsetStore_whenNullKeyIsUsed_thenErrorIsThrown_04() {
        // Given
        OffsetStore store = createOffsetStore();

        // When
        // Then
        store.deleteOffset(null);
    }

    @Test
    public void givenStringOffset_whenStoringOffsets_thenOffsetsCanBeRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(String.class)) {
            unsupportedOffsetType(store, String.class);
        }

        // When
        store.saveOffset("test", "file1.txt");

        // Then
        verifyHasOffsets(store, "test");
        verifyOffsets(store, Map.of("test", "file1.txt"));
    }

    @Test
    public void givenStringOffset_whenStoringNullOffset_thenNullOffsetIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(String.class)) {
            unsupportedOffsetType(store, String.class);
        }

        // When
        store.saveOffset("test", null);

        // Then
        verifyHasOffsets(store, "test");
        Assert.assertNull(store.loadOffset("test"));
    }

    @Test
    public void givenStringOffset_whenStoringMultipleOffsets_thenOffsetsCanBeRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(String.class)) {
            unsupportedOffsetType(store, String.class);
        }

        // When
        store.saveOffset("a", "file1.txt");
        store.saveOffset("b", "file2.txt");
        store.saveOffset("c", "file3.txt");

        // Then
        verifyHasOffsets(store, "a", "b", "c");
        verifyOffsets(store, Map.of("a", "file1.txt", "b", "file2.txt", "c", "file3.txt"));
    }

    @Test
    public void givenStringOffset_whenNotPopulated_thenNothingIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(String.class)) {
            unsupportedOffsetType(store, String.class);
        }

        // When
        // Intentionally not populating the offset store

        // Then
        verifyOffsetsNotPresent(store, "test");
    }

    @Test
    public void givenStringOffset_whenUpdatingKey_thenLatestValueIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(String.class)) {
            unsupportedOffsetType(store, String.class);
        }
        verifyOffsetsNotPresent(store, "test");

        // When and Then
        store.saveOffset("test", "a");
        verifyOffsets(store, Map.of("test", "a"));
        store.saveOffset("test", "z");
        verifyOffsets(store, Map.of("test", "z"));
        store.saveOffset("test", "g");
        verifyOffsets(store, Map.of("test", "g"));
    }

    @Test
    public void givenComplexOffset_whenStoringOffsets_thenOffsetsCanBeRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(ComplexOffset.class)) {
            unsupportedOffsetType(store, ComplexOffset.class);
        }
        ComplexOffset test = new ComplexOffset("test", 7, 369);
        verifyOffsetsNotPresent(store, "test");

        // When
        store.saveOffset("test", test);

        // Then
        verifyHasOffsets(store, "test");
        verifyOffsets(store, Map.of("test", test));
    }

    @Test
    public void givenComplexOffset_whenStoringNullOffset_thenNullOffsetIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(ComplexOffset.class)) {
            unsupportedOffsetType(store, ComplexOffset.class);
        }

        // When
        store.saveOffset("test", (ComplexOffset) null);

        // Then
        verifyHasOffsets(store, "test");
        Assert.assertNull(store.loadOffset("test"));
    }

    @Test(expectedExceptions = ClassCastException.class)
    @SuppressWarnings("unused")
    public void givenComplexOffset_whenRetrievingWithWrongType_thenErrorIsThrown() {
        // Given
        OffsetStore store = createOffsetStore();
        if (!store.supportsOffsetType(ComplexOffset.class)) {
            unsupportedOffsetType(store, ComplexOffset.class);
        }

        // When
        ComplexOffset test = new ComplexOffset("test", 4, 33333);
        store.saveOffset("test", test);

        // Then
        Long offset = store.loadOffset("test");
        Assert.fail("Should have thrown a ClassCastException");
    }

    @Test
    public void givenComplexOffset_whenStoringMultipleOffsets_thenOffsetsCanBeRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);
        verifyOffsetsNotPresent(store, "test");

        // When
        ComplexOffset a = new ComplexOffset("a", 1, 1);
        ComplexOffset b = new ComplexOffset("b", 3, 17);
        ComplexOffset c = new ComplexOffset("c", 89, 88888);
        store.saveOffset("a", a);
        store.saveOffset("b", b);
        store.saveOffset("c", c);

        // Then
        verifyHasOffsets(store, "a", "b", "c");
        verifyOffsets(store, Map.of("a", a, "b", b, "c", c));
    }

    @Test
    public void givenComplexOffset_whenUpdatingKey_thenLatestValueIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);
        verifyOffsetsNotPresent(store, "test");

        // When and Then
        ComplexOffset original = new ComplexOffset("test", 1, 2);
        store.saveOffset("test", original);
        verifyOffsets(store, Map.of("test", original));
        ComplexOffset updated = new ComplexOffset("test", 1, 500);
        store.saveOffset("test", updated);
        verifyOffsets(store, Map.of("test", updated));
        ComplexOffset mostRecent = new ComplexOffset("test", 1, 10);
        store.saveOffset("test", mostRecent);
        verifyOffsets(store, Map.of("test", mostRecent));
    }

    @Test
    public void givenComplexOffset_whenDeletingOffsets_thenOffsetsAreNotRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);
        verifyOffsetsNotPresent(store, "test");

        // When
        ComplexOffset test = new ComplexOffset("test", 1, 1);
        store.saveOffset("test", test);
        verifyOffsets(store, Map.of("test", test));
        store.deleteOffset("test");

        // Then
        verifyOffsetsNotPresent(store, "test");
    }

    @Test
    public void givenComplexOffset_whenNotPopulated_thenNothingIsRetrieved() {
        // Given
        OffsetStore store = createOffsetStore();
        requireLongOffsets(store);

        // When
        // Intentionally not populating the offset store

        // Then
        verifyOffsetsNotPresent(store, "test");
    }

    @DataProvider(name = "mixedOffsets")
    public Object[][] mixedOffsets() {
        ComplexOffset complex = new ComplexOffset("test", 1, 2);
        return new Object[][] {
                { 100L, "data", complex },
                { "data", complex, 100L },
                { complex, 100L, "data" },
                { complex, 100L, 10_000L, 100_000L, "data", 1_000_000L },
                { complex, "data", 100L, null },
                { null, null, null, 100L }
        };
    }

    @Test(dataProvider = "mixedOffsets")
    public void givenMixedOffsets_whenTypeChanges_thenMostRecentTypeIsRetrieved(Object[] mixedOffsets) {
        // Given
        OffsetStore store = createOffsetStore();
        for (Object offset : mixedOffsets) {
            if (offset != null) {
                if (!store.supportsOffsetType(offset.getClass())) {
                    unsupportedOffsetType(store, offset.getClass());
                }
            }
        }

        // When
        for (Object offset : mixedOffsets) {
            store.saveOffset("test", offset);
        }

        // Then
        if (mixedOffsets[mixedOffsets.length - 1] == null) {
            Assert.assertNull(store.loadOffset("test"));
        } else {
            verifyOffsets(store, Map.of("test", mixedOffsets[mixedOffsets.length - 1]));
        }
    }

    @Test
    public void givenPersistentStore_whenStoredClosedAndReopened_thenPersistentOffsetsAreRetrieved() {
        // Given
        if (!this.isPersistent()) {
            throw new SkipException("This test requires a persistent OffsetStore implementation");
        }
        OffsetStore store = this.createOffsetStore();
        requireLongOffsets(store);

        // When
        store.saveOffset("test", 12345L);
        store.close();
        store = this.createOffsetStore();

        // Then
        verifyHasOffsets(store, "test");
        verifyOffsets(store, Map.of("test", 12345L));
    }

    @Test
    public void givenPersistentStore_whenNotPopulatedClosedAndReopened_thenNothingIsRetrieved() {
        // Given
        if (!this.isPersistent()) {
            throw new SkipException("This test requires a persistent OffsetStore implementation");
        }
        OffsetStore store = this.createOffsetStore();
        requireLongOffsets(store);

        // When
        store.close();
        store = this.createOffsetStore();

        // Then
        verifyOffsetsNotPresent(store);
    }

    private void requireLongOffsets(OffsetStore store) {
        if (!store.supportsOffsetType(Long.class)) {
            unsupportedOffsetType(store, Long.class);
        }
    }

    @Test(dataProvider = "mixedOffsets")
    public void givenPersistentStoreAndMixedOffsets_whenClosingAndReopening_thenMostRecentTypeIsRetrieved(
            Object[] mixedOffsets) {
        // Given
        if (!this.isPersistent()) {
            throw new SkipException("This test requires a persistent OffsetStore implementation");
        }
        OffsetStore store = this.createOffsetStore();
        for (Object offset : mixedOffsets) {
            if (offset != null) {
                if (!store.supportsOffsetType(offset.getClass())) {
                    unsupportedOffsetType(store, offset.getClass());
                }
            }
        }

        // When
        for (Object offset : mixedOffsets) {
            store.saveOffset("test", offset);
        }
        store.close();
        store = this.createOffsetStore();

        // Then
        if (mixedOffsets[mixedOffsets.length - 1] == null) {
            Assert.assertNull(store.loadOffset("test"));
        } else {
            verifyOffsets(store, Map.of("test", mixedOffsets[mixedOffsets.length - 1]));
        }
    }

    @Test(dataProvider = "mixedOffsets")
    public void givenPersistentStoreAndMixedOffsets_whenClosingAndReopeningRepeatedly_thenMostRecentTypeIsRetrieved(
            Object[] mixedOffsets) {
        // Given
        if (!this.isPersistent()) {
            throw new SkipException("This test requires a persistent OffsetStore implementation");
        }
        OffsetStore store = this.createOffsetStore();
        for (Object offset : mixedOffsets) {
            if (offset != null) {
                if (!store.supportsOffsetType(offset.getClass())) {
                    unsupportedOffsetType(store, offset.getClass());
                }
            }
        }

        // When
        for (Object offset : mixedOffsets) {
            store.saveOffset("test", offset);
            store.close();
            store = this.createOffsetStore();

            // Then
            if (offset == null) {
                Assert.assertNull(store.loadOffset("test"));
            } else {
                verifyOffsets(store, Map.of("test", offset));
            }
        }
    }

    @Test
    public void givenDelayedPersistentStore_whenStoringOffsetsAndCreatingAdditionalInstance_thenInstancesAreNotInSync() {
        // Given
        if (!this.isPersistent()) {
            throw new SkipException("This test requires a persistent OffsetStore implementation");
        } else if (!this.isPersistenceDelayed()) {
            throw new SkipException("This test requires an OffsetStore implementation with delayed persistence");
        }
        OffsetStore a = this.createOffsetStore();
        OffsetStore b = this.createOffsetStore();
        requireLongOffsets(a);
        requireLongOffsets(b);

        // When
        a.saveOffset("test", 12345L);

        // Then
        verifyOffsets(a, Map.of("test", 12345L));
        verifyOffsetsNotPresent(b, "test");

        // When
        a.flush();
        OffsetStore c = this.createOffsetStore();

        // Then
        verifyOffsets(c, Map.of("test", 12345L));
    }

    @Test
    public void givenImmediatelyPersistentStore_whenStoringOffsetsAndCreatingAdditionalInstance_thenInstancesAreInSync() {
        // Given
        if (!this.isPersistent()) {
            throw new SkipException("This test requires a persistent OffsetStore implementation");
        } else if (this.isPersistenceDelayed()) {
            throw new SkipException("This test requires an OffsetStore implementation with immediate persistence");
        }
        OffsetStore a = this.createOffsetStore();
        OffsetStore b = this.createOffsetStore();
        requireLongOffsets(a);
        requireLongOffsets(b);

        // When
        a.saveOffset("test", 12345L);

        // Then
        verifyOffsets(a, Map.of("test", 12345L));
        verifyOffsets(b, Map.of("test", 12345L));

        // When
        OffsetStore c = this.createOffsetStore();

        // Then
        verifyOffsets(c, Map.of("test", 12345L));
    }
}
