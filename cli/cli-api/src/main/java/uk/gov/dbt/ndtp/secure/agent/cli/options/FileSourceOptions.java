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
package uk.gov.dbt.ndtp.secure.agent.cli.options;

import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Directory;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import uk.gov.dbt.ndtp.secure.agent.cli.restrictions.AllowedEventFileFormats;
import uk.gov.dbt.ndtp.secure.agent.cli.restrictions.SourceRequired;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events.file.EventCapturingSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormatProvider;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventFormats;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlFormat;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options that provide file based event sources
 */
public class FileSourceOptions<TKey, TValue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSourceOptions.class);

    @Option(name = {
            "--source-dir",
            "--source-directory"
    }, title = "SourceDirectory", description = "Specifies a directory from which events should be read.  This is intended for use as an alternative to Kafka for integration testing.")
    @SourceRequired(name = "Directory")
    @Directory(mustExist = true)
    private File sourceDirectory = null;

    @Option(name = { "--source-file" }, title = "SourceFile", description = "Specifies a single file to treat as an event source.  This is intended for use as an alternative to Kafka for integration testing.")
    @SourceRequired(name = "File")
    @com.github.rvesse.airline.annotations.restrictions.File(mustExist = true)
    private File sourceFile = null;

    @Option(name = { "--source-format" }, title = "SourceFormat", description = "Specifies the file format for the file event source, defaults to yaml.  Only relevant when a file event source is used via the --source-directory option")
    @AllowedEventFileFormats
    private String sourceFormat = YamlFormat.EVENT_FORMAT_NAME;

    @Option(name = {
            "--capture-dir", "--capture-directory"
    }, title = "CaptureDirectory", description = "Specifies a directory to which events should be captured, this will allow them to later be replayed via the --source-directory option.")
    private File captureDirectory = null;

    @Option(name = {
            "--capture-format"
    }, title = "CaptureFormat", description = "Specifies the file format for event capture, defaults to yaml.  Only relevant when event capture has been enabled via the --capture-dir option.")
    @AllowedEventFileFormats
    private String captureFormat = YamlFormat.EVENT_FORMAT_NAME;

    private boolean usingFileCapture = false;

    /**
     * Gets whether a file based event source should be used
     *
     * @return True if a file based source should be used, false otherwise
     */
    public boolean useFileSource() {
        return this.sourceDirectory != null || this.sourceFile != null;
    }

    /**
     * Gets the file based event source to use
     *
     * @param keyDeserializer   Key deserializer, may be needed depending on the capture format
     * @param valueDeserializer Value deserializer, may be needed depending on the capture format
     * @return File event source
     */
    public EventSource<TKey, TValue> getFileSource(
            Deserializer<TKey> keyDeserializer,
            Deserializer<TValue> valueDeserializer) {
        if (this.sourceDirectory != null) {
            LOGGER.info("Replaying events from directory based event source {}",
                        this.sourceDirectory.getAbsolutePath());
        } else {
            LOGGER.info("Replaying events from single file event source {}", this.sourceFile.getAbsolutePath());
        }
        FileEventFormatProvider format = FileEventFormats.get(this.sourceFormat);
        if (format == null) {
            throw new IllegalArgumentException(
                    String.format("Source format '%s' is not a valid event file format", this.sourceFormat));
        }
        if (this.sourceDirectory != null) {
            return format.createSource(keyDeserializer, valueDeserializer, this.sourceDirectory);
        } else {
            return format.createSingleFileSource(keyDeserializer, valueDeserializer, this.sourceFile);
        }
    }

    /**
     * Gets whether file capture has been enabled
     *
     * @return True if enabled (and the capture sink has been created), false otherwise
     */
    public boolean usingFileCapture() {
        return this.usingFileCapture;
    }

    /**
     * Gets the absolute path of the file event capture directory that is in use
     *
     * @return Capture directory
     */
    public String getCaptureDirectory() {
        return this.captureDirectory != null ? this.captureDirectory.getAbsolutePath() : null;
    }

    /**
     * Gets the capture sink to use (if enabled)
     *
     * @param keySerializer              Key serializer to use, may be needed depending on the capture format
     * @param valueSerializer            Value serializer to use, may be needed depending on the capture format
     * @param additionalHeaders          Additional headers to add to captured events
     * @param additionalHeaderGenerators Additional header generator functions to use to add headers to captured events
     * @return Capture sink (if capture is enabled), otherwise returns {@code null}
     */
    public Sink<Event<TKey, TValue>> getCaptureSink(Serializer<TKey> keySerializer,
                                                    Serializer<TValue> valueSerializer, List<Header> additionalHeaders,
                                                    List<Function<Event<TKey, TValue>, Header>> additionalHeaderGenerators) {
        if (this.captureDirectory == null) {
            return null;
        }

        if (this.useFileSource() && this.sourceDirectory != null && Objects.equals(this.sourceDirectory.getAbsolutePath(), this.captureDirectory.getAbsolutePath()) && Objects.equals(this.sourceFormat, this.captureFormat)) {
            throw new IllegalArgumentException(
                    "Cannot specify the same file source and event file capture directories, unless the source and capture formats are different");
        }
        LOGGER.warn(
                "Event file capture has been enabled, this will impact performance BUT will allow replaying events in future");
        LOGGER.info("Events are being captured to {}", this.captureDirectory.getAbsolutePath());

        if (!this.captureDirectory.exists()) {
            LOGGER.info("Attempting to create capture directory {}", this.captureDirectory.getAbsolutePath());
            if (!this.captureDirectory.mkdirs()) {
                throw new IllegalArgumentException(String.format("Failed to create required event capture directory %s",
                                                                 this.captureDirectory.getAbsolutePath()));
            }
        }

        FileEventFormatProvider format = FileEventFormats.get(this.captureFormat);
        if (format == null) {
            throw new IllegalArgumentException(
                    String.format("Source format '%s' is not a valid event file format", this.captureFormat));
        }

        this.usingFileCapture = true;
        return EventCapturingSink.<TKey, TValue>create()
                                 .directory(this.captureDirectory)
                                 .writer(format.createWriter(keySerializer, valueSerializer))
                                 .extension(format.defaultFileExtension())
                                 .addHeaders(additionalHeaders)
                                 .generateHeaders(additionalHeaderGenerators)
                                 .discard()
                                 .build();
    }
}
