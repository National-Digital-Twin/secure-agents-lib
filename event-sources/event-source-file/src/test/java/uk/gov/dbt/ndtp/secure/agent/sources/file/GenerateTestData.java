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
package uk.gov.dbt.ndtp.secure.agent.sources.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.jena.riot.Lang;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.IANodeHeaders;
import uk.gov.dbt.ndtp.secure.agent.sources.file.gzip.GZipEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.memory.SimpleEvent;

/**
 * Regenerates some of the test data used by this module.  Useful if you change the behaviour of
 * {@link YamlEventReaderWriter} and its underlying
 * {@link uk.gov.dbt.ndtp.secure.agent.sources.file.jackson.AbstractJacksonEventReaderWriter} in any way
 */
public class GenerateTestData {

    public static final Header NQUADS_CONTENT_TYPE =
            new Header("Content-Type", Lang.NQUADS.getContentType().getContentTypeStr());
    public static final Header TOP_SECRET_CLEARANCE_REQUIRED = new Header(
            IANodeHeaders.SECURITY_LABEL, "clearance=TS");
    public static final List<Header> DEFAULT_TEST_HEADERS = List.of(NQUADS_CONTENT_TYPE, TOP_SECRET_CLEARANCE_REQUIRED);
    public static final String SIMPLE_RDF_VALUE = """
            <http://subject> <http://predicate> <http://object> .
            <http://other> <http://predicate> "value" .
            """;

    public static final String TRIG_RDF_VALUE = """
            @prefix : <http://example.org/> .
            :subject :predicate :object ;
                     :value "a" .
            """;
    public static final GZipEventReaderWriter<Integer, String> GZIPPED_WRITER =
            new GZipEventReaderWriter<>(Serdes.YAML_INTEGER_STRING);
    private static File BASE_DIR;

    private static void simple() throws IOException {
        SimpleEvent<Integer, String> simple = generateSimpleEvent(123456789, SIMPLE_RDF_VALUE, "test-data/event.yaml");
        GZIPPED_WRITER.write(simple, new File(BASE_DIR, "test-data/event.yaml.gz"));
    }

    private static void noValue() throws IOException {
        generateSimpleEvent(123456789, null, "test-data/no-value.yaml");
    }

    private static void noKey() throws IOException {
        generateSimpleEvent(null, SIMPLE_RDF_VALUE, "test-data/no-key.yaml");
    }

    private static SimpleEvent<Integer, String> generateSimpleEvent(Integer key, String value, String filename) throws
            IOException {
        SimpleEvent<Integer, String> event = createEvent(DEFAULT_TEST_HEADERS,
                                                         key, value
        );
        Serdes.YAML_INTEGER_STRING.write(event, new File(BASE_DIR, filename));
        return event;
    }

    private static SimpleEvent<Integer, String> createEvent(List<Header> headers, Integer key, String value) {
        return new SimpleEvent<>(
                headers, key, value);
    }

    private static void complex() throws IOException {
        List<Header> headers = DEFAULT_TEST_HEADERS;
        Header key = new Header("Key", "Value");
        Map<String, Object> value =
                Map.of("a", 1, "b", 2, "c", 3, "d", List.of(4, 5, 6), "e", Map.of("f", 7, "g", false));

        SimpleEvent<Header, Map> event = new SimpleEvent<>(headers, key, value);
        String extraData = """
                extra-value: value
                extra-object:
                  foo: bar
                extra-list:
                  - a
                  - b
                  - c
                """;
        try (FileOutputStream output = new FileOutputStream(new File(BASE_DIR, "test-data/complex-event.yaml"))) {
            YamlEventReaderWriter<Header, Map> writer =
                    new YamlEventReaderWriter<>(Serdes.HEADER_SERIALIZER, Serdes.MAP_SERIALIZER);
            try (CloseShieldOutputStream shielded = CloseShieldOutputStream.wrap(output)) {
                writer.write(event, shielded);
            }
            // Add extraneous trailing data that we expect to be ignored
            output.write(extraData.getBytes(StandardCharsets.UTF_8));
        }

    }

    public static void bytes() throws IOException {
        String value = "test";
        Serdes.YAML_BYTES_STRING.write(new SimpleEvent<>(Collections.emptyList(), null, value),
                                       new File(BASE_DIR, "test-data/kafka/bytes1.yaml"));
    }

    public static void rdf() throws IOException {
        SimpleEvent<Integer, String> rdf1 = createEvent(DEFAULT_TEST_HEADERS, 1, SIMPLE_RDF_VALUE);
        Serdes.YAML_INTEGER_STRING.write(rdf1, new File(BASE_DIR, "test-data/rdf/rdf1.yaml"));
        GZIPPED_WRITER.write(rdf1, new File(BASE_DIR, "test-data/rdf/rdf1.yaml.gz"));

        SimpleEvent<Integer, String> rdf2 = createEvent(List.of(TOP_SECRET_CLEARANCE_REQUIRED), 2, SIMPLE_RDF_VALUE);
        Serdes.YAML_INTEGER_STRING.write(rdf2, new File(BASE_DIR, "test-data/rdf/rdf2.yaml"));
        GZIPPED_WRITER.write(rdf2, new File(BASE_DIR, "test-data/rdf/rdf2.yaml.gz"));

        SimpleEvent<Integer, String> rdf3 =
                createEvent(List.of(new Header("Content-Type", Lang.TRIG.getContentType().getContentTypeStr())), 3,
                            TRIG_RDF_VALUE);
        Serdes.YAML_INTEGER_STRING.write(rdf3, new File(BASE_DIR, "test-data/rdf/rdf3.yaml"));
        GZIPPED_WRITER.write(rdf3, new File(BASE_DIR, "test-data/rdf/rdf3.yaml.gz"));

        SimpleEvent<Integer, String> rdf4 =
                createEvent(List.of(new Header("Content-Type", "foo/bar")), 4, SIMPLE_RDF_VALUE);
        Serdes.YAML_INTEGER_STRING.write(rdf4, new File(BASE_DIR, "test-data/rdf/rdf4.yaml"));
        GZIPPED_WRITER.write(rdf4, new File(BASE_DIR, "test-data/rdf/rdf4.yaml.gz"));
    }

    public static void main(String[] args) throws IOException {
        BASE_DIR = new File("event-sources/event-source-file/");
        simple();
        complex();
        noKey();
        noValue();
        bytes();
        rdf();

        for (int i = 1; i <= 4; i++) {
            byte[] raw = Serdes.INTEGER_SERIALIZER.serialize("test", i);
            String base64 = Base64.getEncoder().encodeToString(raw);
            System.out.format("%d: \"%s\"\n", i, base64);
        }
    }
}
