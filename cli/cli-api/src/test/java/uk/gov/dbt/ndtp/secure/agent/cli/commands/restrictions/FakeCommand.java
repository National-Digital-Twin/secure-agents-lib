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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.restrictions;

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import org.apache.kafka.common.utils.Bytes;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.options.FileSourceOptions;
import uk.gov.dbt.ndtp.secure.agent.cli.options.KafkaOptions;
import uk.gov.dbt.ndtp.secure.agent.cli.restrictions.SourceRequired;

@Command(name = "fake")
public class FakeCommand extends SecureAgentCommand {

    @AirlineModule
    private final KafkaOptions kafka = new KafkaOptions();

    @AirlineModule
    private final FileSourceOptions<Bytes, Bytes> fileSource = new FileSourceOptions<>();

    @Option(name = "--fake-source", arity = 1)
    @SourceRequired(name = "fake", unlessEnvironment = "FAKE_SOURCE")
    private String fakeSource;

    @Override
    public int run() {
        return 0;
    }
}
