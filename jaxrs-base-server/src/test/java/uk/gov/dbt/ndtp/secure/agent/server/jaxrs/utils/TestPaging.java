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
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestPaging {

    private static List<Integer> range(int start, int stop) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = start; i <= stop; i++) {
            numbers.add(i);
        }
        return numbers;
    }

    @DataProvider(name = "inputLists")
    public Object[][] createInputLists() {
        return new Object[][] {
                { Collections.<Integer>emptyList() },
                { range(1, 5) },
                { range(1, 10) },
                { range(1, 100) },
                { range(1, 10000) }
        };
    }

    @Test(dataProvider = "inputLists")
    public void paging_unlimited_01(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(Paging.UNLIMITED, Paging.FIRST_OFFSET, input);
        Assert.assertSame(input, actual);
    }

    @Test(dataProvider = "inputLists")
    public void paging_unlimited_02(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(Long.MIN_VALUE, Paging.FIRST_OFFSET, input);
        Assert.assertSame(input, actual);
    }

    @Test(dataProvider = "inputLists")
    public void paging_no_results(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(0L, Paging.FIRST_OFFSET, input);
        if (!input.isEmpty()) {
            Assert.assertNotSame(input, actual);
        }
        Assert.assertTrue(actual.isEmpty());
    }

    @Test(dataProvider = "inputLists")
    public void paging_offset_01(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(Paging.UNLIMITED, 2L, input);
        if (!input.isEmpty()) {
            Assert.assertNotSame(input, actual);
            Assert.assertEquals(actual.size(), input.size() - 1);
        }
    }

    @Test(dataProvider = "inputLists")
    public void paging_offset_02(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(Paging.UNLIMITED, 10L, input);
        if (!input.isEmpty()) {
            Assert.assertNotSame(input, actual);
            if (input.size() >= 10) {
                Assert.assertEquals(actual.size(), input.size() - 9);
            } else {
                Assert.assertTrue(actual.isEmpty());
            }
        }
    }

    @Test(dataProvider = "inputLists")
    public void paging_offset_03(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(Paging.UNLIMITED, Long.MAX_VALUE, input);
        Assert.assertTrue(actual.isEmpty());
    }

    @Test(dataProvider = "inputLists")
    public void paging_offset_04(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(1L, 10L, input);
        if (!input.isEmpty()) {
            Assert.assertNotSame(input, actual);
            if (input.size() >= 10) {
                Assert.assertEquals(actual.size(), 1);
            } else {
                Assert.assertTrue(actual.isEmpty());
            }
        }
    }

    @Test(dataProvider = "inputLists")
    public void paging_limit_01(List<Integer> input) {
        List<Integer> actual = Paging.applyPaging(10L, Paging.FIRST_OFFSET, input);
        if (input.size() >= 10) {
            Assert.assertEquals(actual.size(), 10);
        } else {
            Assert.assertSame(input, actual);
            Assert.assertEquals(actual.size(), input.size());
        }
    }
}
