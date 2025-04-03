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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.debug;

import com.github.rvesse.airline.annotations.Cli;
import com.github.rvesse.airline.annotations.Parser;
import com.github.rvesse.airline.parser.ParseResult;
import com.github.rvesse.airline.parser.errors.handlers.CollectAll;
import com.github.rvesse.airline.parser.options.MaybePairValueOptionParser;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.Capture;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.Dump;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.RdfDump;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.projection.debug.Replay;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.HelpCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;

/**
 * A CLI that provides debug tools
 */
//@formatter:off
@Cli(
        name = "debug",
        description = "Provides commands for debugging Secure Agents pipelines.",
        defaultCommand = HelpCommand.class,
        commands = {
                HelpCommand.class,
                Dump.class,
                FakeReporter.class,
                RdfDump.class,
                Capture.class,
                Replay.class
        },
        parserConfiguration =
        @Parser(flagNegationPrefix = "--no-",
                errorHandler = CollectAll.class,
                defaultParsersFirst = false,
                optionParsers = {
                MaybePairValueOptionParser.class
        })
)
//@formatter:on
public class DebugCli {
    /**
     * Private constructor prevents instantiation
     */
    private DebugCli() {

    }

    /**
     * Entrypoint for the debug CLI
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        com.github.rvesse.airline.Cli<SecureAgentCommand> cli =
                new com.github.rvesse.airline.Cli<>(DebugCli.class);
        ParseResult<SecureAgentCommand> result = cli.parseWithResult(args);

        SecureAgentCommand.handleParseResult(result);
    }
}
