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
package uk.gov.dbt.ndtp.secure.agent.projectors.driver;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

/**
 * An infinite event source for projector driver testing
 */
public class InfiniteEventSource implements EventSource<Integer, String> {

    private final AtomicInteger key = new AtomicInteger(0);
    private final String valueFormat;
    final long sleepBeforeYield;

    private boolean closed = false;

    public InfiniteEventSource(String valueFormat, long sleepBeforeYield) {
        this.valueFormat = valueFormat;
        this.sleepBeforeYield = sleepBeforeYield;
    }

    @Override
    public boolean availableImmediately() {
        return !this.closed && this.sleepBeforeYield == 0;
    }

    @Override
    public boolean isExhausted() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public Event<Integer, String> poll(Duration timeout) {
        if (this.sleepBeforeYield > 0) {
            try {
                Thread.sleep(Math.min(this.sleepBeforeYield, timeout.toMillis()));
            } catch (InterruptedException e) {
                return null;
            }
            if (timeout.toMillis() < this.sleepBeforeYield) {
                return null;
            }
        }
        int key = this.key.incrementAndGet();
        return new SimpleEvent<>(Collections.emptyList(), key, String.format(this.valueFormat, key));
    }

    @Override
    public Long remaining() {
        // Infinite so always unknown
        return null;
    }

    @Override
    public void processed(Collection<Event> processedEvents) {
        // No-op
    }

    /**
     * Gets how many events this source has yielded aka how many times {@link #poll(Duration)} was called and completed
     *
     * @return Events yielded
     */
    public int eventsYielded() {
        return this.key.get();
    }
}
