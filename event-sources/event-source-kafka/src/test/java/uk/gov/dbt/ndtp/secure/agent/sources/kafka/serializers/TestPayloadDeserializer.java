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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdfpatch.RDFPatch;
import org.apache.jena.rdfpatch.RDFPatchOps;
import org.apache.jena.rdfpatch.changes.RDFChangesCollector;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.IsoMatcher;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.payloads.RdfPayload;

public class TestPayloadDeserializer {

    private static final byte[] JUNK_DATA = "junk".getBytes(StandardCharsets.UTF_8);

    public static DatasetGraph createTestDataset(int numGraphs, int size) {
        DatasetGraph dataset = DatasetGraphFactory.create();
        if (numGraphs <= 0) {
            return dataset;
        }

        for (int g = 0; g < numGraphs; g++) {
            Node graphName = g == 0 ? Quad.defaultGraphIRI : NodeFactory.createURI("urn:graphs:" + g);
            for (int i = 0; i < size; i++) {
                dataset.add(new Quad(graphName, NodeFactory.createURI("urn:subjects:" + i),
                                     NodeFactory.createURI("urn:predicate"),
                                     NodeFactory.createLiteralDT(Integer.toString(i), XSDDatatype.XSDinteger)));
            }
        }

        return dataset;
    }

    public static RDFPatch datasetToPatch(DatasetGraph dsg) {
        RDFChangesCollector collector = new RDFChangesCollector();
        collector.start();
        for (Map.Entry<String, String> prefix : dsg.prefixes().getMapping().entrySet()) {
            collector.addPrefix(Quad.defaultGraphIRI, prefix.getKey(), prefix.getValue());
        }
        Iterator<Quad> quads = dsg.find();
        while (quads.hasNext()) {
            Quad q = quads.next();
            collector.add(q.getGraph(), q.getSubject(), q.getPredicate(), q.getObject());
        }
        collector.finish();

        return collector.getRDFPatch();
    }

