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

import java.io.ByteArrayInputStream;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Abstract base class for RDF deserializers
 * <p>
 * This implements common logic around allowing the deserializer to have a default language.  Also it honours a
 * {@code Content-Type} header in the headers of a message to automatically select the correct RDF parser, thus allowing
 * it to cope with topics that contain RDF serialized in different RDF formats.
 * </p>
 *
 * @param <T> Type of the RDF Data structure that is deserialized
 */
public abstract class AbstractRdfDeserializer<T> extends AbstractRdfSerdes implements Deserializer<T> {

    /**
     * Creates a new RDF deserializer
     *
     * @param defaultLang Default RDF Language to use
     */
    public AbstractRdfDeserializer(Lang defaultLang) {
        super(defaultLang);
        Objects.requireNonNull(defaultLang, "defaultLang cannot be null");
    }

    /**
     * Builds an RDF parser for the given data assuming this deserializers configured default language
     *
     * @param data Data
     * @return RDF parser
     */
    protected RDFParser buildParser(byte[] data) {
        return buildParser(this.defaultLang, data);
    }

    /**
     * Builds an RDF parser for the given RDF language and data
     *
     * @param lang RDF Language
     * @param data Data
     * @return RDF parser
     */
    protected RDFParser buildParser(Lang lang, byte[] data) {
        return RDFParserBuilder.create()
                               .lang(lang != null ? lang : this.defaultLang)
                               .source(new ByteArrayInputStream(data))
                               .build();
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        RDFParser parser = buildParser(data);
        return deserializeInternal(parser);
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) {
            return null;
        }

        String contentType = findContentType(headers);

        // No explicit Content-Type header(s) defined so just fallback to deserializing with our default language
        if (StringUtils.isBlank(contentType)) {
            return deserialize(topic, data);
        }

        // Explicit Content-Type headers(s) so deserialize with the appropriate language
        RDFParser parser = buildParser(RDFLanguages.contentTypeToLang(contentType), data);
        return deserializeInternal(parser);
    }

    /**
     * Deserializes using the given RDF Parser that has been built based upon the deserializer configuration and Kafka
     * headers.
     * <p>
     * The built parser already includes the bytes to be deserialized as part of its configuration so derived
     * implementations should merely need to invoke the appropriate {@code parse()} method on it e.g.
     * {@link RDFParser#parse(Graph)}.
     * </p>
     *
     * @param parser RDF Parser to use
     * @return Deserialized RDF
     */
    protected abstract T deserializeInternal(RDFParser parser);

}
