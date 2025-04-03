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

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * A Kafka deserializer that deserializes RDF Graphs represented using Jena's {@link Graph} class
 * <p>
 * This will use either the default language ({@link Lang#NTRIPLES}, or the language identified by the
 * {@code Content-Type} header from the event headers, to try and parse the event key/value as an RDF graph.
 * </p>
 */
public class GraphDeserializer extends AbstractRdfDeserializer<Graph> {

    /**
     * Creates a new deserializer that defaults to {@link Lang#NTRIPLES}
     */
    public GraphDeserializer() {
        this(Lang.NTRIPLES);
    }

    /**
     * Creates a new deserializer with the given default language
     * <p>
     * This is declared as package private so that the default language can only be changed by explicitly deriving a
     * sub-class from this implementation, or in unit test cases.
     * </p>
     *
     * @param defaultLang Default language
     */
    GraphDeserializer(Lang defaultLang) {
        super(defaultLang);
    }

    @Override
    protected Graph deserializeInternal(RDFParser parser) {
        Graph g = GraphFactory.createDefaultGraph();
        parser.parse(g);
        return g;
    }

}