    @Test
    public void givenPayloadDeserializer_whenDeserializingNullData_thenNullIsReturned() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            // When and Then
            Assert.assertNull(deserializer.deserialize("test", null));
        }
    }

    /**
     * Gets whether the {@link RdfPayloadDeserializer} returned by {@link #createPayloadDeserializer()} is configured
     * for eager parsing
     *
     * @return True if eager parsing is configured, false otherwise
     */
    protected boolean isEagerParsing() {
        return false;
    }

    /**
     * Creates a payload deserializer for use in the tests
     *
     * @return Payload deserializer
     */
    protected RdfPayloadDeserializer createPayloadDeserializer() {
        return new RdfPayloadDeserializer();
    }

    @Test
    public void givenPayloadDeserializer_whenSerializingAndDeserializingEmptyBytes_thenEmptyDataset() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            try (DatasetGraphSerializer serializer = new DatasetGraphSerializer()) {
                // When
                byte[] data = serializer.serialize("test", null);
                RdfPayload payload = deserializer.deserialize("test", data);

                // Then
                Assert.assertNotNull(payload);
                Assert.assertTrue(payload.isDataset());
                Assert.assertEquals(Iter.count(payload.getDataset().find()), 0);
            }
        }
    }

    @Test
    public void givenPayloadDeserializer_whenSerializingAndDeserializingEmptyBytesAndHeaders_thenEmptyDataset() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            try (DatasetGraphSerializer serializer = new DatasetGraphSerializer()) {
                // When
                byte[] data = serializer.serialize("test", null, null);
                RdfPayload payload = deserializer.deserialize("test", data);

                // Then
                Assert.assertNotNull(payload);
                Assert.assertTrue(payload.isDataset());
                Assert.assertEquals(Iter.count(payload.getDataset().find()), 0);
            }
        }
    }

    @Test
    public void givenPayloadSerializer_whenSerializingNullPayload_thenEmptyBytes() {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            // When
            byte[] data = serializer.serialize("test", null);

            // Then
            Assert.assertEquals(data.length, 0);
        }
    }

    @Test
    public void givenPayloadSerializer_whenSerializingNullPayloadAndHeaders_thenEmptyBytes() {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            // When
            byte[] data = serializer.serialize("test", new RecordHeaders(), null);

            // Then
            Assert.assertEquals(data.length, 0);
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void givenPayloadDeserializer_whenDeserializingMalformedPayload_thenFails() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            byte[] junkData = JUNK_DATA;

            // When
            RdfPayload payload = deserializer.deserialize("test", junkData);

            // Then
            verifyParsingFailed(payload);
        }
    }

    private void verifyParsingFailed(RdfPayload payload) {
        if (this.isEagerParsing()) {
            Assert.fail("Should have eagerly thrown error during deserialization");
        } else {
            // If lazy parsing then error happens when we try and access the payload
            if (payload.isDataset()) {
                payload.getDataset();
            } else {
                payload.getPatch();
            }
        }
    }

    @Test
    public void givenPayloadDeserializer_whenDeserializingADataset_thenCorrectDatasetIsReturned() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            DatasetGraph dsg = createTestDataset(10, 100);
            try (DatasetGraphSerializer serializer = new DatasetGraphSerializer()) {
                // When
                byte[] data = serializer.serialize("test", dsg);
                RdfPayload payload = deserializer.deserialize("test", data);

                // Then
                Assert.assertNotNull(payload);
                Assert.assertTrue(payload.isDataset());
                Assert.assertTrue(IsoMatcher.isomorphic(dsg, payload.getDataset()));
                verifyRoundTrip(payload, null);
            }
        }
    }

    @Test
    public void givenPayloadDeserializer_whenDeserializingADatasetWithNullHeaders_thenCorrectDatasetIsReturned() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            DatasetGraph dsg = createTestDataset(10, 100);
            try (DatasetGraphSerializer serializer = new DatasetGraphSerializer()) {
                // When
                byte[] data = serializer.serialize("test", null, dsg);
                RdfPayload payload = deserializer.deserialize("test", data);

                // Then
                Assert.assertNotNull(payload);
                Assert.assertTrue(payload.isDataset());
                Assert.assertTrue(IsoMatcher.isomorphic(dsg, payload.getDataset()));
                verifyRoundTrip(payload, null);
            }
        }
    }

    @Test
    public void givenPayloadDeserializer_whenDeserializingADatasetWithContentTypeHeader_thenCorrectDatasetIsReturned() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            DatasetGraph dsg = createTestDataset(10, 100);
            Headers headers = new RecordHeaders().add(HttpNames.hContentType, Lang.TRIG.getContentType()
                                                                                       .getContentTypeStr()
                                                                                       .getBytes(
                                                                                               StandardCharsets.UTF_8));
            try (DatasetGraphSerializer serializer = new DatasetGraphSerializer()) {
                // When
                byte[] data = serializer.serialize("test", headers, dsg);
                RdfPayload payload = deserializer.deserialize("test", headers, data);

                // Then
                Assert.assertNotNull(payload);
                Assert.assertTrue(payload.isDataset());
                Assert.assertTrue(IsoMatcher.isomorphic(dsg, payload.getDataset()));
                verifyRoundTrip(payload, Lang.TRIG.getContentType().getContentTypeStr());
            }
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void givenPayloadDeserializer_whenDeserializingADatasetWithIncorrectContentTypeHeader_thenFails() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            DatasetGraph dsg = createTestDataset(10, 100);
            Headers headers = new RecordHeaders().add(HttpNames.hContentType, Lang.TRIG.getContentType()
                                                                                       .getContentTypeStr()
                                                                                       .getBytes(
                                                                                               StandardCharsets.UTF_8));
            try (DatasetGraphSerializer serializer = new DatasetGraphSerializer()) {
                // When
                byte[] data = serializer.serialize("test", dsg);
                RdfPayload payload = deserializer.deserialize("test", headers, data);

                // Then
                verifyParsingFailed(payload);
            }
        }
    }

    @Test
    public void givenPayloadDeserializer_whenDeserializingADatasetWithUnrecognisedContentType_thenCorrectDatasetIsReturned() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            DatasetGraph dsg = createTestDataset(10, 100);

            // For unrecognised content-type we fall back to default language
            Headers headers = new RecordHeaders().add(HttpNames.hContentType,
                                                      "random/content-type".getBytes(StandardCharsets.UTF_8));
            try (DatasetGraphSerializer serializer = new DatasetGraphSerializer()) {
                // When
                byte[] data = serializer.serialize("test", headers, dsg);
                RdfPayload payload = deserializer.deserialize("test", headers, data);

                // Then
                Assert.assertNotNull(payload);
                Assert.assertTrue(payload.isDataset());
                Assert.assertTrue(IsoMatcher.isomorphic(dsg, payload.getDataset()));
                verifyRoundTrip(payload, "random/content-type");
            }
        }
    }

    @Test
    public void givenPayloadDeserializer_whenDeserializingPatch_thenCorrectPatchIsReturned() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            DatasetGraph dsg = createTestDataset(10, 100);
            RDFPatch patch = datasetToPatch(dsg);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            RDFPatchOps.write(output, patch);

            // When
            RdfPayload payload = deserializer.deserialize("test", new RecordHeaders().add(HttpNames.hContentType,
                                                                                          WebContent.contentTypePatch.getBytes(
                                                                                                  StandardCharsets.UTF_8)),
                                                          output.toByteArray());

            // Then
            Assert.assertNotNull(payload);
            Assert.assertTrue(payload.isPatch());
            verifyEquivalentPatches(patch, payload.getPatch());
            verifyRoundTrip(payload, WebContent.ctPatch.getContentTypeStr());
        }
    }

    @Test
    public void givenPayloadDeserializer_whenDeserializingBinaryPatch_thenCorrectPatchIsReturned() {
        // Given
        try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
            DatasetGraph dsg = createTestDataset(10, 100);
            RDFPatch patch = datasetToPatch(dsg);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            RDFPatchOps.writeBinary(output, patch);

            // When
            RdfPayload payload = deserializer.deserialize("test", new RecordHeaders().add(HttpNames.hContentType,
                                                                                          WebContent.contentTypePatchThrift.getBytes(
                                                                                                  StandardCharsets.UTF_8)),
                                                          output.toByteArray());

            // Then
            Assert.assertNotNull(payload);
            Assert.assertTrue(payload.isPatch());
            verifyEquivalentPatches(patch, payload.getPatch());
            verifyRoundTrip(payload, WebContent.ctPatchThrift.getContentTypeStr());
        }
    }

    @Test(expectedExceptions = SerializationException.class, expectedExceptionsMessageRegExp = ".*suitable Content-Type Header")
    public void givenPayloadSerializer_whenSerializingPatchWithoutContentType_thenFails() {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            // When and Then
            serializer.serialize("test", RdfPayload.of(RDFPatchOps.emptyPatch()));
        }
    }

    @Test(expectedExceptions = SerializationException.class, expectedExceptionsMessageRegExp = ".*suitable Content-Type Header")
    public void givenPayloadSerializer_whenSerializingPatchWithHeadersButWithoutContentType_thenFails() {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            // When and Then
            serializer.serialize("test", new RecordHeaders(), RdfPayload.of(RDFPatchOps.emptyPatch()));
        }
    }

    @Test
    public void givenPayloadSerializer_whenSerializingLazyPayload_thenSucceeds() {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            RdfPayload payload = RdfPayload.of(WebContent.contentTypeTurtle, new byte[0]);

            // When
            byte[] serialized = serializer.serialize("test", payload);

            // Then
            Assert.assertNotNull(serialized);
            Assert.assertEquals(serialized.length, 0);
        }
    }

    @Test
    public void givenPayloadSerializer_whenSerializingLazyMalformedPayload_thenSucceeds() {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            RdfPayload payload = RdfPayload.of(WebContent.contentTypeTurtle, JUNK_DATA);

            // When
            byte[] serialized = serializer.serialize("test", payload);

            // Then
            Assert.assertNotNull(serialized);
            Assert.assertEquals(serialized.length, JUNK_DATA.length);
            Assert.assertEquals(serialized, JUNK_DATA);
        }
    }

    @Test(expectedExceptions = SerializationException.class)
    public void givenPayloadSerializer_whenSerializingBrokenPayload_thenErrors() {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            RdfPayload payload = RdfPayload.of(Mockito.mock(DatasetGraph.class));

            // When and Then
            serializer.serialize("test", payload);
        }
    }

    private void verifyEquivalentPatches(RDFPatch expected, RDFPatch actual) {
        // Then
        DatasetGraph dsgExpected = DatasetGraphFactory.create();
        RDFPatchOps.applyChange(dsgExpected, expected);
        DatasetGraph dsgActual = DatasetGraphFactory.create();
        RDFPatchOps.applyChange(dsgActual, actual);

        Assert.assertTrue(IsoMatcher.isomorphic(dsgExpected, dsgActual));
    }

    private void verifyRoundTrip(RdfPayload payload, String contentType) {
        // Given
        try (RdfPayloadSerializer serializer = new RdfPayloadSerializer()) {
            Headers headers = new RecordHeaders();
            if (StringUtils.isNotBlank(contentType)) {
                headers.add(HttpNames.hContentType, contentType.getBytes(StandardCharsets.UTF_8));
            }

            // When
            byte[] data = serializer.serialize("test", headers, payload);
            try (RdfPayloadDeserializer deserializer = createPayloadDeserializer()) {
                RdfPayload retrieved = deserializer.deserialize("test", headers, data);

                // Then
                verifySamePayload(payload, retrieved);
                if (StringUtils.isBlank(contentType)) {
                    data = serializer.serialize("test", payload);
                    retrieved = deserializer.deserialize("test", data);
                    verifySamePayload(payload, retrieved);
                }
            }
        }
    }

    private void verifySamePayload(RdfPayload expected, RdfPayload actual) {
        // Then
        Assert.assertEquals(expected.isDataset(), actual.isDataset());
        Assert.assertEquals(expected.isPatch(), actual.isPatch());

        if (expected.isDataset()) {
            Assert.assertTrue(IsoMatcher.isomorphic(expected.getDataset(), actual.getDataset()));
        } else {
            verifyEquivalentPatches(expected.getPatch(), actual.getPatch());
        }
    }
}
