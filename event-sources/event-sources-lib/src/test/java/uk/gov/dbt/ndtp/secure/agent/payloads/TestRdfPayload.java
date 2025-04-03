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
package uk.gov.dbt.ndtp.secure.agent.payloads;

import java.nio.charset.StandardCharsets;
import org.apache.jena.rdfpatch.RDFPatchOps;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRdfPayload {

    @Test
    public void givenEagerDatasetPayload_whenAccessingMethods_thenReturnValuesAreCorrect() {
        // Given
        RdfPayload payload = RdfPayload.of(DatasetGraphFactory.empty());

        // When and Then
        Assert.assertTrue(payload.isDataset());
        Assert.assertFalse(payload.isPatch());
        Assert.assertTrue(payload.isReady());
        Assert.assertNotNull(payload.getDataset());
        Assert.assertNull(payload.getPatch());
    }

    @Test
    public void givenEagerPatchPayload_whenAccessingMethods_thenReturnValuesAreCorrect() {
        // Given
        RdfPayload payload = RdfPayload.of(RDFPatchOps.emptyPatch());

        // When and Then
        Assert.assertFalse(payload.isDataset());
        Assert.assertTrue(payload.isPatch());
        Assert.assertTrue(payload.isReady());
        Assert.assertNull(payload.getDataset());
        Assert.assertNotNull(payload.getPatch());
    }

    @Test
    public void givenLazyDatasetPayload_whenAccessingMethods_thenReturnValuesAreCorrect() {
        // Given
        RdfPayload payload = RdfPayload.of(null, new byte[0]);

        // When and Then
        Assert.assertTrue(payload.isDataset());
        Assert.assertFalse(payload.isPatch());
        Assert.assertFalse(payload.isReady());
        Assert.assertNotNull(payload.getDataset());
        Assert.assertNull(payload.getPatch());
        Assert.assertTrue(payload.isReady());
    }

    @Test
    public void givenLazyPatchPayload_whenAccessingMethods_thenReturnValuesAreCorrect() {
        // Given
        RdfPayload payload = RdfPayload.of(WebContent.contentTypePatch, new byte[0]);

        // When and Then
        Assert.assertFalse(payload.isDataset());
        Assert.assertTrue(payload.isPatch());
        Assert.assertFalse(payload.isReady());
        Assert.assertNull(payload.getDataset());
        Assert.assertNotNull(payload.getPatch());
        Assert.assertTrue(payload.isReady());
    }

    @Test(expectedExceptions = RdfPayloadException.class, expectedExceptionsMessageRegExp = "Failed to deserialise.*")
    public void givenLazyInvalidDatasetPayload_whenAccessingDataset_thenErrorIsThrown() {
        // Given
        RdfPayload payload = RdfPayload.of(null, "junk".getBytes(StandardCharsets.UTF_8));

        // When and Then
        Assert.assertTrue(payload.isDataset());
        Assert.assertFalse(payload.isReady());
        payload.getDataset();
    }

    @Test(expectedExceptions = RdfPayloadException.class, expectedExceptionsMessageRegExp = "Failed to deserialise.*")
    public void givenLazyInvalidDatasetPayloadWithWrongContentType_whenAccessingDataset_thenErrorIsThrown() {
        // Given
        RdfPayload payload = RdfPayload.of(WebContent.contentTypeRDFXML,
                                           "<http://s> <http://p> <http://o> .".getBytes(StandardCharsets.UTF_8));

        // When and Then
        Assert.assertTrue(payload.isDataset());
        Assert.assertFalse(payload.isReady());
        payload.getDataset();
    }

    @Test(expectedExceptions = RdfPayloadException.class, expectedExceptionsMessageRegExp = "Failed to deserialise.*")
    public void givenLazyInvalidPatchPayload_whenAccessingPatch_thenErrorIsThrown() {
        // Given
        RdfPayload payload = RdfPayload.of(WebContent.contentTypePatch, "junk".getBytes(StandardCharsets.UTF_8));

        // When and Then
        Assert.assertTrue(payload.isPatch());
        Assert.assertFalse(payload.isReady());
        payload.getPatch();
    }
    @Test//(expectedExceptions = RdfPayloadException.class, expectedExceptionsMessageRegExp = "Failed to deserialise.*")
    public void givenLazyInvalidPatchPayloadWithBinaryContentType_whenAccessingPatch_thenErrorIsThrown() {
        // Given
        RdfPayload payload = RdfPayload.of(WebContent.contentTypePatchThrift, "junk".getBytes(StandardCharsets.UTF_8));

        // When and Then
        Assert.assertTrue(payload.isPatch());
        Assert.assertFalse(payload.isReady());
        // BUG - Really Jena should throw an exception in this case but currently it doesn't and just returns an empty
        //       patch instead
        payload.getPatch();
    }

}
