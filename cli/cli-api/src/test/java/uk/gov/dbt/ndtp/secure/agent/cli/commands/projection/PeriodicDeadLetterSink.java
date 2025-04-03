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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.projection;

import java.util.Objects;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;

public class PeriodicDeadLetterSink<T> implements Sink<T> {

    private final Sink<T> destination, deadLetters;
    private long count = 0;
    private final long deadLetterFrequency;

    public PeriodicDeadLetterSink(Sink<T> destination, long deadLetterFrequency, Sink<T> deadLetterSink) {
        this.deadLetterFrequency = deadLetterFrequency;
        this.destination = Objects.requireNonNull(destination);
        this.deadLetters = deadLetterSink;
    }

    @Override
    public void send(T item) {
        count++;
        if (count % deadLetterFrequency == 0 && deadLetters != null) {
            deadLetters.send(item);
        } else {
            destination.send(item);
        }
    }

    @Override
    public void close() {
        Sink.super.close();
    }
}
