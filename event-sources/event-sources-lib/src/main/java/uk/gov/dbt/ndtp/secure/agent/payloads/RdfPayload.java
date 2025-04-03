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

import java.io.ByteArrayInputStream;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdfpatch.RDFPatch;
import org.apache.jena.rdfpatch.RDFPatchOps;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.WebContent;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import uk.gov.dbt.ndtp.secure.agent.projectors.utils.WriteOnceReference;

/**
 * Represents an RDF Payload, this may either be a purely additive {@link DatasetGraph} or a mutative {@link RDFPatch}
 * containing an ordered sequence of additions and deletions.
 * <p>
 * As of {@code 0.14.0} payloads are lazily deserialised, meaning that they hold the raw RDF value, and only deserialize
 * it when a consumer actually accesses it for the first time via the {@link #getDataset()} or {@link #getPatch()}
 * methods.  This avoids malformed RDF, or RDF events with incorrect {@code Content-Type} headers, causing head of line
 * blocking because it allows for the event to always be read from Kafka.  Consumers need to take note of the new
 * {@link RdfPayloadException} that may be thrown by these methods when they attempt to deserialise an invalid RDF event
 * and handle these appropriately.
 * </p>
 */
public class RdfPayload {

    private static final CharSequence[] RDF_PATCH_CONTENT_TYPES = {
            WebContent.contentTypePatch,
            WebContent.contentTypePatchThrift
    };

    /**
     * Creates a new additive RDF Payload from a Dataset Graph
     *
     * @param dsg Dataset graph
     * @return Payload
     */
    public static RdfPayload of(DatasetGraph dsg) {
        return new RdfPayload(dsg);
    }

    /**
     * Creates a new mutative RDF Payload from an RDF Patch
     *
     * @param patch RDF Patch
     * @return Payload
     */
    public static RdfPayload of(RDFPatch patch) {
        return new RdfPayload(patch);
    }

    /**
     * Creates a new lazily deserialised RDF Payload
     *
     * @param contentType Content Type (if known, if {@code null} then a suitable default will be assumed)
     * @param rawData     Raw RDF payload data for later deserialization
     * @return RDF Payload
     */
    public static RdfPayload of(String contentType, byte[] rawData) {
        return new RdfPayload(contentType, rawData);
    }

    private byte[] rawData;
    private final String contentType;

    private final WriteOnceReference<DatasetGraph> dsg = new WriteOnceReference<>();
    private final WriteOnceReference<RDFPatch> patch = new WriteOnceReference<>();

    /**
     * Creates a lazily deserialised payload
     *
     * @param contentType Content Type (if known, if {@code null} a suitable default is assumed)
     * @param rawData     The raw data for lazy deserialisation
     */
    private RdfPayload(String contentType, byte[] rawData) {
        this.contentType = contentType;
        this.rawData = Objects.requireNonNull(rawData, "Raw RDF Payload Data cannot be null");
    }

    /**
     * Creates a dataset payload
     *
     * @param dsg Dataset
     */
    private RdfPayload(DatasetGraph dsg) {
        this.dsg.set(Objects.requireNonNull(dsg, "Dataset cannot be null"));
        this.contentType = null;
    }

    /**
     * Creates a patch payload
     *
     * @param patch Patch
     */
    private RdfPayload(RDFPatch patch) {
        this.patch.set(Objects.requireNonNull(patch, "Patch cannot be null"));
        this.contentType = null;
    }

    /**
     * Gets whether this payload contains raw data i.e. it has been lazily deserialised and the raw data has yet to be
     * deserialised into an actual data structure.
     * <p>
     * Note that if {@link #getDataset()} or {@link #getPatch()} has already been called and the payload was
     * successfully deserialised then the raw data would have been cleared as a result as it is no longer needed.  Thus
     * this will only be {@code true} if deserialisation was never attempted, or was attempted but failed due to
     * malformed data.
     * </p>
     *
     * @return True if raw data is present, false otherwise
     */
    public boolean hasRawData() {
        return rawData != null;
    }

    /**
     * Gets the raw data, if any
     * <p>
     * See {@link #hasRawData()} for how to check whether raw data is present and notes about the lifecycle of the raw
     * data
     * </p>
     *
     * @return Raw data, or {@code null} if not present
     */
    public byte[] getRawData() {
        return this.rawData;
    }

