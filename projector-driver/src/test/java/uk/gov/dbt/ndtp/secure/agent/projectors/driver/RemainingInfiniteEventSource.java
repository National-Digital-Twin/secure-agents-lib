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
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;

public class RemainingInfiniteEventSource extends InfiniteEventSource {

    private final Random random = new Random();
    private final AtomicLong remaining = new AtomicLong(0);

    private boolean resetRemaining = true;

    public RemainingInfiniteEventSource(String valueFormat, long sleepBeforeYield) {
        super(valueFormat, sleepBeforeYield);
    }

    public RemainingInfiniteEventSource(String valueFormat, long sleepBeforeYield, long initialRemaining) {
        this(valueFormat, sleepBeforeYield);
        this.resetRemaining = false;
        this.remaining.set(initialRemaining);
    }

    @Override
    public Event<Integer, String> poll(Duration timeout) {
        if (this.resetRemaining) {
            this.remaining.set(this.random.nextLong(-1_000, 1_000));
            this.resetRemaining = false;
        } else if (this.remaining.get() < 0) {
            this.remaining.incrementAndGet();
            if (this.sleepBeforeYield > 0) {
                try {
                    Thread.sleep(this.sleepBeforeYield);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        } else if (this.remaining.get() == 0) {
            this.resetRemaining = true;
            return null;
        }
        Event<Integer, String> event = super.poll(timeout);
        if (event != null) {
            this.remaining.decrementAndGet();
        }
        return event;
    }

    @Override
    public Long remaining() {
        long remaining = this.remaining.get();
        return remaining >= 0L ? remaining : null;
    }

    @Override
    public boolean availableImmediately() {
        return this.remaining.get() > 0 && this.sleepBeforeYield == 0;
    }
}
