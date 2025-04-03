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

import ch.qos.logback.classic.Level;
import com.github.rvesse.airline.annotations.Option;
import uk.gov.dbt.ndtp.secure.agent.observability.RuntimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides common options related to controlling the application logging at runtime
 */
public class LoggingOptions {

    private static final String MULTIPLE_OPTIONS_BEHAVIOUR =
            "If multiple logging options are supplied then the most verbose logging level requested is applied.";

    @Option(name = { "--verbose" }, arity = 0, description = "Specifies verbose mode i.e. increased logging verbosity. " + MULTIPLE_OPTIONS_BEHAVIOUR)
    private boolean verbose = false;

    @Option(name = { "--trace" }, arity = 0, description = "Specifies trace mode i.e. greatly increased logging verbosity. " + MULTIPLE_OPTIONS_BEHAVIOUR)
    private boolean trace = false;

    @Option(name = { "--quiet" }, arity = 0, description = "Specifies quiet mode i.e. greatly reduced logging verbosity. " + MULTIPLE_OPTIONS_BEHAVIOUR)
    private boolean quiet = false;

    @Option(name = {
            "--runtime-info",
            "--no-runtime-info"
    }, arity = 0, description = "When specified will print basic runtime information (Memory, JVM and OS) to the logs during command startup.  Defaults to enabled, may be disabled by specifying the --no-runtime-info option.")
    private boolean showRuntimeInfo = true;

    /**
     * (Re-)configures logging based on the provided CLI options (if any)
     * <p>
     * This potentially overrides the root logger level set by any Logback configuration file the application is
     * providing.  However, since we are only changing the root logger level if an application has explicitly set the
     * log level for another logger then that level continues to be honoured irregardless of what the user configures at
     * runtime via these options.
     * </p>
     */
    public void configureLogging() {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Logger logger = LoggerFactory.getLogger(LoggingOptions.class);

        // Print the runtime information if requested
        // Intentionally do this prior to reconfiguring the log level as this is logged as INFO level so could be
        // omitted if --quiet was used
        if (this.showRuntimeInfo) {
            RuntimeInfo.printRuntimeInfo(logger);
        }

        // Reconfigure the log level (if needed) and print a warning (which will always remain visible regardless of the
        // logging verbosity selected) so that the fact logging was changed is itself logged!
        if (this.trace) {
            root.setLevel(Level.TRACE);
            logger.warn("Logging set to TRACE level as requested (--trace supplied)");
        } else if (this.verbose) {
            root.setLevel(Level.DEBUG);
            logger.warn("Logging set to DEBUG level as requested (--verbose supplied)");
        } else if (this.quiet) {
            root.setLevel(Level.WARN);
            logger.warn("Logging set to WARN level as requested (--quiet supplied)");
        }
    }

    /**
     * Resets the root logger to the default level, only intended for test usage
     */
    static void resetLogging() {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }
}