    /**
     * Gets whether this payload is ready for immediate processing i.e. if it's a lazily deserialised payload has it
     * been deserialised?
     *
     * @return True if ready, false otherwise
     */
    public boolean isReady() {
        return this.dsg.isSet() || this.patch.isSet();
    }

    /**
     * Gets whether this payload contains an RDF Dataset and is thus purely additive
     *
     * @return True if contains a dataset, false otherwise
     */
    public boolean isDataset() {
        return this.dsg.isSet() || (!this.patch.isSet() && !isRdfPatchContentType());
    }

    /**
     * Gets whether this payload contains an RDF Patch and thus may contain sequences of additions and deletions
     *
     * @return True if contains a patch, false otherwise
     */
    public boolean isPatch() {
        return this.patch.isSet() || isRdfPatchContentType();
    }

    /**
     * Gets the dataset for this payload (if any)
     *
     * @return Dataset, or {@code null} if not a dataset payload
     * @throws RdfPayloadException Thrown if the raw data for this payload cannot be parsed into a valid RDF Dataset
     */
    public DatasetGraph getDataset() {
        return this.dsg.computeIfAbsent(() -> {
            // Abort early if the provided Content-Type is for an RDF Patch
            if (isRdfPatchContentType()) {
                return null;
            }

            // Also abort if not a lazy payload, this check is needed because eager Patch payloads will set contentType
            // to null which will fail the preceding content type check allowing flow to drop through to here even
            // though a Patch is present in the payload
            if (this.rawData == null) {
                return null;
            }

            // Otherwise try to deserialise into a DatasetGraph now
            Lang lang = RDFLanguages.contentTypeToLang(contentType);
            Lang selectedLang = lang != null ? lang : Lang.NQUADS;
            try {
                DatasetGraph dsg = DatasetGraphFactory.create();
                RDFParserBuilder.create()
                                .lang(selectedLang)
                                .source(new ByteArrayInputStream(this.rawData))
                                .build()
                                .parse(dsg);
                // NB - We could just call toDatasetGraph() but that creates a much more expensive transactional dataset
                //      graph which tanks performance in our test scenarios

                clearRawData();

                return dsg;
            } catch (JenaException e) {
                throw new RdfPayloadException(String.format(
                        "Failed to deserialise RDF Payload, selected RDF Language '%s' based on Content-Type header '%s', which could not successfully parse the provided RDF data",
                        selectedLang.getName(), contentType), e);
            }
        });
    }

    private void clearRawData() {
        // Once we've successfully deserialised can stop storing the raw bytes
        this.rawData = null;
    }

    private boolean isRdfPatchContentType() {
        return StringUtils.equalsAnyIgnoreCase(contentType, RDF_PATCH_CONTENT_TYPES);
    }

    /**
     * Gets the patch for this payload (if any)
     *
     * @return Patch, or {@code null} if not a patch payload
     * @throws RdfPayloadException Thrown if the raw data for this payload cannot be parsed into a valid RDF Patch
     */
    public RDFPatch getPatch() {
        return this.patch.computeIfAbsent(() -> {
            // Abort early if not an RDF Patch content type
            if (!isRdfPatchContentType()) {
                return null;
            }

            // NB - Don't need a check for null rawData here as the only way for the preceding check to succeed is for
            //      a valid RDF Patch Content-Type to be present so this instance MUST have been created by the
            //      constructor that enforces that rawData is non-null

            // Otherwise try to deserialise now
            try {
                RDFPatch patch = null;
                if (StringUtils.equalsIgnoreCase(contentType, WebContent.contentTypePatch)) {
                    patch = RDFPatchOps.read(new ByteArrayInputStream(this.rawData));
                } else if (StringUtils.equalsIgnoreCase(contentType, WebContent.contentTypePatchThrift)) {
                    patch = RDFPatchOps.readBinary(new ByteArrayInputStream(this.rawData));
                }

                if (patch == null) {
                    // NB - This code is essentially unreachable because we're already checking for all our supported
                    //      patch content types earlier, this serves mainly as a future-proofing should new patch
                    //      serialisations be introduced
                    throw new RdfPayloadException(String.format(
                            "Failed to deserialise RDF Payload, Content-Type '%s' is not a known RDF Patch serialisation",
                            contentType));
                } else {
                    clearRawData();
                    return patch;
                }
            } catch (JenaException e) {
                throw new RdfPayloadException(String.format(
                        "Failed to deserialise RDF Payload, selected RDF Patch based on Content-Type header '%s', which could not successfully parse the provided RDF patch",
                        contentType), e);
            }
        });
    }
}
