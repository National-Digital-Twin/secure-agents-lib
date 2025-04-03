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
package uk.gov.dbt.ndtp.secure.agent.cli.commands;

import com.github.rvesse.airline.parser.ParseResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;

/**
 * Provides the ability to put {@link SecureAgentCommand} derived command classes into a test mode in order to allow
 * writing unit test cases against their behaviour
 */
public class SecureAgentCommandTester {

    private static final PrintStream ORIGINAL_OUTPUT = System.out, ORIGINAL_ERROR = System.err;
    private static final ByteArrayOutputStream LAST_OUTPUT = new ByteArrayOutputStream(), LAST_ERROR =
            new ByteArrayOutputStream();

    private static File LAST_OUTPUT_FILE = null, LAST_ERROR_FILE = null;

    /**
     * When set to true the captured output and error from each test is also tee'd to the original standard output and
     * error, when set to false it is also captured to files for later review
     */
    public static boolean TEE_TO_ORIGINAL_STREAMS = false;

    private SecureAgentCommandTester() {

    }

    /**
     * Setups command test mode, in this mode commands will not exit the application when invoked (providing they are
     * calling {@link SecureAgentCommand#exit(int)} and not {@link System#exit(int)} directly) and their parse result,
     * exit status and output streams are intercepted.  You can then inspect the parse result, exit status, output and
     * error streams via the various methods on this class.
     */
    public static void setup() {
        SecureAgentCommand.TEST = true;
        resetTestState();

        // Capture output streams
        System.setOut(new PrintStream(new TeeOutputStream(LAST_OUTPUT, getOutputTeeDestination()), true));
        System.setErr(new PrintStream(new TeeOutputStream(LAST_ERROR, getErrorTeeDestination()), true));
    }

    private static OutputStream getErrorTeeDestination() {
        if (TEE_TO_ORIGINAL_STREAMS || LAST_ERROR_FILE == null) {
            return ORIGINAL_ERROR;
        } else {
            try {
                OutputStream output = new FileOutputStream(LAST_ERROR_FILE);
                ORIGINAL_ERROR.println(
                        "Standard error for the next test will be captured to file " + LAST_ERROR_FILE.getAbsolutePath());
                return output;
            } catch (FileNotFoundException e) {
                return NullOutputStream.INSTANCE;
            }
        }
    }

    private static OutputStream getOutputTeeDestination() {
        if (TEE_TO_ORIGINAL_STREAMS || LAST_OUTPUT_FILE == null) {
            return ORIGINAL_OUTPUT;
        } else {
            try {
                OutputStream error = new FileOutputStream(LAST_OUTPUT_FILE);
                ORIGINAL_OUTPUT.println(
                        "Standard output for the next test will be captured to file " + LAST_OUTPUT_FILE.getAbsolutePath());
                return error;
            } catch (FileNotFoundException e) {
                return NullOutputStream.INSTANCE;
            }
        }
    }

    /**
     * Resets internal test state, should be called between individual test cases and command invocations
     */
    public static void resetTestState() {
        SecureAgentCommand.LAST_PARSE_RESULT = null;
        SecureAgentCommand.LAST_EXIT_STATUS = Integer.MIN_VALUE;
        LAST_OUTPUT.reset();
        LAST_ERROR.reset();

        if (!TEE_TO_ORIGINAL_STREAMS) {
            Path target = Path.of("target");
            try {
                LAST_OUTPUT_FILE = Files.createTempFile(target, "stdout", ".txt").toFile();
            } catch (IOException e) {
                LAST_OUTPUT_FILE = null;
            }
            try {
                LAST_ERROR_FILE = Files.createTempFile(target, "stderr", ".txt").toFile();
            } catch (IOException e) {
                LAST_ERROR_FILE = null;
            }
        }
    }

    /**
     * Tears down command test mode i.e. commands will now exit normally when invoked and won't record their parse
     * result and exit status for inspection.
     */
    public static void teardown() {
        SecureAgentCommand.TEST = false;
        resetTestState();

        // Restore original output streams
        System.setOut(ORIGINAL_OUTPUT);
        System.setOut(ORIGINAL_ERROR);
    }

    /**
     * Prints to the original standard output
     *
     * @param value Value
     */
    public static void printToOriginalStdOut(String value) {
        ORIGINAL_OUTPUT.println(value);
    }

    /**
     * Gets the last command parse result (if any)
     *
     * @param <T> Parse result
     * @return Parse result or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T extends SecureAgentCommand> ParseResult<T> getLastParseResult() {
        if (SecureAgentCommand.LAST_PARSE_RESULT == null) {
            return null;
        } else {
            return (ParseResult<T>) SecureAgentCommand.LAST_PARSE_RESULT;
        }
    }

    /**
     * Gets the last command exit code or {@link Integer#MIN_VALUE} if none recorded
     *
     * @return Exit status or {@link Integer#MIN_VALUE}
     */
    public static int getLastExitStatus() {
        return SecureAgentCommand.LAST_EXIT_STATUS;
    }

    /**
     * Gets the standard output from the last command invocation
     *
     * @return Standard output
     */
    public static String getLastStdOut() {
        return LAST_OUTPUT.toString(StandardCharsets.UTF_8);
    }

    /**
     * Gets the standard error from the last command invocation
     *
     * @return Standard error
     */
    public static String getLastStdErr() {
        return LAST_ERROR.toString(StandardCharsets.UTF_8);
    }
}
