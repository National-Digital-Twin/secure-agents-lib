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
package uk.gov.dbt.ndtp.secure.agent.cli.options;

import com.github.rvesse.airline.annotations.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;

@Command(name = "logging", description = "Logging options test command")
public class LoggingCommand extends SecureAgentCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCommand.class);
    public static final String ERROR_MESSAGE = "Error";
    public static final String WARNING_MESSAGE = "Warning";
    public static final String INFORMATION_MESSAGE = "Information";
    public static final String DEBUGGING_MESSAGE = "Debugging";
    public static final String TRACING_MESSAGE = "Tracing";

    @Override
    public int run() {
        // Print a log message at every log level, this is so tests against this class can check whether the logging
        // options supplied customised the
        LOGGER.error(ERROR_MESSAGE);
        LOGGER.warn(WARNING_MESSAGE);
        LOGGER.info(INFORMATION_MESSAGE);
        LOGGER.debug(DEBUGGING_MESSAGE);
        LOGGER.trace(TRACING_MESSAGE);
        return 0;
    }


    public static void main(String[] args) {
        SecureAgentCommand.runAsSingleCommand(LoggingCommand.class, args);
    }
}
