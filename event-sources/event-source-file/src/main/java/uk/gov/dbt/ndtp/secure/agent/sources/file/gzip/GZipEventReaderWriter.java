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
package uk.gov.dbt.ndtp.secure.agent.sources.file.gzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventAccessMode;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventReaderWriter;

/**
 * A GZip decorator around an event reader/writer that compresses and decompresses the output with GZip
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class GZipEventReaderWriter<TKey, TValue> implements FileEventReaderWriter<TKey, TValue> {

    private final FileEventReaderWriter<TKey, TValue> readerWriter;


    /**
     * Creates a new GZipped reader/writer
     *
     * @param readerWriter Underlying reader/writer
     */
    public GZipEventReaderWriter(FileEventReaderWriter<TKey, TValue> readerWriter) {
        Objects.requireNonNull(readerWriter, "Event reader/writer cannot be null");
        this.readerWriter = readerWriter;
    }

    @Override
    public final Event<TKey, TValue> read(File f) throws IOException {
        try (FileInputStream input = new FileInputStream(f)) {
            try (GZIPInputStream gzipInput = new GZIPInputStream(input)) {
                return read(gzipInput);
            }
        }
    }

    @Override
    public final Event<TKey, TValue> read(InputStream input) throws IOException {
        if (!(input instanceof GZIPInputStream)) {
            input = new GZIPInputStream(input);
        }
        return this.readerWriter.read(input);
    }

    @Override
    public final void write(Event<TKey, TValue> event, File f) throws IOException {
        try (FileOutputStream output = new FileOutputStream(f)) {
            try (GZIPOutputStream gzipOutput = new GZIPOutputStream(output)) {
                this.write(event, gzipOutput);
            }
        }
    }

    @Override
    public final void write(Event<TKey, TValue> event, OutputStream output) throws IOException {
        if (!(output instanceof GZIPOutputStream)) {
            output = new GZIPOutputStream(output);
        }
        this.readerWriter.write(event, output);
    }

    @Override
    public FileEventAccessMode getMode() {
        return this.readerWriter.getMode();
    }
}
