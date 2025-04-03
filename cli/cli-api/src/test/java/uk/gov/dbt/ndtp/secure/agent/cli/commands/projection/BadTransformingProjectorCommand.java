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

import com.github.rvesse.airline.annotations.Command;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;

@Command(name = "projector")
public class BadTransformingProjectorCommand
        extends TransformingProjectorCommand {

    @Override
    protected Sink<Event<Integer, String>> prepareWorkSink() {
        // BAD - As the command transforms the event types from their original types can't use the declared key/value
        // serializer directly as when the sink receives a dead letter it'll produce a ClassCastException
        // Correct behaviour is shown in parent class of supplying the appropriate serializer classes for the types at
        // the point the event will be dead lettered
        Sink<Event<Integer, String>> deadLetters =
                this.prepareDeadLetterSink(this.kafka.dlqTopic, keySerializerClass(), valueSerializerClass());
        return new PeriodicDeadLetterSink<>(this.deadLetterTestingOptions.successful,
                                            this.deadLetterTestingOptions.deadLetterFrequency,
                                            deadLetters);
    }
}
