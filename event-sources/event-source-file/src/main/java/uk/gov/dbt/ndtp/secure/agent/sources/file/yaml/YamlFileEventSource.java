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
package uk.gov.dbt.ndtp.secure.agent.sources.file.yaml;

import java.io.File;
import java.io.FileFilter;
import org.apache.kafka.common.serialization.Deserializer;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventReaderWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.file.NumericFilenameComparator;
import uk.gov.dbt.ndtp.secure.agent.sources.file.NumericallyNamedWithExtensionFilter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.gzip.GZipEventReaderWriter;

/**
 * A file event source where the files are encoded with YAML
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class YamlFileEventSource<TKey, TValue> extends FileEventSource<TKey, TValue> {
    /**
     * The default file filter used to select YAML event files
     */
    public static final FileFilter YAML_FILTER = new NumericallyNamedWithExtensionFilter(".yaml");

    /**
     * The default filter used to select GZipped YAML event files
     */
    public static final FileFilter YAML_GZIPPED_FILTER = new NumericallyNamedWithExtensionFilter(".yaml.gz");

    /**
     * Creates a new file event source
     *
     * @param sourceDir         Source directory containing the events
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     */
    public YamlFileEventSource(File sourceDir, Deserializer<TKey> keyDeserializer,
                               Deserializer<TValue> valueDeserializer) {
        this(sourceDir, false, keyDeserializer, valueDeserializer);
    }

    /**
     * Creates a new file event source
     *
     * @param sourceDir         Source directory containing the events
     * @param gzip              Whether the event files are GZipped
     * @param keyDeserializer   Key deserializer
     * @param valueDeserializer Value deserializer
     */
    public YamlFileEventSource(File sourceDir, boolean gzip, Deserializer<TKey> keyDeserializer,
                               Deserializer<TValue> valueDeserializer) {
        super(sourceDir, selectFilter(gzip), new NumericFilenameComparator(),
              selectReader(gzip, keyDeserializer, valueDeserializer));
    }

    private static FileFilter selectFilter(boolean gzip) {
        return gzip ? YAML_GZIPPED_FILTER : YAML_FILTER;
    }

    private static <TKey, TValue> FileEventReaderWriter<TKey, TValue> selectReader(boolean gzip,
                                                                                   Deserializer<TKey> keyDeserializer,
                                                                                   Deserializer<TValue> valueDeserializer) {
        return gzip ? new GZipEventReaderWriter<>(new YamlEventReaderWriter<>(keyDeserializer, valueDeserializer)) :
               new YamlEventReaderWriter<>(keyDeserializer, valueDeserializer);
    }

}
