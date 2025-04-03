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

import static uk.gov.dbt.ndtp.secure.agent.cli.options.LoggingCommand.DEBUGGING_MESSAGE;
import static uk.gov.dbt.ndtp.secure.agent.cli.options.LoggingCommand.ERROR_MESSAGE;
import static uk.gov.dbt.ndtp.secure.agent.cli.options.LoggingCommand.INFORMATION_MESSAGE;
import static uk.gov.dbt.ndtp.secure.agent.cli.options.LoggingCommand.TRACING_MESSAGE;
import static uk.gov.dbt.ndtp.secure.agent.cli.options.LoggingCommand.WARNING_MESSAGE;

import uk.gov.dbt.ndtp.secure.agent.cli.commands.AbstractCommandHelper;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommandTester;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class TestLoggerCommand extends AbstractCommandHelper {
    @AfterMethod
    @Override
    public void testCleanup() throws InterruptedException {
        super.testCleanup();

        LoggingOptions.resetLogging();
    }

    private void verifyStdErr(String[] expected, String[] unexpected) {
        String stdErr = SecureAgentCommandTester.getLastStdErr();

        for (String message : expected) {
            Assert.assertTrue(StringUtils.contains(stdErr, message),
                              "Standard error missing expected message " + message);
        }

        for (String message : unexpected) {
            Assert.assertFalse(StringUtils.contains(stdErr, message),
                               "Standard error contains unexpected message " + message);
        }
    }

    @Test
    public void logging_default() {
        LoggingCommand.main(new String[0]);

        verifyStdErr(new String[] { ERROR_MESSAGE, WARNING_MESSAGE, INFORMATION_MESSAGE },
                     new String[] { DEBUGGING_MESSAGE, TRACING_MESSAGE });
    }

    @Test
    public void logging_quiet_01() {
        LoggingCommand.main(new String[] { "--quiet" });

        // Runtime info is displayed before log level gets reconfigured so should still be present even with --quiet
        verifyStdErr(new String[] { ERROR_MESSAGE, WARNING_MESSAGE, "set to WARN level", "Memory:", "OS:", "Java:" },
                     new String[] { INFORMATION_MESSAGE, DEBUGGING_MESSAGE, TRACING_MESSAGE });
    }

    @Test
    public void logging_quiet_02() {
        LoggingCommand.main(new String[] { "--quiet", "--no-runtime-info" });

        verifyStdErr(new String[] { ERROR_MESSAGE, WARNING_MESSAGE, "set to WARN level", },
                     // Explicitly disabled runtime info so should not be present
                     new String[] {
                             INFORMATION_MESSAGE,
                             DEBUGGING_MESSAGE,
                             TRACING_MESSAGE,
                             "Memory:",
                             "OS:",
                             "Java:"
                     });
    }

    @Test
    public void logging_verbose() {
        LoggingCommand.main(new String[] { "--verbose" });

        verifyStdErr(new String[] {
                ERROR_MESSAGE, WARNING_MESSAGE, INFORMATION_MESSAGE, DEBUGGING_MESSAGE, "set to DEBUG level"
        }, new String[] { TRACING_MESSAGE });
    }

    @Test
    public void logging_trace() {
        LoggingCommand.main(new String[] { "--trace" });

        verifyStdErr(new String[] {
                ERROR_MESSAGE,
                WARNING_MESSAGE,
                INFORMATION_MESSAGE,
                DEBUGGING_MESSAGE,
                TRACING_MESSAGE,
                "set to TRACE level"
        }, new String[0]);
    }
}
