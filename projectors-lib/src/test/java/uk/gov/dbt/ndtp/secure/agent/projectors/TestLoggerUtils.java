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
package uk.gov.dbt.ndtp.secure.agent.projectors;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import java.util.Iterator;
import java.util.stream.Stream;

public class TestLoggerUtils {
    /**
     * Gets the stream of formatted log messages from the test logger
     * <p>
     * The {@link TestLogger} infrastructure stores {@link LoggingEvent} instances that contain
     * all the arguments passed to the logger <strong>BUT</strong> does not format the messages.  If we want to test
     * that the actual log messages are as expected we need to do the formatting ourselves.
     * </p>
     *
     * @param testLogger Test Logger
     * @return Formatted log messages
     */
    public static Stream<String> formattedLogMessages(TestLogger testLogger) {
        return testLogger.getLoggingEvents().stream().map(e -> {
            String formatted = e.getMessage();
            Iterator<Object> args = e.getArguments().iterator();
            while (formatted.contains("{}") && args.hasNext()) {
                Object arg = args.next();
                formatted = formatted.replaceFirst("\\{\\}", arg != null ? arg.toString() : "null");
            }
            return formatted;
        });
    }
}
