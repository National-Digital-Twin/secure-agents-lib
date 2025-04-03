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
package uk.gov.dbt.ndtp.secure.agent.live.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import org.slf4j.event.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLiveError {

    @Test
    public void live_error_equality_01() {
        LiveError error = LiveError.create()
                                   .id("equality-01")
                                   .now()
                                   .type("EqualityTest")
                                   .message("This is an equality test")
                                   .level(Level.ERROR)
                                   .build();
        Assert.assertTrue(error == error);
        Assert.assertFalse(error == null);
        Assert.assertTrue(error.equals(error));
        Assert.assertFalse(error.equals(null));
        Assert.assertFalse(error.equals(new Object()));
    }

    @Test
    public void live_error_equality_02() {
        LiveError a = LiveError.create()
                               .id("equality-03")
                               .now()
                               .type("EqualityTest")
                               .message("This is an equality test")
                               .error(null)
                               .traceback("foo caused bar")
                               .level(Level.WARN)
                               .recordCounter(13L)
                               .securityLabels(Map.of("error_message", "clearance=TS"))
                               .securityLabel("level", "clearance=O")
                               .build();
        LiveError b = a.copy();

        verifyEquality(a, b);

        // Modify each field one by one
        b.setCounter(123L);
        verifyInequality(a, b);
        b.setTimestamp(Date.from(Instant.now().plusSeconds(10)));
        verifyInequality(a, b);
        b.setLevel(Level.INFO.toString());
        verifyInequality(a, b);
        b.setTraceback("foo");
        verifyInequality(a, b);
        b.setMessage("This is an inequality test");
        verifyInequality(a, b);
        b.setType("InequalityTest");
        verifyInequality(a, b);
        b.setId("inequality-03");
        verifyInequality(a, b);
        b.setSecurityLabels(Map.of("error_message", "clearance=S"));
        verifyInequality(a, b);

    }

    private static void verifyEquality(LiveError a, LiveError b) {
        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    private static void verifyInequality(LiveError a, LiveError b) {
        Assert.assertFalse(a.equals(b));
        Assert.assertFalse(b.equals(a));
        Assert.assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void live_error_throwables_01() {
        LiveError error = LiveError.create().build();
        Assert.assertNull(error.getMessage());
        Assert.assertNull(error.getTraceback());

        error.setError(null);
        Assert.assertNull(error.getMessage());
        Assert.assertNull(error.getTraceback());
    }

    @Test
    public void live_error_throwables_02() {
        LiveError error = LiveError.create().build();
        error.setError(new RuntimeException("foo"));
        Assert.assertNotNull(error.getMessage());
        Assert.assertNotNull(error.getTraceback());
    }

    @Test
    public void live_error_timestamps_01() {
        LiveError error = LiveError.create().build();
        Assert.assertNull(error.getTimestamp());

        error.setTimestampToNow();
        Assert.assertNotNull(error.getTimestamp());
    }

    @Test
    public void live_error_timestamps_02() {
        LiveError error = LiveError.create().build();
        Assert.assertNull(error.getTimestamp());

        Date date = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        error.setTimestamp(date);
        Assert.assertNotNull(date);
        Assert.assertEquals(error.getTimestamp(), date);
    }
}
