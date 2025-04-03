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
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdfpatch.RDFPatchOps;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.WebContent;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import uk.gov.dbt.ndtp.secure.agent.payloads.RdfPayload;

/**
 * An RDF Payload deserialiser that can cope with both RDF Dataset and RDF Patch format events.  Which format is used is
 * dependent on the {@code Content-Type} header of the event.  If no such header is present then an RDF Dataset is
 * assumed to be the default.
 */
public class RdfPayloadDeserializer extends AbstractRdfSerdes implements Deserializer<RdfPayload> {

    /**
     * A Kafka configuration key that can be used to configure the deserialiser to eagerly parse the raw data from Kafka
     * rather than delaying parsing to later
     */
    public static final String EAGER_PARSING_CONFIG_KEY = "rdf.payload.parsing.eager";
    private boolean eagerParsing = false;
    private final DatasetGraphDeserializer dsgDeserializer;

    /**
     * Creates a new deserializer
     */
    public RdfPayloadDeserializer() {
        this(Lang.NQUADS);
    }

    /**
     * Creates a new deserializer
     *
     * @param defaultLang Default RDF language to use
     */
    public RdfPayloadDeserializer(Lang defaultLang) {
        super(defaultLang);
        this.dsgDeserializer = new DatasetGraphDeserializer(defaultLang);
    }

    private RdfPayload deserializeInternal(String topic, Headers headers, byte[] data) {
        if (data == null) {
            return null;
        }

        String contentType = findContentType(headers);

        // If eager parsing is not in use create a lazy payload that will be deserialised later
        // To ensure it honours our configured default language in this case we inject a Content-Type header if one
        // wasn't explicitly present
        if (!this.eagerParsing) {
            return RdfPayload.of(contentType != null ? contentType :
                                 this.dsgDeserializer.defaultLang.getContentType().getContentTypeStr(), data);
        }

        if (StringUtils.isBlank(contentType)) {
            // Assume RDF Dataset in our default language
            return RdfPayload.of(this.dsgDeserializer.deserialize(topic, headers, data));
        }

        if (StringUtils.equalsIgnoreCase(contentType, WebContent.ctPatch.getContentTypeStr())) {
            return RdfPayload.of(RDFPatchOps.read(new ByteArrayInputStream(data)));
        } else if (StringUtils.equalsIgnoreCase(contentType, WebContent.ctPatchThrift.getContentTypeStr())) {
            return RdfPayload.of(RDFPatchOps.readBinary(new ByteArrayInputStream(data)));
        } else {
            // Assume an RDF Dataset
            return RdfPayload.of(this.dsgDeserializer.deserialize(topic, headers, data));
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        try {
            this.eagerParsing = StringUtils.equalsIgnoreCase((String) configs.get(EAGER_PARSING_CONFIG_KEY),
                                                             Boolean.TRUE.toString());
        } catch (ClassCastException e) {
            this.eagerParsing = false;
        }
    }

    @Override
    public RdfPayload deserialize(String topic, byte[] data) {
        return deserializeInternal(topic, null, data);
    }

    @Override
    public RdfPayload deserialize(String topic, Headers headers, byte[] data) {
        return deserializeInternal(topic, headers, data);
    }
}
