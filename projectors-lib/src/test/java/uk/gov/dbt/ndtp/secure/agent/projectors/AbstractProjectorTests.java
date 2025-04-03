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
package uk.gov.dbt.ndtp.secure.agent.projectors;

import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;

public abstract class AbstractProjectorTests<TInput, TOutput> {

    /**
     * Gets an instance of the projector to be tested
     *
     * @return Projector
     */
    protected abstract Projector<TInput, TOutput> getProjector();

    /**
     * Gets some sample input, this input <strong>MUST</strong> cause the projector to produce at least one output
     *
     * @return Sample input
     */
    protected abstract TInput getSampleInput();

    @Test(expectedExceptions = NullPointerException.class)
    public void projector_bad_01() {
        Projector<TInput, TOutput> projector = getProjector();
        // A null sink should not be permitted
        projector.project(getSampleInput(), null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void projector_bad_02() {
        Projector<TInput, TOutput> projector = getProjector();
        NullSink<TOutput> sink = NullSink.of();
        // A null input should not be permitted
        projector.project(null, sink);
    }

    @Test
    public void projector_01() {
        Projector<TInput, TOutput> projector = getProjector();
        NullSink<TOutput> sink = NullSink.of();
        projector.project(getSampleInput(), sink);

        Assert.assertTrue(sink.count() > 0, "Projector must produce at least one output");
    }

    @Test
    public void projector_02() {
        Projector<TInput, TOutput> projector = getProjector();
        NullSink<TOutput> sink = NullSink.of();
        TInput input = getSampleInput();
        projector.project(input, sink);

        long count = sink.count();
        Assert.assertTrue(sink.count() > 0, "Projector must produce at least one output");

        // Repeating the projection on the same input should produce the same number of outputs again
        projector.project(input, sink);
        Assert.assertEquals(sink.count(), count * 2,
                            "Projector should produce same number of outputs each time for same input");
    }

}
