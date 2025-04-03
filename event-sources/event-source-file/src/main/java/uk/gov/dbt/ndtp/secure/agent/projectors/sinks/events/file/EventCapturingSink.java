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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks.events.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.SinkException;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.AbstractTransformingSink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.AbstractForwardingSinkBuilder;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.Header;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventSource;
import uk.gov.dbt.ndtp.secure.agent.sources.file.FileEventWriter;
import uk.gov.dbt.ndtp.secure.agent.sources.file.yaml.YamlEventReaderWriter;

/**
 * A sink that captures events to files so that they can be replayed using a
 * {@link FileEventSource} in the future while also forwarding them onto another
 * sink.
 * <p>
 * This is primarily intended for capturing events for future reuse in integration tests, it's implemented as a
 * forwarding sink so it can be inserted into relevant points of a pipeline to capture events as they flow through.
 * </p>
 *
 * @param <TKey>   Key type
 * @param <TValue> Value type
 */
public class EventCapturingSink<TKey, TValue>
    extends AbstractTransformingSink<Event<TKey, TValue>, Event<TKey, TValue>> {

  private long nextFileNumber = -1;
  private final int padding;
  private final String prefix, extension;
  private final File targetDirectory;
  private final FileEventWriter<TKey, TValue> writer;
  private final List<Header> additionalHeaders;
  private final List<Function<Event<TKey, TValue>, Header>> additionalHeaderGenerators;

  /**
   * Creates a new sink
   *
   * @param destination     Destination sink
   * @param targetDirectory Target directory to which events will be written
   * @param writer          Event writer
   * @param prefix          Filename prefix
   * @param padding         How many characters to pad the numeric portion of the generated filename to
   * @param extension       Filename extension
   */
  EventCapturingSink(Sink<Event<TKey, TValue>> destination, File targetDirectory,
      FileEventWriter<TKey, TValue> writer, String prefix, int padding, String extension,
      List<Header> additionalHeaders,
      List<Function<Event<TKey, TValue>, Header>> additionalHeaderGenerators) {
    super(destination);
    Objects.requireNonNull(targetDirectory, "Target directory cannot be null");
    Objects.requireNonNull(writer, "Event writer cannot be null");
    this.targetDirectory = targetDirectory;
    this.writer = writer;
    this.prefix = prefix;
    this.padding = padding;
    this.extension = StringUtils.isBlank(extension) ? extension :
        (StringUtils.startsWith(extension, ".") ? extension : "." + extension);
    this.additionalHeaders =
        additionalHeaders != null ? new ArrayList<>(additionalHeaders) : Collections.emptyList();
    this.additionalHeaderGenerators =
        additionalHeaderGenerators != null ? new ArrayList<>(additionalHeaderGenerators) :
            Collections.emptyList();
  }

  @Override
  protected Event<TKey, TValue> transform(Event<TKey, TValue> event) {
    if (event == null) {
      return null;
    }

    // Prepare the extra headers (if any)
    List<Header> extraHeaders = new ArrayList<>(this.additionalHeaders);
    final Event<TKey, TValue> finalEvent = event;
    this.additionalHeaderGenerators.stream()
        .map(g -> g.apply(finalEvent))
        .filter(Objects::nonNull)
        .forEach(extraHeaders::add);
    if (!extraHeaders.isEmpty()) {
      // If we have extra headers modify the event before we capture it
      event = event.addHeaders(extraHeaders.stream());
    }

    try {
      // Prepare the next filename
      nextFileNumber++;
      StringBuilder filename = new StringBuilder();
      if (StringUtils.isNotBlank(this.prefix)) {
        filename.append(this.prefix);
      }
      filename.append(StringUtils.leftPad(Long.toString(nextFileNumber), this.padding, '0'));
      if (StringUtils.isNotBlank(this.extension)) {
        filename.append(this.extension);
      }

      // Actually capture the event
      this.writer.write(event, new File(this.targetDirectory, filename.toString()));
    } catch (Throwable e) {
      throw new SinkException(e);
    }
    return event;
  }

  @Override
  public void close() {
    this.nextFileNumber = -1;
  }

  /**
   * Creates a builder for this sink
   *
   * @param <TKey>   Key type
   * @param <TValue> Value type
   * @return Builder
   */
  public static <TKey, TValue> Builder<TKey, TValue> create() {
    return new Builder<>();
  }

  /**
   * A builder for event to file sinks
   *
   * @param <TKey>   Key type
   * @param <TValue> Value type
   */
  public static class Builder<TKey, TValue> extends
      AbstractForwardingSinkBuilder<Event<TKey, TValue>, Event<TKey, TValue>, EventCapturingSink<TKey, TValue>, Builder<TKey, TValue>> {

    private int padding = 6;
    private String prefix = "event-", extension = ".yaml";
    private File targetDirectory;
    private FileEventWriter<TKey, TValue> writer;
    private final List<Header> additionalHeaders = new ArrayList<>();
    private final List<Function<Event<TKey, TValue>, Header>> additionalHeaderGenerators = new ArrayList<>();

    /**
     * Sets the target directory to which event files will be written
     *
     * @param targetDir Target directory
     * @return Builder
     */
    public Builder<TKey, TValue> directory(File targetDir) {
      this.targetDirectory = targetDir;
      return this;
    }

    /**
     * Sets the padding for the numeric portion of the filenames produced by this sink
     *
     * @param padding Padding
     * @return Builder
     */
    public Builder<TKey, TValue> padding(int padding) {
      this.padding = padding;
      return this;
    }

    /**
     * Sets the prefix for the filenames produced by this sink
     *
     * @param prefix Filename prefix
     * @return Builder
     */
    public Builder<TKey, TValue> prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Sets the file extension for the filenames produced by this sink
     *
     * @param extension Extension
     * @return Builder
     */
    public Builder<TKey, TValue> extension(String extension) {
      this.extension = extension;
      return this;
    }

    /**
     * Sets the event writer used to write events as files
     *
     * @param writer Event writer
     * @return Builder
     */
    public Builder<TKey, TValue> writer(FileEventWriter<TKey, TValue> writer) {
      this.writer = writer;
      return this;
    }

    /**
     * Sets the event writer to the {@link YamlEventReaderWriter} using a builder function to configure it
     *
     * @param f Function that configures the {@link YamlEventReaderWriter.Builder} as desired
     * @return Builder
     */
    public Builder<TKey, TValue> writeYaml(
        Function<YamlEventReaderWriter.Builder<TKey, TValue>, YamlEventReaderWriter.Builder<TKey, TValue>> f) {

      this.writer = f.apply(YamlEventReaderWriter.create()).build();
      return this;
    }

    /**
     * Adds a header to the events that are being captured
     *
     * @param key   Header key
     * @param value Header value
     * @return Builder
     */
    public Builder<TKey, TValue> addHeader(String key, String value) {
      return addHeader(new Header(key, value));
    }

    /**
     * Adds a header to the events that are being captured
     *
     * @param header Header
     * @return Builder
     */
    public Builder<TKey, TValue> addHeader(Header header) {
      if (header == null) {
        return this;
      }
      this.additionalHeaders.add(header);
      return this;
    }

    /**
     * Adds multiple headers to the events that are being captured
     *
     * @param headers Headers
     * @return Builder
     */
    public Builder<TKey, TValue> addHeaders(List<Header> headers) {
      if (headers == null) {
        return this;
      }

      CollectionUtils.addAll(this.additionalHeaders, headers);
      return this;
    }

    /**
     * Adds a header generator function that can selectively generate an additional header that is added to the
     * events being captured
     *
     * @param generator Generator function, returns either a header or {@code null} if no additional header should
     *                  be added for an event
     * @return Builder
     */
    public Builder<TKey, TValue> generateHeaders(Function<Event<TKey, TValue>, Header> generator) {
      if (generator == null) {
        return this;
      }
      this.additionalHeaderGenerators.add(generator);
      return this;
    }

    /**
     * Adds header generator functions that can selectively generate additional headers that are added to the events
     * being captured
     *
     * @param generators Generator functions, each returns either a header or {@code null} if no additional header
     *                   should be added for an event
     * @return Builder
     */
    public Builder<TKey, TValue> generateHeaders(
        List<Function<Event<TKey, TValue>, Header>> generators) {
      if (generators == null) {
        return this;
      }
      CollectionUtils.addAll(this.additionalHeaderGenerators, generators);
      return this;
    }

    @Override
    public EventCapturingSink<TKey, TValue> build() {
      return new EventCapturingSink<>(this.getDestination(), this.targetDirectory, this.writer,
          this.prefix,
          this.padding, this.extension, this.additionalHeaders,
          this.additionalHeaderGenerators);
    }
  }
}
