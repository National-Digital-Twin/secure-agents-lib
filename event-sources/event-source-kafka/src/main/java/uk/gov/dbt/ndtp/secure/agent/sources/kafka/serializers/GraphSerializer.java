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
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriterBuilder;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

/**
 * A Kafka serializer for RDF Graphs
 */
public class GraphSerializer extends AbstractRdfSerdes implements Serializer<Graph> {
    /**
     * Creates a new serializer that uses NTriples as the output serialization
     */
    public GraphSerializer() {
        this(RDFLanguages.NTRIPLES);
    }

    /**
     * Creates a new serializer that uses the given RDF Language as the output serialization
     *
     * @param defaultLang Default language
     */
    public GraphSerializer(Lang defaultLang) {
        super(defaultLang);
    }

    /**
     * Prepares an RDF writer
     *
     * @param lang Language to use for the output serialization, if {@code null} defaults to the default language
     * @return RDF writer
     */
    protected RDFWriterBuilder prepareWriter(Lang lang) {
        return RDFWriterBuilder.create().lang(lang != null ? lang : this.defaultLang);
    }

    @Override
    public byte[] serialize(String topic, Graph data) {
        if (data == null) {
            return new byte[0];
        }
        return serializeInternal(data, null);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Graph data) {
        if (data == null) {
            return new byte[0];
        }

        String contentType = findContentType(headers);
        Lang lang = StringUtils.isNotBlank(contentType) ? RDFLanguages.contentTypeToLang(contentType) : null;
        return serializeInternal(data, lang);
    }

    /**
     * Serializes the dataset graph to a byte sequence
     *
     * @param data Dataset graph
     * @param lang Language, if {@code null} then the serializers default language is used
     * @return Serialized dataset graph
     */
    protected byte[] serializeInternal(Graph data, Lang lang) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        prepareWriter(lang).source(data).output(output);
        return output.toByteArray();
    }
}
