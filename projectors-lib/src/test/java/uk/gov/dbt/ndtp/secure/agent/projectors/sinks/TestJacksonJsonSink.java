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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;

public class TestJacksonJsonSink extends AbstractSinkHelper {

    @Test
    public void givenSimpleItem_whenSendingToJson_thenSimpleJsonOutput() {
        // Given
        List<Object> values = Collections.singletonList("foo");

        // When and Then
        List<String> expectedJson = List.of("\"foo\"");
        verifyJacksonJson(values, false, expectedJson);
    }

    @Test
    public void givenObject_whenSendingToJson_thenJsonObjectOutput() {
        // Given
        Map<String, Object> map = createJohnSmithObject();
        List<Object> values = Collections.singletonList(map);

        // When and Then
        List<String> expectedJson =
                List.of("{\"aliases\":[\"J Smith\",\"John Frederick Smith\"],\"name\":\"John Smith\"}");
        verifyJacksonJson(values, false, expectedJson);
    }

    private static Map<String, Object> createJohnSmithObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John Smith");
        map.put("aliases", Arrays.asList("J Smith", "John Frederick Smith"));
        return map;
    }

    @Test
    public void givenObject_whenSendingToJsonWithPrettyPrinting_thenPrettyJsonObjectOutput() {
        // Given
        Map<String, Object> map = createJohnSmithObject();
        List<Object> values = Collections.singletonList(map);

        // When and Then
        List<String> expectedJson = Collections.singletonList(StringUtils.join("{", System.lineSeparator(),
                                                                               "  \"aliases\" : [ \"J Smith\", \"John Frederick Smith\" ],",
                                                                               System.lineSeparator(),
                                                                               "  \"name\" : \"John Smith\"",
                                                                               System.lineSeparator(), "}"));
        verifyJacksonJson(values, true, expectedJson);
    }

    @Test
    public void givenListOfObjects_whenSendingToJson_thenCorrectJsonOutput() {
        // Given
        Map<String, Object> map = createJohnSmithObject();
        List<Object> values = Arrays.asList("foo", map);

        // When and Then
        List<String> expectedJson = Arrays.asList("\"foo\"",
                                                  "{\"aliases\":[\"J Smith\",\"John Frederick Smith\"],\"name\":\"John Smith\"}");
        verifyJacksonJson(values, false, expectedJson);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullOutputAndPrettyPrintingDisabled_whenConstructing_thenNPE() {
        new JacksonJsonSink<>(null, false);
    }

    @SuppressWarnings("resource")
    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullOutputAndPrettyPrintingEnabled_whenConstructing_thenNPE() {
        new JacksonJsonSink<>(null, true);
    }

    @Test(expectedExceptions = SinkException.class, expectedExceptionsMessageRegExp = "Failed to serialize.*")
    public void givenClosedOutput_whenSendingItem_thenSinkException() throws IOException {
        // Given
        CloseShieldOutputStream output = CloseShieldOutputStream.wrap(System.out);
        output.close();

        // When
        try (JacksonJsonSink<String> sink = new JacksonJsonSink<>(output, false)) {
            // Then
            // Expect this to throw a sink error as the output stream is closed
            sink.send("test");
        }
    }

    @Test
    public void givenNoParameters_whenCreatingSink_thenDefaultsAreUsed() {
        // Given and When
        try (InspectableJacksonJsonSink sink = new InspectableJacksonJsonSink()) {
            // Then
            Assert.assertEquals(sink.getOutputStream(), System.out);
            Assert.assertFalse(sink.getObjectMapper().isEnabled(SerializationFeature.INDENT_OUTPUT));
        }
    }

    @Test
    public void givenPrettyPrinting_whenCreatingSink_thenPrettyPrintingEnabled() {
        // Given and When
        try (InspectableJacksonJsonSink sink = new InspectableJacksonJsonSink(true)) {
            // Then
            Assert.assertEquals(sink.getOutputStream(), System.out);
            Assert.assertTrue(sink.getObjectMapper().isEnabled(SerializationFeature.INDENT_OUTPUT));
        }
    }

    protected void verifyJacksonJson(List<Object> values, boolean prettyPrint, List<String> expectedJson) {
        // When
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JacksonJsonSink<Object> sink = new JacksonJsonSink<>(output, prettyPrint)) {
            for (int i = 0; i < values.size(); i++) {
                sink.send(values.get(i));

                // Then
                String actualJson = output.toString(StandardCharsets.UTF_8);
                String expected = expectedJson.get(i);
                Assert.assertEquals(actualJson, expected,
                                    "Object at Index " + i + " did not produce the expected JSON");
                output.reset();
            }
        }
    }
}
