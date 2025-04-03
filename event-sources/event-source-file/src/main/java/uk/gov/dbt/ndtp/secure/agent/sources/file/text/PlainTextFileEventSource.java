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
package uk.gov.dbt.ndtp.secure.agent.sources.file.text;

import java.io.File;
import java.io.FileFilter;
import org.apache.kafka.common.serialization.Deserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.file.NumericFilenameComparator;
import uk.gov.dbt.ndtp.secure.agent.sources.file.NumericallyNamedWithExtensionFilter;

/**
 * A file event source where the events are plain text encoded, see {@link PlainTextEventReaderWriter} for more detail
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class PlainTextFileEventSource<TKey, TValue> extends FileEventSource<TKey, TValue> {
    /**
     * The default file filter used to select plain text event files
     */
    public static final FileFilter PLAINTEXT_FILTER = new NumericallyNamedWithExtensionFilter(".txt");

    /**
     * Creates a new file event source
     *
     * @param sourceDir         Source directory containing the events
     * @param valueDeserializer Value deserializer
     */
    public PlainTextFileEventSource(File sourceDir, Deserializer<TValue> valueDeserializer) {
        super(sourceDir, PLAINTEXT_FILTER, new NumericFilenameComparator(),
              new PlainTextEventReaderWriter<>(valueDeserializer));
    }
}
