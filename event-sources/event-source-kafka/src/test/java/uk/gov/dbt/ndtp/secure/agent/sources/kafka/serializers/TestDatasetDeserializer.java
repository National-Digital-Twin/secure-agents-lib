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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.kafka.common.errors.SerializationException;
import org.mockito.Mockito;
import org.testng.annotations.Test;

@Test
public class TestDatasetDeserializer extends AbstractRdfDeserializerTests<DatasetGraph> {
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
                                     NodeFactory.createLiteral(Integer.toString(i))));
            }
        }

        return dataset;
    }

    @Override
    protected AbstractRdfDeserializer<DatasetGraph> createDeserializer() {
        return new DatasetGraphDeserializer();
    }

    @Override
    protected Lang getDefaultLanguage() {
        return Lang.NQUADS;
    }

    @Test(expectedExceptions = SerializationException.class)
    public void givenBrokenDataset_whenSerializing_thenErrors() {
        // Given
        DatasetGraphSerializer serializer = new DatasetGraphSerializer();
        DatasetGraph dsg = Mockito.mock(DatasetGraph.class);

        // When and Then
        serializer.serialize("test", dsg);
    }
}
