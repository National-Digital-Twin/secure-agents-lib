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
package uk.gov.dbt.ndtp.secure.agent.sources.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestNumericFilenameComparator {

    private void verifyComparator(List<File> files, List<File> expected) {
        // Copy the input and sort it with a new comparator
        List<File> actual = new ArrayList<>(files);
        actual.sort(new NumericFilenameComparator());

        // Verify order is as expected
        Assert.assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++) {
            Assert.assertEquals(actual.get(i), expected.get(i), "Mismatched files at index " + i);
        }
    }

    @Test
    public void test_numeric_filename_comparator_01() {
        File one = new File("test1.txt");
        File two = new File("test2.txt");
        File three = new File("test3.txt");

        List<File> input = List.of(one, two, three);
        verifyComparator(input, input);
    }

    @Test
    public void test_numeric_filename_comparator_02() {
        File one = new File("test1.txt");
        File two = new File("test2.txt");
        File three = new File("test3.txt");

        List<File> input = List.of(three, two, one);
        verifyComparator(input, List.of(one, two, three));
    }

    @Test
    public void test_numeric_filename_comparator_03() {
        File one = new File("test1.txt");
        File two = new File("test2.txt");
        File three = new File("test3.txt");

        List<File> input = List.of(three, one, two);
        verifyComparator(input, List.of(one, two, three));
    }

    @DataProvider(name = "sizes")
    public Object[][] sizes() {
        return new Object[][] {
                { 10 },
                { 100 },
                { 1_000 },
                { 10_000 },
                { 100_000 }
        };
    }

    @Test(dataProvider = "sizes", invocationCount = 5)
    public void test_numeric_filename_comparator_04(int size) {
        // Generate a large random sequence of filenames
        List<File> expected = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            expected.add(new File("test" + i + ".txt"));
            numbers.add(i);
        }
        List<File> input = new ArrayList<>();
        Random random = new Random();
        while (!numbers.isEmpty()) {
            int num = random.nextInt(numbers.size());
            input.add(new File("test" + numbers.get(num) + ".txt"));
            numbers.remove(num);
        }

        verifyComparator(input, expected);
    }

    @Test(dataProvider = "sizes", invocationCount = 5)
    public void test_numeric_filename_comparator_05(int size) {
        // Generate a large random sequence of filenames
        List<File> expected = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= size / 100; i++) {
            for (int j = 0; j < 100; j++) {
                expected.add(new File("test" + i + ".txt"));
                numbers.add(i);
            }
        }
        List<File> input = new ArrayList<>();
        Random random = new Random();
        while (!numbers.isEmpty()) {
            int num = random.nextInt(numbers.size());
            input.add(new File("test" + numbers.get(num) + ".txt"));
            numbers.remove(num);
        }

        verifyComparator(input, expected);
    }

    @Test
    public void test_numeric_filename_comparator_06() {
        File noNumerics = new File("test.txt");
        File outOfBounds = new File("test" + Long.MAX_VALUE + "000.txt");
        File numeric = new File("test1234.txt");

        List<File> input = List.of(numeric, outOfBounds, noNumerics);
        verifyComparator(input, List.of(noNumerics, outOfBounds, numeric));
    }

    @Test
    public void test_numeric_filename_comparator_07() {
        NumericFilenameComparator comparator = new NumericFilenameComparator();
        File f = new File("test1.txt");
        Assert.assertEquals(comparator.compare(null, f), -1);
        Assert.assertEquals(comparator.compare(f, null), 1);
    }

    @Test
    public void test_numeric_filename_comparator_08() {
        NumericFilenameComparator comparator = new NumericFilenameComparator();
        File f = new File("test.txt");
        Assert.assertEquals(comparator.compare(null, f), -1);
        Assert.assertEquals(comparator.compare(f, null), 1);
    }

    @Test
    public void test_numeric_filename_comparator_09() {
        NumericFilenameComparator comparator = new NumericFilenameComparator();
        Assert.assertEquals(comparator.compare(null, null), 0);
    }

    @Test
    public void test_numeric_filename_comparator_10() {
        NumericFilenameComparator comparator = new NumericFilenameComparator();
        File a = new File("1234.txt");
        File b = new File("1234");
        Assert.assertTrue(comparator.compare(a, b) >= 1);
        Assert.assertTrue(comparator.compare(b, a) <= -1);
    }

}
