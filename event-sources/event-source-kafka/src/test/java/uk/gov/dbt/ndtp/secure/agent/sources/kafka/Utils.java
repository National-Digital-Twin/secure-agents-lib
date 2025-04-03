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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.testng.Assert;

public class Utils {
    /**
     * Creates mock Kafka headers
     *
     * @param data Dictionary of headers to expose
     * @return Mocked Kafka Headers
     */
    public static Headers createMockHeaders(Map<String, String> data) {
        Headers headers = mock(Headers.class);
        data.forEach((key, value) -> {
            Header header = mock(Header.class);
            when(header.key()).thenReturn(key);
            if (value != null) {
                when(header.value()).thenReturn(value.getBytes(StandardCharsets.UTF_8));
            } else {
                when(header.value()).thenReturn(null);
            }
            when(headers.headers(key)).thenReturn(Collections.singletonList(header));
        });
        when(headers.toArray()).thenReturn(data.entrySet()
                                               .stream()
                                               .map(e -> new RecordHeader(e.getKey(), e.getValue() != null ?
                                                                                      e.getValue().getBytes(
                                                                                              StandardCharsets.UTF_8) :
                                                                                      null)).toArray(Header[]::new));
        return headers;
    }

    /**
     * Logs that a test class is starting
     * <p>
     * Mainly intended to give developers running from the command line some visibility that test classes are actually
     * firing because some of them take a long time due to Test Containers spin up.
     * </p>
     *
     * @param cls Test class
     */
    public static void logTestClassStarted(Class<?> cls) {
        logStdOut("Started test class %s", cls.getCanonicalName());
    }

    /**
     * Logs that a test class is finished
     * <p>
     * Mainly intended to give developers running from the command line some visibility that test classes are actually
     * firing because some of them take a long time due to Test Containers spin up.
     * </p>
     *
     * @param cls Test class
     */
    public static void logTestClassFinished(Class<?> cls) {
        logStdOut("Finished test class %s", cls.getCanonicalName());
    }

    /**
     * Logs a formatted message to standard out
     * <p>
     * This is required because the tests in this module use a special slf4j implementation that captures the logs
     * in-memory for inspection and verification during tests.  This means anything we actually want the developer
     * running the tests via Maven to see has to be directly written to standard output.
     * </p>
     *
     * @param message Message format
     * @param args    Format arguments
     */
    public static void logStdOut(String message, Object... args) {
        System.out.print("[");
        System.out.print(Instant.now().toString());
        System.out.print("] ");
        System.out.format(message, args);
        System.out.println();
    }

    /**
     * Waits a while for a described event to occur, returning whether the event did occur within the wait period. This
     * implementation makes 3 attempts to check for the occurrence of the event, waiting for 3 seconds between checks.
     *
     * @param eventDescription a description of the event to check, for logging purposes.
     * @param eventChecker a checker for the event occurrence, returning the outcome of the check after up to a maximum number of checks were made.
     * @return true, if the event checker determined that the event occurred, false otherwise.
     */
    public static boolean waitAWhileFor(final String eventDescription, final BooleanSupplier eventChecker) {
        for (int n=1; n <= 3; n++) {
            boolean checkSuccessFull = eventChecker.getAsBoolean();
            if (checkSuccessFull) {
                return true;
            }

            logStdOut("Waiting for event [%s] to occur...after attempt %d", eventDescription, n);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logStdOut("Wait for event [%s] was interrupted [%s] on attempt %d, ending wait for this event...", eventDescription, n, e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    /**
     * Waits a while for a described event to occur, failing if the event did not occur within the wait period. This
     * implementation makes 3 attempts to check for the occurrence of the event, waiting for 3 seconds between checks.
     *
     * @param eventDescription a description of the event to check, for logging purposes.
     * @param eventChecker a checker for the event occurrence, returning the outcome of the check after up to a maximum number of checks were made.
     * @see #waitAWhileFor(String, BooleanSupplier)
     */
    public static void waitAWhileOrFailFor(final String eventDescription, final BooleanSupplier eventChecker) {
        Assert.assertTrue(waitAWhileFor(eventDescription, eventChecker), "Event ["+eventDescription+"] did not occur within the allotted time period.");
    }
}
