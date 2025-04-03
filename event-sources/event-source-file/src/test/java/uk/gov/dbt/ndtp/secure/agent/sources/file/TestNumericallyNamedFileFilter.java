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
import java.io.IOException;
import java.nio.file.Files;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestNumericallyNamedFileFilter {

    @Test
    public void numerically_named_file_filter_01() throws IOException {
        NumericallyNamedWithExtensionFilter filter = new NumericallyNamedWithExtensionFilter(".foo");
        File foo = Files.createTempFile("test", ".foo").toFile();
        File bar = Files.createTempFile("test", ".bar").toFile();
        foo.deleteOnExit();
        bar.deleteOnExit();
        Assert.assertTrue(filter.accept(foo));
        Assert.assertFalse(filter.accept(bar));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*cannot be null/blank")
    public void numerically_named_file_filter_bad_01() {
        new NumericallyNamedWithExtensionFilter(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*cannot be null/blank")
    public void numerically_named_file_filter_bad_02() {
        new NumericallyNamedWithExtensionFilter("   ");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*must start with a \\. character")
    public void numerically_named_file_filter_bad_03() {
        new NumericallyNamedWithExtensionFilter("foo");
    }
}
