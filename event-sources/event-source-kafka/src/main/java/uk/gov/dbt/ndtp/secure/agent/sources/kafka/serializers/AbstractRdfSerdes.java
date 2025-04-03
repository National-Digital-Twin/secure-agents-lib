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

import java.nio.charset.StandardCharsets;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.web.HttpNames;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

/**
 * Abstract base class for RDF serializers and deserializers
 */
public class AbstractRdfSerdes {
    /**
     * The default RDF language to use for (de)serialization if an event doesn't declare a specific language via its
     * content type header
     */
    protected final Lang defaultLang;

    /**
     * Creates a new serdes
     *
     * @param defaultLang Default RDF language to use
     */
    public AbstractRdfSerdes(Lang defaultLang) {
        this.defaultLang = defaultLang;
    }

    /**
     * Gets the configured default RDF language for the serdes
     *
     * @return Default RDF Language
     */
    Lang getDefaultLang() {
        return this.defaultLang;
    }

    /**
     * Finds the value of the first Content Type header (if any)
     *
     * @param headers Kafka headers
     * @return Content Type, or {@code null} if no such header
     */
    protected String findContentType(Headers headers) {
        if (headers == null) {
            return null;
        }
        Header header = headers.lastHeader(HttpNames.hContentType);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }
}
