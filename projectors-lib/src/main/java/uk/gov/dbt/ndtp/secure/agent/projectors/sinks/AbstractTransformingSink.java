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

import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;

/**
 * An abstract sink that transforms its input and forwards it onto another sink
 */
public abstract class AbstractTransformingSink<TInput, TOutput> implements Sink<TInput> {

    /**
     * The destination sink to which this sink forwards its output
     */
    protected final Sink<TOutput> destination;

    /**
     * Creates a new sink with an optional forwarding destination
     * <p>
     * If no forwarding destination is provided then the discarding {@link NullSink} is used.
     * </p>
     *
     * @param destination Forwarding destination
     */
    public AbstractTransformingSink(Sink<TOutput> destination) {
        this.destination = destination != null ? destination : new NullSink<>();
    }

    @Override
    public void send(TInput item) throws SinkException {
        if (!shouldForward(item)) {
            return;
        }

        TOutput output = transform(item);
        forward(output);
    }

    /**
     * Determines whether the derived implementation will forward this item on.
     * <p>
     * Defaults to {@code true} meaning all items are always forwarded on.  This is called prior to
     * {@link #transform(Object)} and {@link #forward(Object)} being called meaning that derived implementations can
     * avoid running their transformation logic if the item isn't being forwarded.
     * </p>
     *
     * @param item Item
     * @return True if item should be forwarded, false otherwise
     */
    protected boolean shouldForward(TInput item) {
        return true;
    }

    /**
     * Transforms the input of the sink into an output
     *
     * @param input Input
     * @return Output
     */
    protected abstract TOutput transform(TInput input);

    /**
     * Forwards the output of this sink onto the destination sink
     *
     * @param object Item to forwarded
     * @throws SinkException Thrown if the destination sink fails to accept the item
     */
    protected final void forward(TOutput object) {
        this.destination.send(object);
    }

    @Override
    public void close() {
        // Pass onwards to destination sink
        this.destination.close();
    }
}
