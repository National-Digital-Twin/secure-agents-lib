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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka.serializers;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.impl.GraphMatcher;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RDFWriterBuilder;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.kafka.common.header.Headers;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.TestKafkaEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.Utils;

public class TestGraphDeserializer extends AbstractRdfDeserializerTests<Graph> {

    @Override
    protected AbstractRdfDeserializer<Graph> createDeserializer() {
        return new GraphDeserializer();
    }

    @Override
    protected Lang getDefaultLanguage() {
        return Lang.NTRIPLES;
    }

    @Test
    public void graph_deserialize_null_01() {
        try (GraphDeserializer deserializer = new GraphDeserializer()) {
            Graph g = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, null);
            Assert.assertNull(g);
        }
    }

    @Test
    public void graph_deserialize_null_02() {
        try (GraphDeserializer deserializer = new GraphDeserializer()) {
            Graph g = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, null, (byte[])null);
            Assert.assertNull(g);
        }
    }

    public static Graph createTestGraph(int size) {
        Graph g = GraphFactory.createDefaultGraph();
        for (int i = 0; i < size; i++) {
            g.add(NodeFactory.createURI("urn:subjects:" + i), NodeFactory.createURI("urn:predicate"),
                  NodeFactory.createLiteralString(Integer.toString(i)));
        }
        return g;
    }

    private byte[] graphToBytes(Graph g, Lang lang) {
        RDFWriter writer = RDFWriterBuilder.create().source(g).lang(lang).build();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.output(output);
        return output.toByteArray();
    }

    @DataProvider(name = "graph-sizes")
    private Object[][] getGraphSizes() {
        return new Object[][] {
                { 100 },
                { 10_000 },
                { 100_000 }
        };
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_01(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.NTRIPLES);

        try (GraphDeserializer deserializer = new GraphDeserializer()) {
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, data);
            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_02(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.TURTLE);

        try (GraphDeserializer deserializer = new GraphDeserializer(Lang.TURTLE)) {
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, data);
            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_03(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.TURTLE);

        try (GraphDeserializer deserializer = new GraphDeserializer()) {
            Headers headers = Utils.createMockHeaders(
                    Map.of(HttpNames.hContentType, Lang.TURTLE.getContentType().getContentTypeStr()));
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, headers, data);

            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_04(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.NTRIPLES);

        try (GraphDeserializer deserializer = new GraphDeserializer()) {
            Headers headers = Utils.createMockHeaders(Map.of("foo", "bar"));
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, headers, data);

            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_05(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.NTRIPLES);

        try (GraphDeserializer deserializer = new GraphDeserializer()) {
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, null, data);

            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_06(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.NTRIPLES);

        try (GraphDeserializer deserializer = new GraphDeserializer()) {
            Map<String, String> headerData = new HashMap<>();
            headerData.put(HttpNames.hContentType, null);
            Headers headers = Utils.createMockHeaders(headerData);
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, headers, data);

            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_07(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.RDFTHRIFT);

        try (GraphDeserializer deserializer = new GraphDeserializer(Lang.RDFTHRIFT)) {
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, data);

            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes")
    public void graph_deserialize_08(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.RDFPROTO);

        try (GraphDeserializer deserializer = new GraphDeserializer(Lang.RDFPROTO)) {
            Graph output = deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, data);

            Assert.assertTrue(GraphMatcher.equals(input, output), "Expected graphs to be equal");
        }
    }

    @Test(dataProvider = "graph-sizes", expectedExceptions = RiotException.class)
    public void graph_deserialize_bad_01(int size) {
        Graph input = createTestGraph(size);
        byte[] data = graphToBytes(input, Lang.NTRIPLES);

        try (GraphDeserializer deserializer = new GraphDeserializer(Lang.RDFXML)) {
            deserializer.deserialize(TestKafkaEventSource.TEST_TOPIC, data);
        }
    }
}
