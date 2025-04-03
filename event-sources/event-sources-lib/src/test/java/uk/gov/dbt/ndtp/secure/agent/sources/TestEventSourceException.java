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
package uk.gov.dbt.ndtp.secure.agent.sources;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestEventSourceException {

    private void verifyException(EventSourceException e, String expectedMessage, Throwable expectedCause) {
        Assert.assertNotNull(e);

        Assert.assertEquals(e.getMessage(), expectedMessage);
        Assert.assertEquals(e.getCause(), expectedCause);
    }

    @Test
    public void event_source_exception_01() {
        EventSourceException e = new EventSourceException((String) null);
        verifyException(e, null, null);
    }

    @Test
    public void event_source_exception_02() {
        EventSourceException e = new EventSourceException((Throwable) null);
        verifyException(e, null, null);
    }

    @Test
    public void event_source_exception_03() {
        EventSourceException e = new EventSourceException("Error");
        verifyException(e, "Error", null);
    }

    @Test
    public void event_source_exception_04() {
        IllegalStateException cause = new IllegalStateException();
        EventSourceException e = new EventSourceException("Error", cause);
        verifyException(e, "Error", cause);
    }
}
