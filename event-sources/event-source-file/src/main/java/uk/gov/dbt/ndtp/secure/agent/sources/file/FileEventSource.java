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
package uk.gov.dbt.ndtp.secure.agent.sources.file;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSourceException;

/**
 * An event source where the events are files on disk in a directory
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class FileEventSource<TKey, TValue> implements EventSource<TKey, TValue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileEventSource.class);

    private final List<File> events = new ArrayList<>();
    private final FileEventReader<TKey, TValue> eventReader;
    private boolean closed = false;

    /**
     * Creates a new file event source
     *
     * @param sourceDir       Source directory containing the events
     * @param eventFileFilter Filter used to identify files that represent events
     * @param fileComparator  File comparator used to sort events into the desired order
     * @param reader          File event reader to use to convert the files into events
     */
    public FileEventSource(File sourceDir, FileFilter eventFileFilter, Comparator<File> fileComparator,
                           FileEventReader<TKey, TValue> reader) {
        Objects.requireNonNull(sourceDir, "Source directory cannot be null");
        Objects.requireNonNull(eventFileFilter, "Event filter filter cannot be null");
        Objects.requireNonNull(fileComparator, "File comparator cannot be null");
        Objects.requireNonNull(reader, "File event reader cannot be null");
        if (!sourceDir.exists()) {
            throw new IllegalArgumentException("No such directory " + sourceDir.getAbsolutePath());
        }
        if (!sourceDir.isDirectory()) {
            throw new IllegalArgumentException(sourceDir.getAbsolutePath() + " is not a directory");
        }
        this.eventReader = reader;
        this.events.addAll(obtainEventFiles(sourceDir,eventFileFilter));
        this.events.sort(fileComparator);
    }

    @Override
    public boolean availableImmediately() {
        return !this.closed && !this.events.isEmpty();
    }

    @Override
    public boolean isExhausted() {
        return this.closed || this.events.isEmpty();
    }

    @Override
    public void close() {
        this.closed = true;
        this.events.clear();
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public Event<TKey, TValue> poll(Duration timeout) {
        if (this.closed) {
            throw new IllegalStateException("Event source is closed");
        }
        if (!this.events.isEmpty()) {
            File f = this.events.remove(0);
            try {
                return this.eventReader.read(f);
            } catch (IOException e) {
                throw new EventSourceException("Failed to parse an Event from file " + f.getAbsolutePath(), e);
            } catch (Throwable e) {
                throw new EventSourceException("Invalid Event in file " + f.getAbsolutePath(), e);
            }
        } else {
            return null;
        }
    }

    @Override
    public Long remaining() {
        return (long) this.events.size();
    }

    @Override
    public void processed(Collection<Event> processedEvents) {
        // No-op
        LOGGER.trace("Received {} processed events in processed() callback, this is ignored by the FileEventSource",
                     processedEvents.size());
    }

    /**
     * Creates a list of files from the given directory using the given filter.
     *
     * @param sourceDir       Source directory containing the events
     * @param eventFileFilter Filter used to identify files that represent events
     * @return List of files, or empty list if null.
     */
    private List<File> obtainEventFiles(File sourceDir, FileFilter eventFileFilter) {
        File[] fileArray = sourceDir.listFiles(eventFileFilter);
        if (null != fileArray) {
            return Arrays.asList(fileArray);
        }
        return Collections.emptyList();
    }
}
